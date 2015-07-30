/*
 * Copyright (c) 2002-2015 "Neo Technology,"
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

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.TopLevelTransaction;
import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.SchemaWriteOperations;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.store.Neo4jTypes;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTInteger;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTNode;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTText;
import static org.neo4j.kernel.impl.util.IoPrimitiveUtils.readAsString;

public class ProceduresKernelIT extends KernelIntegrationTest
{
    public static final Label PRODUCT = DynamicLabel.label( "PRODUCT" );

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final ProcedureSignature signature = procedureSignature( "example", "exampleProc" )
                                                .in( "name", NTText )
                                                .out( "name", NTText ).build();

    @Test
    public void shouldCreateProcedure() throws Throwable
    {
        // Given
        SchemaWriteOperations ops = schemaWriteOperationsInNewTransaction();

        // When
        ops.procedureCreate( signature, "javascript", "yield record(1);" );

        // Then
        assertThat( asCollection( ops.proceduresGetAll() ),
                Matchers.<Collection<ProcedureSignature>>equalTo( asList( signature ) ) );

        // And when
        commit();

        // Then
        assertThat( asCollection( readOperationsInNewTransaction().proceduresGetAll() ),
                Matchers.<Collection<ProcedureSignature>>equalTo( asList( signature ) ) );
    }

    @Test
    public void shouldGetProcedureByName() throws Throwable
    {
        // Given
        shouldCreateProcedure();

        // When
        ProcedureSignature found = readOperationsInNewTransaction().procedureGetSignature( new String[]{"example"}, "exampleProc" );

        // Then
        assertThat( found, equalTo( found ));
    }

    @Test
    public void nonexistantProcedureShouldThrow() throws Throwable
    {
        // Expect
        exception.expect( ProcedureException.class );

        // When
        readOperationsInNewTransaction().procedureGetSignature( new String[]{"example"}, "exampleProc" );
    }

    @Test
    public void shouldBeAbleToInvokeSimpleProcedure() throws Throwable
    {
        // Given
        {
            SchemaWriteOperations ops = schemaWriteOperationsInNewTransaction();

            ops.procedureCreate( signature, "javascript", "yield record(name);\n" );
            commit();
        }

        ReadOperations ops = readOperationsInNewTransaction();

        // When
        RecordCursor res = ops.procedureCall( signature, new Object[]{"hello"} );

        // Then
        assertTrue( res.next() );
        assertTrue( Arrays.equals( new Object[]{"hello"}, res.record() ) );
        assertFalse( res.next() );

        res.close();
    }

    @Test
    public void shouldBeAbleToCallProcedureFromProcedure() throws KernelException
    {
        // TODO Cleanup
        // Given
        try ( Transaction tx = db.beginTx() )
        {
            db.schema().indexFor( PRODUCT ).on( "ID" ).create();
            tx.success();
        }

        try ( Transaction tx = db.beginTx() )
        {
            db.schema().awaitIndexesOnline( 10, TimeUnit.SECONDS );

            Node product = db.createNode( PRODUCT );
            product.setProperty( "ID", "product1" );

            Node root = db.createNode();

            product.createRelationshipTo( root, DynamicRelationshipType.withName( "PARENT" ) );

            Node promo = db.createNode();
            promo.setProperty( "PROMOTION_ID", "promo1" );
            promo.setProperty( "PARENT", "promo2" );

            product.createRelationshipTo( promo, DynamicRelationshipType.withName( "APPLY" ) );

            Node promo2 = db.createNode();
            promo2.setProperty( "PROMOTION_ID", "promo2" );
            promo2.setProperty( "PARENT", "promo3" );

            root.createRelationshipTo( promo2, DynamicRelationshipType.withName( "EXCLUDE" ) );

            tx.success();
        }

        // Create procedures
        ProcedureSignature exampleProc = procedureSignature( new String[]{"ex"}, "exampleProc" )
                .in( "id", NTText )
                .out( "id", NTText ).out( "parent", NTText ).out( "type", NTText ).build();
        {
            SchemaWriteOperations ops = schemaWriteOperationsInNewTransaction();

            ops.procedureCreate( exampleProc, "javascript", readAsString( getClass().getResourceAsStream( "procedure1.js" ) ));

            ProcedureSignature chain = procedureSignature( new String[]{"procs"}, "getChain" )
                    .in( "id", NTInteger )
                    .out( "link", NTNode).build();
            ops.procedureCreate( chain, "cypher", "MATCH (n)-[:PARENT*0..]->(link) WHERE id(n) = {id} RETURN link"
            );

            commit();
        }

        try ( Transaction tx = db.beginTx() )
        {
            ReadOperations ops = ((TopLevelTransaction) tx).getTransaction().acquireStatement().readOperations();

            // When
            RecordCursor res = ops.procedureCall( exampleProc, new Object[]{"product1"} );

            // Then
            while (res.next())
            {
                Object[] record = res.record();
                for ( int i = 0; i < exampleProc.outputSignature().size(); i++ )
                {
                    Pair<String,Neo4jTypes.AnyType> arg =
                            exampleProc.outputSignature().get( i );
                    if (i > 0)
                        System.out.print(", ");
                    System.out.print(arg.first()+"="+record[i]);
                }
                System.out.println();
            }
            res.close();
/*
            assertTrue( res.next() );
            assertTrue( Arrays.equals( new Object[]{"1", "3", "APPLY"}, res.getRecord() ) );
            assertFalse( res.next() );
*/

            res.close();
        }
    }

    private InputStream streamOf( String s )
    {
        return new ByteArrayInputStream( s.getBytes( StandardCharsets.UTF_8 ) );
    }
}
