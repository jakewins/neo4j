package org.neo4j.bolt.v1.packstream.utf8;

import java.io.IOException;

import org.neo4j.bolt.v1.packstream.PackOutput;

interface Encoder
{
    void encode( String input, PackOutput output ) throws IOException;
}
