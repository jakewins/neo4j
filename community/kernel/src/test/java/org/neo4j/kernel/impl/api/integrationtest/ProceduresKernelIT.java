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
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.SchemaWriteOperations;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTInteger;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTText;

public class ProceduresKernelIT extends KernelIntegrationTest
{
    @Test
    public void shouldCreateProcedure() throws Throwable
    {
        // Given
        SchemaWriteOperations ops = schemaWriteOperationsInNewTransaction();
        ProcedureSignature signature = procedureSignature( new String[]{"neo4j"}, "exampleProc" )
                .in( "name", NTText )
                .out( "age", NTInteger ).build();

        // When
        ops.procedureCreate( signature, "javascript", "yield record(1);\n" );

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
    public void shouldBeAbleToInvokeSimpleProcedure() throws Throwable
    {
        // Given
        ProcedureSignature signature = procedureSignature( new String[]{"neo4j"}, "exampleProc" )
                .in( "name", NTText )
                .out( "name", NTInteger ).build();

        // Create a procedure
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
        assertTrue( Arrays.equals(new Object[]{"hello"}, res.getRecord() ));
        assertFalse( res.next() );
    }
}
