/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api.integrationtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.neo4j.cursor.Cursor;
import org.neo4j.graphdb.Direction;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.DataWriteOperations;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.impl.util.Cursors;
import org.neo4j.register.LongRegister;
import org.neo4j.register.ObjectRegister;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.neo4j.kernel.impl.util.Cursors.countDownCursor;

public class TraverseIT extends KernelIntegrationTest
{
    private int relType1;
    private int relType2;

    @Test
    public void shouldTraverseBothDirections() throws Exception
    {
        // Given
        long nodeId = createGraph();

        ReadOperations ops = readOperationsInNewTransaction();
        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        // When
        Cursor cursor = ops.traverse( countDownCursor( 1 ),
                new LongRegister(nodeId), new ObjectRegister<>( new int[]{relType1} ),
                new ObjectRegister<>(Direction.BOTH), relId, neighborId );

        // Then
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
           equalTo( asList(
                   Pair.of(0l, 0l), Pair.of(1l, 0l), Pair.of(2l, 1l), Pair.of(3l, 1l),
                   Pair.of(4l, 1l), Pair.of(5l, 1l), Pair.of(6l, 2l), Pair.of(7l, 2l),
                   Pair.of(8l, 2l), Pair.of(9l, 2l)
           )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );
    }

    @Test
    public void shouldTraverseIncoming() throws Exception
    {
        // Given
        long nodeId = createGraph();

        ReadOperations ops = readOperationsInNewTransaction();
        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        // When
        Cursor cursor = ops.traverse( countDownCursor( 1 ),
                new LongRegister(nodeId), new ObjectRegister<>( new int[]{relType1}),
                new ObjectRegister<>(Direction.INCOMING), relId, neighborId );

        // Then
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
                equalTo( asList(
                        Pair.of(0l, 0l), Pair.of(1l, 0l), Pair.of(4l, 1l),
                        Pair.of(5l, 1l), Pair.of(8l, 2l), Pair.of(9l, 2l)
                )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );
    }

    @Test
    public void shouldTraverseIncomingFromNodeCreatedInCurrentTx() throws Exception
    {
        // Given
        createGraph();
        DataWriteOperations ops = dataWriteOperationsInNewTransaction();

        long nodeId = ops.nodeCreate();
        ops.relationshipCreate( relType1, nodeId, ops.nodeCreate() );
        ops.relationshipDelete( ops.relationshipCreate( relType1, nodeId, ops.nodeCreate() ) ); // Shouldn't show up

        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        // When
        Cursor cursor = ops.traverse( countDownCursor( 1 ),
                new LongRegister(nodeId), new ObjectRegister<>( new int[]{relType1}),
                new ObjectRegister<>(Direction.OUTGOING), relId, neighborId );

        // Then
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
                equalTo( asList(
                        Pair.of(12l, 6l)
                )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );
    }

    @Test
    public void shouldIncludeTxLocalChangesOnIncoming() throws Exception
    {
        // Given
        long nodeId = createGraph();

        DataWriteOperations ops = dataWriteOperationsInNewTransaction();
        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        ops.relationshipDelete( 0l );
        ops.relationshipCreate( relType1, nodeId, ops.nodeCreate() );
        ops.relationshipCreate( relType1, ops.nodeCreate(), nodeId );

        // When
        Cursor cursor = ops.traverse( countDownCursor( 1 ),
                new LongRegister(nodeId), new ObjectRegister<>( new int[]{relType1}),
                new ObjectRegister<>(Direction.INCOMING), relId, neighborId );

        // Then
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
                equalTo( asList(
                        Pair.of(1l, 0l), Pair.of(4l, 1l),
                        Pair.of(5l, 1l), Pair.of(8l, 2l), Pair.of(9l, 2l),
                        Pair.of(13l, 6l)
                )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );
    }

