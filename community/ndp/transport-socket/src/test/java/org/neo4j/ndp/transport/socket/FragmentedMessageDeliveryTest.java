package org.neo4j.ndp.transport.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.neo4j.kernel.impl.util.HexPrinter;
import org.neo4j.logging.NullLog;
import org.neo4j.ndp.messaging.v1.PackStreamMessageFormatV1;
import org.neo4j.ndp.messaging.v1.RecordingByteChannel;
import org.neo4j.ndp.messaging.v1.message.Message;
import org.neo4j.ndp.runtime.Session;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.neo4j.ndp.messaging.v1.message.Messages.run;
import static org.neo4j.ndp.transport.socket.SocketProtocolV1.State.AWAITING_CHUNK;

/**
 * This tests network fragmentation of messages. Given a set of messages, it will serialize and chunk the message up
 * to a specified chunk size. Then it will split that data into a specified number of fragments, trying every possible
 * permutation of fragment sizes for the specified number. For instance, assuming an unfragmented message size of 15,
 * and a fragment count of 3, it will create fragment size permutations like:
 *
 * [1,1,13]
 * [1,2,12]
 * [1,3,11]
 * ..
 * [12,1,1]
 *
 * For each permutation, it delivers the fragments to the protocol implementation, and asserts the protocol handled
 * them properly.
 */
public class FragmentedMessageDeliveryTest
{
    // Only test one chunk size for now, this can be parameterized to test lots of different ones
    private int chunkSize = 16;

    // Only test messages broken into three fragments for now, this can be parameterized later
    private int numFragments = 3;

    // Only test one message for now. This can be parameterized later to test lots of different ones
    private Message[] messages = new Message[]{run( "Mölnir" )};

    private PackStreamMessageFormatV1.Writer format = new PackStreamMessageFormatV1.Writer();

    @Test
    public void testFragmentedMessageDelivery() throws Throwable
    {
        // Given
        byte[] unfragmented = serialize( chunkSize, messages );

        // When & Then
        testAllPossiblePermutations( unfragmented, 0, 0, new ByteBuf[numFragments] );
    }

    private void testAllPossiblePermutations( byte[] unfragmented, int currentFragmentIndex, int startIndex, ByteBuf[] fragments )
    {
        if( currentFragmentIndex >= fragments.length - 1 )
        {
            // Last fragment, only one possible permutation (last fragment always fills the remaining space)
            fragments[currentFragmentIndex] = wrappedBuffer( unfragmented, startIndex, unfragmented.length - startIndex );
            testPermutation( unfragmented, fragments );
        }
        else
        {
            int numFragments = fragments.length;
            int numSubsequentFragments = numFragments - currentFragmentIndex;
            for ( int fragmentSize = 1; fragmentSize < unfragmented.length - startIndex - numSubsequentFragments;
                  fragmentSize++ )
            {
                assert fragmentSize + startIndex + numSubsequentFragments < unfragmented.length;
                fragments[currentFragmentIndex] = wrappedBuffer( unfragmented, startIndex, fragmentSize );

                testAllPossiblePermutations( unfragmented, currentFragmentIndex + 1, startIndex + fragmentSize, fragments );
            }
        }
    }

    private void testPermutation( byte[] unfragmented, ByteBuf[] fragments )
    {
        // Given
        System.out.println("Testing fragmentation:" + describeFragments( fragments) );
        Session sess = mock( Session.class );
        ChannelHandlerContext ch = mock( ChannelHandlerContext.class );
        SocketProtocolV1 protocol = new SocketProtocolV1( NullLog.getInstance(), sess );

        // When data arrives split up according to the current permutation
        for ( ByteBuf fragment : fragments )
        {
            fragment.readerIndex( 0 ).retain();
            protocol.handle( ch, fragment );
        }

        // Then the session should've received the specified messages, and the protocol should be in a nice clean state
        try
        {
            assertEquals( AWAITING_CHUNK, protocol.state() );
            verify( sess ).run( eq( "Mölnir" ), any( Map.class ), any(), any( Session.Callback.class ) );
        } catch(AssertionError e)
        {
            throw new AssertionError( "Failed to handle fragmented delivery.\n" +
                                "Messages: " + Arrays.toString(messages) + "\n" +
                                "Chunk size: "+chunkSize+"\n" +
                                "Serialized data delivered in fragments: " + describeFragments( fragments) + "\n" +
                                "Unfragmented data: " + HexPrinter.hex( unfragmented ) + "\n", e);
        }
    }

    private String describeFragments( ByteBuf[] fragments )
    {
        StringBuilder sb = new StringBuilder( );
        for ( int i = 0; i < fragments.length; i++ )
        {
            if(i > 0) { sb.append( "," ); }
            sb.append( fragments[i].capacity() );
        }
        return sb.toString();
    }

    private byte[] serialize( int chunkSize, Message... msgs ) throws IOException
    {
        byte[][] serialized = new byte[msgs.length][];
        for ( int i = 0; i < msgs.length; i++ )
        {
            RecordingByteChannel channel = new RecordingByteChannel();
            format.reset( channel ).write( msgs[i] ).flush();
            serialized[i] = channel.getBytes();
        }
        return Chunker.chunk( chunkSize, serialized );
    }
}