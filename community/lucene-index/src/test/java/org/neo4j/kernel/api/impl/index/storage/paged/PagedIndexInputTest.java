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
import java.util.Arrays;
import java.util.function.BiFunction;

import org.neo4j.function.ThrowingBiConsumer;
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
        test((byte)-1, IndexOutput::writeByte, IndexInput::readByte);
        test(Byte.MAX_VALUE, IndexOutput::writeByte, IndexInput::readByte);
        test(Byte.MIN_VALUE, IndexOutput::writeByte, IndexInput::readByte);
    }

    @Test
    public void shouldReadAndWriteShort() throws Exception
    {
        test((short)7, IndexOutput::writeShort, IndexInput::readShort);
        test((short)0, IndexOutput::writeShort, IndexInput::readShort);
        test((short)-1, IndexOutput::writeShort, IndexInput::readShort);
        test(Short.MAX_VALUE, IndexOutput::writeShort, IndexInput::readShort);
        test(Short.MIN_VALUE, IndexOutput::writeShort, IndexInput::readShort);
    }

    @Test
    public void shouldReadAndWriteInteger() throws Exception
    {
        test(7, IndexOutput::writeInt, IndexInput::readInt);
        test(0, IndexOutput::writeInt, IndexInput::readInt);
        test(-1, IndexOutput::writeInt, IndexInput::readInt);
        test(Integer.MAX_VALUE, IndexOutput::writeInt, IndexInput::readInt);
        test(Integer.MIN_VALUE, IndexOutput::writeInt, IndexInput::readInt);
    }

    @Test
    public void shouldReadAndWriteLong() throws Exception
    {
        test(7L, IndexOutput::writeLong, IndexInput::readLong);
        test(0L, IndexOutput::writeLong, IndexInput::readLong);
        test(-1L, IndexOutput::writeLong, IndexInput::readLong);
        test(Long.MAX_VALUE, IndexOutput::writeLong, IndexInput::readLong);
        test(Long.MIN_VALUE, IndexOutput::writeLong, IndexInput::readLong);
    }

    @Test
    public void shouldReadAndWriteBytes() throws Exception
    {
        testByteArray( new byte[]{1,3,3,-1});
        testByteArray( new byte[]{(byte) 8});
        testByteArray( new byte[]{});

        byte[] bigAsPage = new byte[pc.pageSize()];
        Arrays.fill( bigAsPage, (byte)-1 );
        testByteArray( bigAsPage );

        byte[] bigAsPagePlusOne = new byte[pc.pageSize() + 1];
        Arrays.fill( bigAsPagePlusOne, (byte)-1 );
        testByteArray( bigAsPagePlusOne );
    }

    private void testByteArray( byte[] val ) throws IOException
    {
        testByteArray(val, 0);
        testByteArray(val, 1);
        testByteArray(val, val.length);
        testByteArray(val, val.length + 1);
        testByteArray(val, pc.pageSize() - 1);
        testByteArray(val, pc.pageSize());
        testByteArray(val, pc.pageSize() + 1);
        if(val.length > 0)
        {
            testByteArray( val, val.length - 1 );
        }
    }

    private void testByteArray( byte[] val, int readOffset ) throws IOException
    {
        ThrowingBiConsumer<IndexOutput,byte[],IOException> write = (o, v) -> o.writeBytes( v, v.length );
        ThrowingFunction<IndexInput,byte[],IOException> read = (i) -> {
            byte[] actual = new byte[val.length + readOffset];
            i.readBytes( actual, readOffset, val.length );
            return Arrays.copyOfRange(actual, readOffset, readOffset + val.length);
        };
        test( val, write, read, Arrays::equals);
    }

    @Before
    public void setup() throws IOException
    {
        pc = this.pageCache.getPageCache( fs );
        // Just in case someone wants standard file system, we have an actual temp dir
        Path workDir = tmpDir.newFolder().toPath();
        fs.mkdirs( workDir.toFile() );
        dir = new PagedDirectory( workDir, pc );
    }

    private <T> void test( T val, ThrowingBiConsumer<IndexOutput,T,IOException> write, ThrowingFunction<IndexInput,T,IOException> read ) throws IOException
    {
        test(val, write, read, Object::equals);
    }

    private <T> void test( T val, ThrowingBiConsumer<IndexOutput,T,IOException> write, ThrowingFunction<IndexInput,T,IOException> read, BiFunction<T,T,Boolean> equals ) throws IOException
    {
        // Test multiple offsets at the beginning on a page
        for ( int offset = 0; offset < 3; offset++ )
        {
            test( val, write, read, equals, offset );
        }
        // Test multiple offsets at page boundary
        for ( int offset = pc.pageSize() - 16; offset < pc.pageSize()+1; offset++ )
        {
            test( val, write, read, equals, offset );
        }
    }

    private <T> void test( T val,
            ThrowingBiConsumer<IndexOutput,T,IOException> write,
            ThrowingFunction<IndexInput,T,IOException> read,
            BiFunction<T,T,Boolean> equals, int offset ) throws IOException
    {
        String fileName = testFileName( "offset" + offset );

        // Write the value
        try(IndexOutput out = dir.createOutput( fileName, IOContext.DEFAULT ))
        {
            out.writeBytes( new byte[offset], offset );
            write.accept( out, val );
        }

        // Read it back
        try(IndexInput in = dir.openInput( fileName, IOContext.READ ))
        {
            in.seek( offset );
            T actual = read.apply( in );

            String expected = val instanceof byte[] ? Arrays.toString( (byte[])val ) : val.toString();
            String found = actual instanceof byte[] ? Arrays.toString( (byte[])actual ) : actual.toString();
            assert equals.apply( val, actual ) : String.format("Expected %s, got %s, when offset by %d", expected, found, offset);
        }

    }

    private static String testFileName(String suffix) {
        // Just to create a file name that's at least marginally helpful during debugging
        StringBuilder name = new StringBuilder(  );
        StackTraceElement testMethod = Thread.currentThread().getStackTrace()[4];
        name.append( testMethod.getMethodName() ).append( "L" ).append( testMethod.getLineNumber() ).append( "." );
        return name.append( suffix ).toString();
    }
}
