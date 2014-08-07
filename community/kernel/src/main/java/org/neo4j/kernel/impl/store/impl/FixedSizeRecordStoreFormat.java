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
package org.neo4j.kernel.impl.store.impl;

import org.neo4j.io.fs.StoreChannel;
import org.neo4j.kernel.impl.store.Store;

public abstract class FixedSizeRecordStoreFormat<RECORD, CURSOR extends Store.RecordCursor>
        implements StoreFormat<RECORD, CURSOR>
{
    private final int recordSize;
    private final String recordName;

    public FixedSizeRecordStoreFormat(int recordSize, String recordName)
    {
        this.recordSize = recordSize;
        this.recordName = recordName;
    }

    @Override
    public String recordName()
    {
        return recordName;
    }

    @Override
    public int recordSize( StoreChannel channel )
    {
        return recordSize;
    }

    @Override
    public void createStore( StoreChannel channel )
    {

    }

    @Override
    public int headerSize()
    {
        return 0;
    }
}
