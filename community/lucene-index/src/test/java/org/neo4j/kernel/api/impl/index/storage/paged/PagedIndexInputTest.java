package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.neo4j.function.ThrowingBiConsumer;
import org.neo4j.function.ThrowingBiFunction;
import org.neo4j.function.ThrowingFunction;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.test.rule.PageCacheRule;
import org.neo4j.test.rule.fs.EphemeralFileSystemRule;
import org.neo4j.test.rule.fs.FileSystemRule;

public class PagedIndexInputTest
{
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule
    public FileSystemRule fs = new EphemeralFileSystemRule();

    @Rule
    public PageCacheRule pageCache = new PageCacheRule();
    private PageCache pc;
    private Directory dir;

    @Test
    public void shouldReadAndWriteByte() throws Exception
    {
        test((byte)7, IndexOutput::writeByte, IndexInput::readByte);
        test((byte)0, IndexOutput::writeByte, IndexInput::readByte);
        test(Byte.MAX_VALUE, IndexOutput::writeByte, IndexInput::readByte);
        test(Byte.MIN_VALUE, IndexOutput::writeByte, IndexInput::readByte);
    }

    @Test
    public void shouldReadAndWriteShort() throws Exception
    {
        test((short)7, IndexOutput::writeShort, IndexInput::readShort);
        test((short)0, IndexOutput::writeShort, IndexInput::readShort);
        test(Short.MAX_VALUE, IndexOutput::writeShort, IndexInput::readShort);
        test(Short.MIN_VALUE, IndexOutput::writeShort, IndexInput::readShort);
    }

    @Test
    public void shouldReadAndWriteInteger() throws Exception
    {
        test(7, IndexOutput::writeInt, IndexInput::readInt);
        test(0, IndexOutput::writeInt, IndexInput::readInt);
        test(Integer.MAX_VALUE, IndexOutput::writeInt, IndexInput::readInt);
        test(Integer.MIN_VALUE, IndexOutput::writeInt, IndexInput::readInt);
    }

    @Test
    public void shouldReadAndWriteLong() throws Exception
    {
        test(7L, IndexOutput::writeLong, IndexInput::readLong);
        test(0L, IndexOutput::writeLong, IndexInput::readLong);
        test(Long.MAX_VALUE, IndexOutput::writeLong, IndexInput::readLong);
        test(Long.MIN_VALUE, IndexOutput::writeLong, IndexInput::readLong);
    }

    @Before
    public void setup() throws IOException
    {
        pc = this.pageCache.getPageCache( fs );
        Path workDir = tmpDir.newFolder().toPath();
        fs.mkdirs( workDir.toFile() );
        dir = new PagedDirectory( workDir, pc );
    }

    private <T> ThrowingFunction<IndexInput,T,IOException> atCurrentOffset(ThrowingBiFunction<IndexInput, Long, T, IOException> offsetRead) {
        return (in) -> {
            return offsetRead.apply( in, in.getFilePointer() );
        };
    }

    private <T> void test( T val, ThrowingBiConsumer<IndexOutput,T,IOException> write, ThrowingFunction<IndexInput,T,IOException> read ) throws IOException
    {
        // Test every possible offset for the page size, to cover all options for cross-page overlaps
        for ( int offset = 0; offset < pc.pageSize() + 1; offset++ )
        {
            test( val, write, read, dir, offset );
        }
    }

    private <T> void test( T val, ThrowingBiConsumer<IndexOutput,T,IOException> write, ThrowingFunction<IndexInput,T,IOException> read, int offset ) throws IOException
    {
        // Given
        PageCache pc = this.pageCache.getPageCache( fs );
        Path workDir = Paths.get( "test" );
        fs.mkdirs( workDir.toFile() );
        Directory dir = new PagedDirectory( workDir, pc );

        test( val, write, read, dir, offset );
    }

    private <T> void test( T val, ThrowingBiConsumer<IndexOutput,T,IOException> write, ThrowingFunction<IndexInput,T,IOException> read, Directory dir,
            int offset ) throws IOException
    {
        String fileName = testFileName( "offset" + offset );

        // Write the value
        IndexOutput out = dir.createOutput( fileName, IOContext.DEFAULT );
        out.writeBytes( new byte[offset], offset );
        write.accept( out, val );
        out.close();

        // Read it back
        IndexInput in = dir.openInput( fileName, IOContext.READ );
        in.seek( offset );
        T actual = read.apply( in );
        in.close();

        assert val.equals(actual) : String.format("Expected %s, got %s, when offset by %d", val, actual, offset);
    }

    private static String testFileName(String suffix) {
        // Just to create a file name that's at least marginally helpful during debugging
        StringBuilder name = new StringBuilder(  );
        StackTraceElement testMethod = Thread.currentThread().getStackTrace()[4];
        name.append( testMethod.getMethodName() ).append( "L" ).append( testMethod.getLineNumber() ).append( "." );
        return name.append( suffix ).toString();
    }
}
