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
package org.neo4j.index.impl.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;

import org.neo4j.function.Function;
import org.neo4j.helpers.collection.PrefetchingIterator;

/**
 * This is a helper mechanism to iterate over large lucene results in batches. The default search mode requires a user
 * to specify up-front the max number of hits to retrieve, and provides a paging mechanism to iterate over very large
 * results. This hides that paging under a regular iterator API.
 * @param <T> the output type, which is an object converted from the underlying lucene document.
 */
public class SearchIterator<T> extends PrefetchingIterator<T>
{
    public static final Function<Document,Document>
            NO_OP_CONVERTER = new Function<Document,Document>()
    {
        @Override
        public Document apply( Document doc ) throws RuntimeException
        {
            return doc;
        }
    };

    private final Function<Document,T> converter;
    private final IndexSearcher searcher;
    private final Query query;
    private final int batchSize;
    private final Sort sort;
    private final boolean correctnessOverSpeed;

    private ScoreDoc lastDoc;
    private int index = 0;
    private ScoreDoc[] currentBatch;
    private int approximateSize = -1;
    private float currentScore;

    public static SearchIterator<Document> search( IndexSearcher searcher, Query query )
    {
        return search( NO_OP_CONVERTER, searcher, 100, query, null, false);
    }

    public static SearchIterator<Document> search(
            IndexSearcher searcher, int batchSize, Query query, Sort sort, boolean correctnessOverSpeed )
    {
        return new SearchIterator<>( NO_OP_CONVERTER, searcher, batchSize, query, sort, correctnessOverSpeed );
    }

    public static <T> SearchIterator<T> search( Function<Document, T> converter, IndexSearcher searcher, Query query )
    {
        return search(converter, searcher, 100, query, null, false);
    }

    public static <T> SearchIterator<T> search(
            Function<Document, T> converter, IndexSearcher searcher,
            int batchSize, Query query, Sort sort, boolean correctnessOverSpeed )
    {
        return new SearchIterator<>( converter, searcher, batchSize, query, sort, correctnessOverSpeed );
    }

    /**
     * Create a new searcher. These are single threaded, non-reusable.
     *
     * @param converter converts from documents to whatever you want. It may return null for any input, which will
     *                  exclude that input from the result.
     * @param searcher
     * @param batchSize how many documents to fetch at a time.
     * @param query query, required. This is the actual query. That should be pretty obvious. Why are you even still
     *              reading this at this point? No, seriously, there is nothing here, you are wasting time this very
     *              second reading something that will give you no benefit.
     * @param sort sort, optional, may be null
     * @param correctnessOverSpeed instruct lucene to calculate scores for each document, improving the accuracy of
     *                             sorting, but sacrificing performance.
     */
    public SearchIterator( Function<Document, T> converter, IndexSearcher searcher, int batchSize, Query query,
                           Sort sort, boolean correctnessOverSpeed)
    {
        this.converter = converter;
        this.searcher = searcher;
        this.query = query;
        this.batchSize = batchSize;
        this.sort = sort;
        this.correctnessOverSpeed = correctnessOverSpeed;

        nextBatch();
    }

    @Override
    protected T fetchNextOrNull()
    {
        try
        {
            if(currentBatch == null || index >= currentBatch.length) // First call, or current batch is exhausted
            {
                if ( index == -1 )
                {
                    return null; // done searching
                }

                if( currentBatch != null && currentBatch.length < batchSize )
                {
                    return null; // done searching
                }

                if ( !nextBatch() )
                {
                    return null;
                }
            }

            currentScore = currentBatch[index].score;
            T out = converter.apply( searcher.doc( currentBatch[index].doc ) );

            index++;

            if(out == null)
            {
                return fetchNextOrNull();
            }
            else
            {
                return out;
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e ); // TODO: something better?
        }
    }

    /** Return an approximate count for the number of entries that the query found. */
    public int approximateSize()
    {
        return approximateSize;
    }

    public float currentScore()
    {
        return currentScore;
    }

    private boolean nextBatch()
    {
        try
        {
            currentBatch = null;
            index = 0;

            TopDocs docs = nextSearch();

            lastDoc = null;
            int docCount = docs != null ? docs.scoreDocs.length : 0;

            if(approximateSize == -1)
            {
                approximateSize = docs != null ? docs.totalHits : 0;
            }

            if ( docCount == 0 )
            {
                index = -1;
                return false;
            }
            currentBatch = docs.scoreDocs;
            lastDoc = currentBatch[docCount - 1];
            return true;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e ); // TODO: something better?
        }
    }

    private TopDocs nextSearch() throws IOException
    {
        if(sort == null)
        {
            return searcher.searchAfter( lastDoc, query, null, batchSize );
        }
        return searcher.searchAfter( lastDoc, query, null, batchSize, sort, correctnessOverSpeed, false );
    }
}

