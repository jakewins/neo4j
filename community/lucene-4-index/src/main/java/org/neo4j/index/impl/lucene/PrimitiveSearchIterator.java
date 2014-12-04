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

import org.neo4j.collection.primitive.PrimitiveLongCollections;
import org.neo4j.function.primitive.FunctionToPrimitiveLong;

public class PrimitiveSearchIterator extends PrimitiveLongCollections.PrimitiveLongBaseIterator
{
    private final FunctionToPrimitiveLong<Document> converter;
    private final SearchIterator<Document> inner;

    /**
     * Wrap a search iterator and decorate it with a long converter.
     */
    public PrimitiveSearchIterator( FunctionToPrimitiveLong<Document> converter, SearchIterator<Document> inner )
    {
        this.converter = converter;
        this.inner = inner;
    }

    @Override
    protected boolean fetchNext()
    {
        if(inner.hasNext())
        {
            next(converter.apply( inner.next() ));
            return true;
        }
        return false;
    }
}
