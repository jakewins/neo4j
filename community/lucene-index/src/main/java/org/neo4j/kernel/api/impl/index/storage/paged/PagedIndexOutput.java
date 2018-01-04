package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.OutputStreamIndexOutput;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.neo4j.io.fs.FileSystemAbstraction;

// Clone of FSIndexOutput
// TODO: Use page cache instead..
class PagedIndexOutput extends OutputStreamIndexOutput
{
    /**
     * The maximum chunk size is 8192 bytes, because file channel mallocs
     * a native buffer outside of stack if the write buffer size is larger.
     */
    private static final int CHUNK_SIZE = 8192;

    PagedIndexOutput( Path path, FileSystemAbstraction fs ) throws IOException
    {
        super("FSIndexOutput(path=\"" + path + "\")",
                new FilterOutputStream( fs.openAsOutputStream( path.toFile(), false ) ) {
            // This implementation ensures, that we never write more than CHUNK_SIZE bytes:
            @Override
            public void write(byte[] b, int offset, int length) throws IOException {
                while (length > 0) {
                    final int chunk = Math.min(length, CHUNK_SIZE);
                    out.write(b, offset, chunk);
                    length -= chunk;
                    offset += chunk;
                }
            }
        }, CHUNK_SIZE);
    }
}
