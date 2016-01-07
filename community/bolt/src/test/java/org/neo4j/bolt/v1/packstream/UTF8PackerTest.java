package org.neo4j.bolt.v1.packstream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.neo4j.bolt.v1.packstream.utf8.UTF8Packer;
import org.neo4j.bolt.v1.packstream.utf8.VanillaUTF8Encoder;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith( Parameterized.class )
public class UTF8PackerTest
{
    @Parameterized.Parameter
    public UTF8Packer packer;

    @Parameterized.Parameters
    public static List<Object[]> params()
    {
        return asList(
            new Object[]{ new UTF8Packer() },
            new Object[]{ new UTF8Packer(new VanillaUTF8Encoder()) }
        );
    }

    @Test
    public void shouldPackBasicStrings() throws Throwable
    {
        // Given
        String val = "123abc";
        PackedOutputArray out = new PackedOutputArray();

        // When
        packer.pack( val, out );

        // Then
        assertThat( new String(out.bytes(), StandardCharsets.UTF_8 ), equalTo( val ));
    }
}