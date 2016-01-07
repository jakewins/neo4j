package org.neo4j.bolt.v1.packstream.utf8;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.neo4j.bolt.v1.packstream.PackOutput;

public class VanillaUTF8Encoder implements Encoder
{
    @Override
    public void encode( String input, PackOutput output ) throws IOException
    {
        output.writeBytes( ByteBuffer.wrap( input.getBytes( StandardCharsets.UTF_8 ) ) );
    }
}
