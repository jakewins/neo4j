package org.neo4j.kernel.impl.nioneo.xa;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.kernel.impl.transaction.xaframework.InMemoryLogBuffer;
import org.neo4j.test.EphemeralFileSystemRule;

import static java.nio.ByteBuffer.allocate;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo4j.kernel.impl.nioneo.xa.Command.readCommand;

public class IndexUpdateCommandTest
{
    @Rule
    public EphemeralFileSystemRule fs = new EphemeralFileSystemRule();

    @Test
    public void shouldSerializeAndDeserializeAddCommand() throws Exception
    {
        // Given
        long nodeId = 1337l;
        int propertyKey = 1;
        Command.IndexUpdateCommand command = new Command.IndexUpdateCommand( null,
                Command.Mode.CREATE, nodeId, propertyKey,
                null, "something", null, new int[]{1} );

        // When
        Command.IndexUpdateCommand result = serializeAndRead( command );

        // Then
        assertThat( result.getMode(), equalTo( Command.Mode.CREATE ) );
        assertThat( result.getKey(), equalTo( nodeId ));
        assertThat( result.getPropertyId(), equalTo( propertyKey ));
        assertThat( result.getValueBefore(), equalTo( null ));
        assertThat( (String)result.getValueAfter(), equalTo( "something" ));
        assertThat( result.getLabelsBefore(), equalTo( null ));
        assertThat( result.getLabelsAfter(), equalTo( new int[]{1} ));
    }

    @Test
    public void shouldSerializeAndDeserializeArbitraryPropertyValues() throws Exception
    {
        assertSerializesValue( "A String!!" );
        assertSerializesValue( "A looaoooaoooooaooaooooaong String!!          12 12 12 " );

        assertSerializesValue( 12 );
        assertSerializesValue( 12l );
        assertSerializesValue( 12.0d );
        assertSerializesValue( 12.0f );

        assertSerializesValue( true );
        assertSerializesValue( false );

        assertSerializesValue( new byte[]{} );
        assertSerializesValue( new byte[]{0x1,0x2} );
        assertSerializesValue( new short[]{} );
        assertSerializesValue( new short[]{0x1,0x2} );
        assertSerializesValue( new int[]{} );
        assertSerializesValue( new int[]{0x1,0x2} );
        assertSerializesValue( new long[]{} );
        assertSerializesValue( new long[]{0x1,0x2} );
        assertSerializesValue( new float[]{} );
        assertSerializesValue( new float[]{0x1,0x2} );
        assertSerializesValue( new double[]{} );
        assertSerializesValue( new double[]{0x1,0x2} );
        assertSerializesValue( new boolean[]{} );
        assertSerializesValue( new boolean[]{true,false} );

        assertSerializesValue( new String[]{} );
        assertSerializesValue( new String[]{"adb", "deg"} );
    }

    private void assertSerializesValue( Object value ) throws IOException
    {
        assertThat( serializeAndRead( new Command.IndexUpdateCommand( null,
                Command.Mode.CREATE, 1337l, 1, value, null, null, new int[]{1} ) ).getValueBefore(), equalTo( value ));
        assertThat( serializeAndRead( new Command.IndexUpdateCommand( null,
                Command.Mode.CREATE, 1337l, 1, null, value, null, new int[]{1} ) ).getValueAfter(), equalTo( value ));
    }

    private Command.IndexUpdateCommand serializeAndRead( Command.IndexUpdateCommand cmd ) throws IOException
    {
        InMemoryLogBuffer buffer = new InMemoryLogBuffer();
        cmd.writeToFile( buffer );
        return (Command.IndexUpdateCommand) readCommand( null, null, buffer, allocate( 64 ));
    }
}
