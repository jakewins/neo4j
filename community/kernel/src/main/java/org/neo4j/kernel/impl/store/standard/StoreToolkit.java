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
package org.neo4j.kernel.impl.store.standard;

/** Utility methods used by a store and its store format. */
public class StoreToolkit
{
    private final int recordSize;
    private final int pageSize;
    private final long firstRecordId;
    private final StoreIdGenerator idGenerator;

    public StoreToolkit( int recordSize, int pageSize, long firstRecordId, StoreIdGenerator idGenerator )
    {
        this.recordSize = recordSize;
        this.pageSize = pageSize;
        this.firstRecordId = firstRecordId;
        this.idGenerator = idGenerator;
    }

    long pageId( long id )
    {
        return id * recordSize / pageSize;
    }

    /** Offset inside a page that a given record can be found at. */
    int recordOffset( long id )
    {
        return (int) (id * recordSize % pageSize);
    }

    /** The store page size. NOTE that this may be different from the system-wide page size reported by the PageCache */
    public int pageSize()
    {
        return pageSize - pageSize % recordSize;
    }

    public int recordSize()
    {
        return recordSize;
    }

    /** An id that has only one guarantee: No in-use records will have ids higher than this. */
    public long highestKnownId() { return idGenerator.highestIdInUse(); }

    /** Get the id of the first record in the store. This exists because initial ids may be reserved for headers. */
    public long firstRecordId()
    {
        return firstRecordId;
    }
}
