package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.neo4j.io.pagecache.PageCache;
import org.neo4j.test.rule.PageCacheRule;
import org.neo4j.test.rule.fs.EphemeralFileSystemRule;

public class PagedIndexInputTest
{
    @Rule
    public EphemeralFileSystemRule fs = new EphemeralFileSystemRule();

    @Rule
    public PageCacheRule pageCache = new PageCacheRule();

    @Test
    public void shouldReadByte() throws Exception
    {
        // Given
        PageCache pc = this.pageCache.getPageCache( fs );
        Path workDir = Paths.get( "test" );
        fs.mkdirs( workDir.toFile() );
        Directory dir = new PagedDirectory( workDir, pc );

        IndexOutput out = dir.createOutput( "1", IOContext.DEFAULT );

        byte val = (byte) 8;
        out.writeByte( val );
        out.close();

        IndexInput in = dir.openInput( "1", IOContext.READ );

        // When
        byte b = in.readByte();
        in.close();

        // Then
        assert 8 == b : String.format("Expected %d == %d", val, b);
    }
}
