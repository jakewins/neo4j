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
package org.neo4j.kernel.impl.store;

import java.io.IOException;

import org.neo4j.kernel.lifecycle.Lifecycle;

public interface Store<RECORD, CURSOR extends Store.RecordCursor> extends Lifecycle
{
    /** No flags */
    static int SF_NO_FLAGS = 0;

    /** Instead of a cursor starting at the beginning of the store, have a cursor start at the end and move backwards */
    static int SF_REVERSE_CURSOR = 1;

    /**
     * Gives you a cursor for efficiently reading the store. The cursor you get back will always at least implement
     * the {@link org.neo4j.kernel.impl.store.Store.RecordCursor} interface, but most stores are expected to provide
     * richer cursor interfaces that allow reading individual fields of whatever record you are currently positioned at.
     */
    CURSOR cursor(int flags);

    /** Read the specified record. */
    RECORD read(long id);

    void write(RECORD record) throws IOException;

    /** Allocate a new free record id. */
    long allocate();

    /** Signal that a record id is no longer used, freeing it up for others. */
    void free(long id);

    interface RecordCursor<RECORD>
    {
        /** Read a full record from the current position. */
        RECORD currentRecord();

        /** The id of the current record. */
        long currentId();

        /** Moves to an explicit record position, independent of if that position is in use or not. */
        boolean next( long id );

        /** Moves to the next in-use record, or returns false if there are no more records in the store. */
        boolean next();

        void close();
    }
}
