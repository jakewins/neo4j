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
    /**
     * Gives you a cursor for efficiently reading the store. The cursor you get back will always at least implement
     * the {@link org.neo4j.kernel.impl.store.Store.RecordCursor} interface, but most stores are expected to provide
     * richer cursor interfaces that allow reading individual fields of whatever record you are currently positioned at.
     */
    CURSOR cursor();

    /** Read the specified record. */
    RECORD read(long id);

    void write(RECORD record) throws IOException;

    /** Allocate a new free record id. */
    long allocate();

    /** Signal that a record slot is no longer used, freeing it up for others. */
    void free(long id);

    interface RecordCursor<RECORD>
    {
        /** Read a full record from the current position. */
        RECORD currentRecord();

        /** Moves to an explicit record position, independent of if that position is in use or not. */
        boolean next( long id );

        /** Moves to the next in-use record, or returns false if there are no more records in the store. */
        boolean next();

        void close();
    }
}
