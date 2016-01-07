package org.neo4j.bolt.v1.packstream.utf8;

import sun.nio.cs.ArrayEncoder;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import org.neo4j.bolt.v1.packstream.PackOutput;
import org.neo4j.helpers.Assertion;

public class SunMiscUTF8Encoder implements Encoder
{
    private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();
    private final ArrayEncoder arrayEnc = (ArrayEncoder) charsetEncoder;

    private final byte[] out = new byte[1024 * 16];
    private final ByteBuffer outBuf = ByteBuffer.wrap( out );

    private final MethodHandle getCharArray;

    public SunMiscUTF8Encoder()
    {
        getCharArray = charArrayGetter();
    }

    @Override
    public void encode( String input, PackOutput output ) throws IOException
    {
        try
        {
            char[] invoke = (char[]) getCharArray.invoke( input );
        }
        catch ( Throwable e )
        {
            throw new AssertionError( e );
        }
        int len = this.arrayEnc.encode( input, 0, input.length, out );

        outBuf.position(0);
        outBuf.limit(len);
        output.writeBytes( outBuf );
    }

    private MethodHandle charArrayGetter()
    {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try
        {
            return lookup.unreflectGetter( String.class.getDeclaredField( "value" ) );
        }
        catch ( Exception e )
        {
            throw new AssertionError( e );
        }
    }
}
