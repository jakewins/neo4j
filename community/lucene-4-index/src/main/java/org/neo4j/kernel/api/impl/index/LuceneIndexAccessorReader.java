/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.Closeable;
import java.io.IOException;

import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.function.primitive.FunctionToPrimitiveLong;
import org.neo4j.helpers.CancellationRequest;
import org.neo4j.index.impl.lucene.PrimitiveSearchIterator;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.index.IndexReader;
import org.neo4j.kernel.impl.api.index.sampling.NonUniqueIndexSampler;

import static org.neo4j.index.impl.lucene.SearchIterator.search;
import static org.neo4j.kernel.api.impl.index.LuceneDocumentStructure.NODE_ID_KEY;
import static org.neo4j.register.Register.DoubleLong;

class LuceneIndexAccessorReader implements IndexReader
{
    private final IndexSearcher searcher;
    private final LuceneDocumentStructure documentLogic;
    private final Closeable onClose;
    private final CancellationRequest cancellation;
    private final int bufferSizeLimit;
    private final FunctionToPrimitiveLong<Document> DOC_2_ID = new FunctionToPrimitiveLong<Document>()
    {
        @Override
        public long apply( Document value )
        {
            return documentLogic.getNodeId( value );
        }
    };

    LuceneIndexAccessorReader( IndexSearcher searcher, LuceneDocumentStructure documentLogic, Closeable onClose,
                               CancellationRequest cancellation, int bufferSizeLimit )
    {
        this.searcher = searcher;
        this.documentLogic = documentLogic;
        this.onClose = onClose;
        this.cancellation = cancellation;
        this.bufferSizeLimit = bufferSizeLimit;

    }

    @Override
    public long sampleIndex( DoubleLong.Out result ) throws IndexNotFoundKernelException
    {
        NonUniqueIndexSampler sampler = new NonUniqueIndexSampler( bufferSizeLimit );

        try
        {
            org.apache.lucene.index.IndexReader reader = luceneIndexReader();
            Fields fields = MultiFields.getFields( reader );
            for ( String field : fields )
            {
                if(!NODE_ID_KEY.equals( field ))
                {
                    TermsEnum iterator = fields.terms( field ).iterator( null );
                    while(iterator.next() != null)
                    {
                        sampler.include(Term.toString(iterator.term()));
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

        return sampler.result( result );
    }


    @Override
    public PrimitiveLongIterator lookup( Object value )
    {
        return new PrimitiveSearchIterator( DOC_2_ID, search( searcher, documentLogic.newQuery( value ) ) );
    }

    @Override
    public int getIndexedCount( long nodeId, Object propertyValue )
    {
        Query nodeIdQuery = new TermQuery( documentLogic.newQueryForChangeOrRemove( nodeId ) );
        Query valueQuery = documentLogic.newQuery( propertyValue );
        BooleanQuery nodeIdAndValueQuery = new BooleanQuery( true );
        nodeIdAndValueQuery.add( nodeIdQuery, BooleanClause.Occur.MUST );
        nodeIdAndValueQuery.add( valueQuery, BooleanClause.Occur.MUST );
        try
        {
            // A <label,propertyKeyId,nodeId> tuple should only match at most a single propertyValue
            return searcher.search( nodeIdAndValueQuery, 8 ).totalHits;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void close()
    {
        try
        {
            onClose.close();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected void checkCancellation() throws IndexNotFoundKernelException
    {
        if ( cancellation.cancellationRequested() )
        {
            throw new IndexNotFoundKernelException( "Index dropped while sampling." );
        }
    }

    protected org.apache.lucene.index.IndexReader luceneIndexReader()
    {
        return searcher.getIndexReader();
    }
}
