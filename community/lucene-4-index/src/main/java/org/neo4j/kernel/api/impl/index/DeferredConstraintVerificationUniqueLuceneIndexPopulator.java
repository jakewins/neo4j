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
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.neo4j.helpers.ThisShouldNotHappenError;
import org.neo4j.kernel.api.StatementConstants;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.api.index.IndexEntryConflictException;
import org.neo4j.kernel.api.index.IndexUpdater;
import org.neo4j.kernel.api.index.NodePropertyUpdate;
import org.neo4j.kernel.api.index.PreexistingIndexEntryConflictException;
import org.neo4j.kernel.api.index.PropertyAccessor;
import org.neo4j.kernel.api.index.util.FailureStorage;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.impl.api.index.sampling.UniqueIndexSampler;

import static org.neo4j.kernel.api.impl.index.LuceneDocumentStructure.NODE_ID_KEY;
import static org.neo4j.register.Register.DoubleLong;

class DeferredConstraintVerificationUniqueLuceneIndexPopulator extends LuceneIndexPopulator
{
    private final IndexDescriptor descriptor;
    private final UniqueIndexSampler sampler;

    private SearcherManager searcherManager;

    DeferredConstraintVerificationUniqueLuceneIndexPopulator( LuceneDocumentStructure documentStructure,
                                                              LuceneIndexWriterFactory indexWriterFactory,
                                                              IndexWriterStatus writerStatus,
                                                              DirectoryFactory dirFactory, File dirFile,
                                                              FailureStorage failureStorage, long indexId,
                                                              IndexDescriptor descriptor )
    {
        super( documentStructure, indexWriterFactory, writerStatus, dirFactory, dirFile, failureStorage, indexId );
        this.descriptor = descriptor;
        this.sampler = new UniqueIndexSampler();
    }

    @Override
    public void create() throws IOException
    {
        super.create();
        searcherManager = new SearcherManager( writer, true, new SearcherFactory() );
    }

    @Override
    public void drop()
    {
    }

    @Override
    protected void flush() throws IOException
    {
        // no need to do anything yet.
    }

    @Override
    public void add( long nodeId, Object propertyValue ) throws IndexEntryConflictException, IOException
    {
        sampler.increment( 1 );
        IndexableField encodedValue = documentStructure.encodeAsFieldable( propertyValue );
        Document doc = documentStructure.newDocumentRepresentingProperty( nodeId, encodedValue );
        writer.addDocument( doc );
    }