    /**
     * This tests that we can give an input cursor that several input rows, and that the output cursor gives us
     * a single continuous stream of outputs from that.
     */
    @Test
    public void shouldAllowMultipleInputRowsWithLocalTxState() throws Exception
    {
        // Given
        final long nodeId = createGraph();

        DataWriteOperations ops = dataWriteOperationsInNewTransaction();
        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        ops.relationshipDelete( 0l );
        ops.relationshipCreate( relType1, nodeId, ops.nodeCreate() );
        ops.relationshipCreate( relType1, ops.nodeCreate(), nodeId );
        ops.relationshipCreate( relType2, nodeId, ops.nodeCreate() );


        final LongRegister nodeRegister = new LongRegister( -1 );
        final ObjectRegister<int[]> typesRegister = new ObjectRegister<>( new int[]{-1} );
        final ObjectRegister<Direction> directionRegister = new ObjectRegister<>( null );

        // An input cursor that contains two rows
        Cursor inputCursor = new Cursor()
        {
            private int count = 0;

            @Override
            public boolean next()
            {
                switch ( count++ )
                {
                    case 0:
                        nodeRegister.write( nodeId );
                        typesRegister.write( new int[]{relType1} );
                        directionRegister.write( Direction.INCOMING );
                        return true;
                    case 1:
                        nodeRegister.write( nodeId );
                        typesRegister.write( new int[]{relType2} ); // Different rel type
                        directionRegister.write( Direction.OUTGOING );
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void reset()
            {

            }

            @Override
            public void close()
            {

            }
        };

        // When
        Cursor cursor = ops.traverse( inputCursor, nodeRegister, typesRegister, directionRegister, relId, neighborId );

        // And it should contain this
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
                equalTo( asList(
                        // Note that this is not the order the rows are returned in, the rows are sorted for
                        // predictability in the test.
                        Pair.of(1l, 0l), Pair.of(4l, 1l),
                        Pair.of(5l, 1l), Pair.of(8l, 2l), Pair.of(9l, 2l),
                        Pair.of(10l, 3l), Pair.of(11l, 4l), Pair.of(13l, 6l), Pair.of(14l, 7l)
                )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );
    }

    /**
     * This tests that we can give an input cursor that several input rows, and that the output cursor gives us
     * a single continuous stream of outputs from that.
     */
    @Test
    public void shouldAllowMultipleInputRows() throws Exception
    {
        // Given
        final long nodeId = createGraph();

        // And given a node with no rels
        DataWriteOperations write = dataWriteOperationsInNewTransaction();
        final long emptyNodeId = write.nodeCreate();
        commit();

        ReadOperations ops = readOperationsInNewTransaction();
        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        final LongRegister nodeRegister = new LongRegister( -1 );
        final ObjectRegister<int[]> typesRegister = new ObjectRegister<>( new int[]{-1} );
        final ObjectRegister<Direction> directionRegister = new ObjectRegister<>( null );

        // An input cursor that contains two rows
        Cursor inputCursor = new Cursor()
        {
            private int count = 0;

            @Override
            public boolean next()
            {
                switch ( count++ )
                {
                    case 0:
                        nodeRegister.write( nodeId );
                        typesRegister.write( new int[]{relType1} );
                        directionRegister.write( Direction.INCOMING );
                        return true;
                    case 1:
                        nodeRegister.write(emptyNodeId);
                        typesRegister.write( new int[]{relType1} );
                        directionRegister.write( Direction.INCOMING );
                        return true;
                    case 2:
                        nodeRegister.write( emptyNodeId + 999 ); // Does not exist
                        typesRegister.write( new int[]{relType1} );
                        directionRegister.write( Direction.INCOMING );
                        return true;
                    case 3:
                        nodeRegister.write( nodeId );
                        typesRegister.write( new int[]{relType2} ); // Different rel type
                        directionRegister.write( Direction.OUTGOING );
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void reset()
            {

            }

            @Override
            public void close()
            {

            }
        };

        // When
        Cursor cursor = ops.traverse( inputCursor, nodeRegister, typesRegister, directionRegister, relId, neighborId );

        // And it should contain this
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
                equalTo( asList(
                        // Note that this is not the order the rows are returned in, the rows are sorted for
                        // predictability in the test.
                        Pair.of(0l, 0l), Pair.of(1l, 0l), Pair.of(4l, 1l),
                        Pair.of(5l, 1l), Pair.of(8l, 2l), Pair.of(9l, 2l),
                        Pair.of(10l, 3l), Pair.of(11l, 4l)
                )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );
    }

    @Test
    public void resetAndCloseShouldPropagateWithLocalTxState() throws Exception
    {
        // Given
        long nodeId = createGraph();

        DataWriteOperations ops = dataWriteOperationsInNewTransaction();
        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        ops.relationshipDelete( 0l );
        ops.relationshipCreate( relType1, nodeId, ops.nodeCreate() );
        ops.relationshipCreate( relType1, ops.nodeCreate(), nodeId );

        Cursor inputCursor = spy( new Cursors.CountDownCursor( 1 ));

        Cursor cursor = ops.traverse( inputCursor,
                new LongRegister(nodeId), new ObjectRegister<>( new int[]{relType1}),
                new ObjectRegister<>(Direction.INCOMING), relId, neighborId );

        // And given I've exhausted the cursor
        while(cursor.next());

        // When
        cursor.reset();

        // Then
        verify(inputCursor).reset();
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
                equalTo( asList(
                        Pair.of(1l, 0l), Pair.of(4l, 1l),
                        Pair.of(5l, 1l), Pair.of(8l, 2l), Pair.of(9l, 2l),
                        Pair.of(13l, 6l)
                )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );

        // And When
        cursor.close();

        // Then
        verify(inputCursor).close();
    }

    @Test
    public void resetAndCloseShouldPropagate() throws Exception
    {
        // Given
        long nodeId = createGraph();

        ReadOperations ops = readOperationsInNewTransaction();
        LongRegister relId = new LongRegister();
        LongRegister neighborId = new LongRegister();

        Cursor inputCursor = spy( new Cursors.CountDownCursor( 1 ));
        Cursor cursor = ops.traverse( inputCursor,
                new LongRegister(nodeId), new ObjectRegister<>( new int[]{relType1}),
                new ObjectRegister<>(Direction.INCOMING), relId, neighborId );

        // And given I've exhausted the cursor
        while(cursor.next());

        // When
        cursor.reset();

        // Then
        verify(inputCursor).reset();
        assertThat( toRelAndNeighborPairs( cursor, relId, neighborId ),
                equalTo( asList(
                        Pair.of(0l, 0l), Pair.of(1l, 0l), Pair.of(4l, 1l),
                        Pair.of(5l, 1l), Pair.of(8l, 2l), Pair.of(9l, 2l)
                )) );
        assertFalse( "Should not contain any more rows.", cursor.next() );

        // And When
        cursor.close();

        // Then
        verify(inputCursor).close();
    }

    private long createGraph() throws KernelException
    {
        long nodeId;
        {
            DataWriteOperations ops = dataWriteOperationsInNewTransaction();
            relType1 = ops.relationshipTypeGetOrCreateForName( "TYPE1" );
            relType2 = ops.relationshipTypeGetOrCreateForName( "TYPE2" );
            nodeId = ops.nodeCreate();

            // Two loop rels
            ops.relationshipCreate( relType1, nodeId, nodeId );
            ops.relationshipCreate( relType1, nodeId, nodeId );

            // 2 * 4 "regular" rels
            for ( int i = 0; i < 2; i++ )
            {
                long target = ops.nodeCreate();

                // 2 outgoing
                ops.relationshipCreate( relType1, nodeId, target );
                ops.relationshipCreate( relType1, nodeId, target );

                // 2 incoming
                ops.relationshipCreate( relType1, target, nodeId );
                ops.relationshipCreate( relType1, target, nodeId );
            }

            // 2 outgoing reltype2
            ops.relationshipCreate( relType2, nodeId, ops.nodeCreate() );
            ops.relationshipCreate( relType2, nodeId, ops.nodeCreate() );

            commit();
        }
        return nodeId;
    }

    private List<Pair<Long, Long>> toRelAndNeighborPairs( Cursor cursor, LongRegister relId, LongRegister neighborId )
    {
        List<Pair<Long, Long>> result = new ArrayList<>();
        while(cursor.next())
        {
            result.add( Pair.of( relId.read(), neighborId.read() ) );
        }
        Collections.sort(result, new Comparator<Pair<Long, Long>>()
        {
            @Override
            public int compare( Pair<Long, Long> o1, Pair<Long, Long> o2 )
            {
                if( o1.first().equals( o2.first() ))
                {
                    return (int) (o1.other() - o2.other());
                }
                else
                {
                    return (int) (o1.first() - o2.first());
                }
            }
        });
        return result;
    }

}
