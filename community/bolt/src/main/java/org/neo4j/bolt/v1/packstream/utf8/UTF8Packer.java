package org.neo4j.bolt.v1.packstream.utf8;

import java.io.IOException;

import org.neo4j.bolt.v1.packstream.PackOutput;

public class UTF8Packer
{
    private final Encoder encoder;

    public UTF8Packer()
    {
        this( fastestAvailableEncoder() );
    }

    public UTF8Packer( Encoder encoder )
    {
        this.encoder = encoder;
    }

    private static Encoder fastestAvailableEncoder()
    {
        Encoder enc;
        try
        {
            enc = (Encoder)Class.forName("org.neo4j.bolt.v1.packstream.utf8.SunMiscUTF8Encoder").getConstructor().newInstance();
        }
        catch ( Exception e )
        {
            enc = new VanillaUTF8Encoder();
        }
        return enc;
    }

    public void pack( String input, PackOutput output ) throws IOException
    {
        encoder.encode( input, output );
    }
}
