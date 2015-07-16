package org.neo4j.kernel.impl.api.integrationtest;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.neo4j.kernel.api.ReadOperations;
import org.neo4j.kernel.api.SchemaWriteOperations;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.Neo4jType.INTEGER;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.Neo4jType.TEXT;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;

public class ProceduresKernelIT extends KernelIntegrationTest
{
    @Test
    public void shouldCreateProcedure() throws Throwable
    {
        // Given
        SchemaWriteOperations ops = schemaWriteOperationsInNewTransaction();
        ProcedureSignature signature = procedureSignature( new String[]{"neo4j"}, "exampleProc" )
                .in( "name", TEXT )
                .out( "age", INTEGER ).build();

        // When
        ops.procedureCreate( signature, "javascript", streamOf( "yield record(1);\n" ) );

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
                .in( "name", TEXT )
                .out( "name", TEXT ).build();

        // Create a procedure
        {
            SchemaWriteOperations ops = schemaWriteOperationsInNewTransaction();

            ops.procedureCreate( signature, "javascript", streamOf( "yield record(name);\n" ) );
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

    private InputStream streamOf( String s )
    {
        return new ByteArrayInputStream( s.getBytes( StandardCharsets.UTF_8 ) );
    }
}
