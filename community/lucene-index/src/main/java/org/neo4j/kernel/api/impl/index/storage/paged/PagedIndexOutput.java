/*
 * Copyright (c) 2002-2018 "Neo Technology,"
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
package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.OutputStreamIndexOutput;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.neo4j.io.fs.FileSystemAbstraction;

// TODO: Use page cache instead?
class PagedIndexOutput extends OutputStreamIndexOutput
{
    // TODO: This is a clone of FSIndexOutput; note the comment on the malloc on write,
    //       might be worth testing and potentially replicating in our regular IO code?
    /**
     * The maximum chunk size is 8192 bytes, because file channel mallocs
     * a native buffer outside of stack if the write buffer size is larger.
     */
    private static final int CHUNK_SIZE = 8192;

    PagedIndexOutput( Path path, FileSystemAbstraction fs ) throws IOException
    {
        super( "PagedIndexOutput(path=\"" + path + "\")", new FilterOutputStream( fs.openAsOutputStream( path.toFile(), false ) )
        {
            // This implementation ensures, that we never write more than CHUNK_SIZE bytes:
            @Override
            public void write( byte[] b, int offset, int length ) throws IOException
            {
                while ( length > 0 )
                {
                    final int chunk = Math.min( length, CHUNK_SIZE );
                    out.write( b, offset, chunk );
                    length -= chunk;
                    offset += chunk;
                }
            }
        }, CHUNK_SIZE );
    }
}