    @Override
    public void verifyDeferredConstraints( PropertyAccessor accessor ) throws IndexEntryConflictException, IOException
    {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();

        try
        {
            DuplicateCheckingCollector collector = duplicateCheckingCollector( accessor );

            IndexReader reader = searcher.getIndexReader();
            Fields fields = MultiFields.getFields( reader );

            for (String field : fields)
            {
                if(!NODE_ID_KEY.equals( field ))
                {
                    TermsEnum termsEnum = fields.terms( field ).iterator( null );
                    while (termsEnum.next() != null)
                    {
                        collector.reset();
                        searcher.search( new TermQuery( new Term( field, termsEnum.term() ) ), collector );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            Throwable cause = e.getCause();
            if ( cause instanceof IndexEntryConflictException )
            {
                throw (IndexEntryConflictException) cause;
            }
            throw e;
        }
        finally
        {
            searcherManager.release( searcher );
        }
    }

    private DuplicateCheckingCollector duplicateCheckingCollector( PropertyAccessor accessor )
    {
        return new DuplicateCheckingCollector( accessor, documentStructure, descriptor.getPropertyKeyId() );
    }

    @Override
    public IndexUpdater newPopulatingUpdater( final PropertyAccessor accessor ) throws IOException
    {
        return new IndexUpdater()
        {
            List<Object> updatedPropertyValues = new ArrayList<>();

            @Override
            public void process( NodePropertyUpdate update ) throws IOException, IndexEntryConflictException
            {
                long nodeId = update.getNodeId();
                switch ( update.getUpdateMode() )
                {
                    case ADDED:
                        sampler.increment( 1 ); // add new value

                        // We don't look at the "before" value, so adding and changing idempotently is done the same way.
                        IndexableField encodedValue = documentStructure.encodeAsFieldable( update.getValueAfter() );
                        writer.updateDocument( documentStructure.newQueryForChangeOrRemove( nodeId ),
                                documentStructure.newDocumentRepresentingProperty( nodeId, encodedValue ) );
                        updatedPropertyValues.add( update.getValueAfter() );
                        break;
                    case CHANGED:
                        // do nothing on the sampler, since it would be something like:
                        // sampler.increment( -1 ); // remove old vale
                        // sampler.increment( 1 ); // add new value

                        // We don't look at the "before" value, so adding and changing idempotently is done the same way.
                        IndexableField encodedValueAfter = documentStructure.encodeAsFieldable( update.getValueAfter() );
                        writer.updateDocument( documentStructure.newQueryForChangeOrRemove( nodeId ),
                                documentStructure.newDocumentRepresentingProperty( nodeId, encodedValueAfter ) );
                        updatedPropertyValues.add( update.getValueAfter() );
                        break;
                    case REMOVED:
                        sampler.increment( -1 ); // remove old value
                        writer.deleteDocuments( documentStructure.newQueryForChangeOrRemove( nodeId ) );
                        break;
                    default:
                        throw new IllegalStateException( "Unknown update mode " + update.getUpdateMode() );
                }
            }

            @Override
            public void close() throws IOException, IndexEntryConflictException
            {
                searcherManager.maybeRefresh();
                IndexSearcher searcher = searcherManager.acquire();
                try
                {
                    DuplicateCheckingCollector collector = duplicateCheckingCollector( accessor );
                    for ( Object propertyValue : updatedPropertyValues )
                    {
                        collector.reset();
                        Query query = documentStructure.newQuery( propertyValue );
                        searcher.search( query, collector );
                    }
                }
                catch ( IOException e )
                {
                    Throwable cause = e.getCause();
                    if ( cause instanceof IndexEntryConflictException )
                    {
                        throw (IndexEntryConflictException) cause;
                    }
                    throw e;
                }
                finally
                {
                    searcherManager.release( searcher );
                }
            }

            @Override
            public void remove( Collection<Long> nodeIds )
            {
                throw new UnsupportedOperationException( "should not remove() from populating index" );
            }
        };
    }

    @Override
    public long sampleResult( DoubleLong.Out result )
    {
        return sampler.result( result );
    }

    private static class DuplicateCheckingCollector extends Collector
    {
        private final PropertyAccessor accessor;
        private final LuceneDocumentStructure documentStructure;
        private final int propertyKeyId;
        private final EntrySet actualValues;
        private AtomicReaderContext readerCtx;

        public DuplicateCheckingCollector(
                PropertyAccessor accessor,
                LuceneDocumentStructure documentStructure,
                int propertyKeyId )
        {
            this.accessor = accessor;
            this.documentStructure = documentStructure;
            this.propertyKeyId = propertyKeyId;
            actualValues = new EntrySet();
        }

        @Override
        public void setScorer( Scorer scorer ) throws IOException
        {
        }

        @Override
        public void collect( int doc ) throws IOException
        {
            try
            {
                doCollect( doc );
            }
            catch ( KernelException e )
            {
                throw new ThisShouldNotHappenError(
                        "Chris", "Indexed node should exist and have the indexed property.", e );
            }
            catch ( PreexistingIndexEntryConflictException e )
            {
                throw new IOException( e );
            }
        }

        @Override
        public void setNextReader( AtomicReaderContext readerContext ) throws IOException
        {
            this.readerCtx = readerContext;
        }

        private void doCollect( int doc ) throws IOException, KernelException, PreexistingIndexEntryConflictException
        {
            Document document = readerCtx.reader().document( doc );
            long nodeId = documentStructure.getNodeId( document );
            Property property = accessor.getProperty( nodeId, propertyKeyId );

            // We either have to find the first conflicting entry set element,
            // or append one for the property we just fetched:
            EntrySet current = actualValues;
            scan:do {
                for ( int i = 0; i < EntrySet.INCREMENT; i++ )
                {
                    Object value = current.value[i];

                    if ( current.nodeId[i] == StatementConstants.NO_SUCH_NODE )
                    {
                        current.value[i] = property.value();
                        current.nodeId[i] = nodeId;
                        if ( i == EntrySet.INCREMENT - 1 )
                        {
                            current.next = new EntrySet();
                        }
                        break scan;
                    }
                    else if ( property.valueEquals( value ) )
                    {
                        throw new PreexistingIndexEntryConflictException(
                                value, current.nodeId[i], nodeId );
                    }
                }
                current = current.next;
            } while ( current != null );
        }

        @Override
        public boolean acceptsDocsOutOfOrder()
        {
            return true;
        }

        public void reset()
        {
            actualValues.reset(); // TODO benchmark this vs. not clearing and instead creating a new object, perhaps
        }
    }

    /**
     * A small struct of arrays of nodeId + property value pairs, with a next pointer.
     * Should exhibit fairly fast linear iteration, small memory overhead and dynamic growth.
     *
     * NOTE: Must always call reset() before use!
     */
    private static class EntrySet
    {
        static final int INCREMENT = 100;

        Object[] value = new Object[INCREMENT];
        long[] nodeId = new long[INCREMENT];
        EntrySet next;

        public void reset()
        {
            EntrySet current = this;
            do {
                for (int i = 0; i < INCREMENT; i++)
                {
                    current.value[i] = null;
                    current.nodeId[i] = StatementConstants.NO_SUCH_NODE;
                }
                current = next;
            } while ( current != null );
        }
    }
}
