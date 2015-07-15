package org.neo4j.kernel.impl.api.integrationtest;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.neo4j.kernel.api.SchemaWriteOperations;

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

        // When
        ops.procedureCreate( procedureSignature( new String[]{"neo4j"}, "exampleProc" )
                        .in( "name", TEXT )
                        .out( "age", INTEGER ).build(), "js", streamOf(
                        "{\n" +
                        "  yield record(1);\n" +
                        "}\n" ) );

        // Then
//        assertThat( proc.language(), equalTo( "js" ) );

    }

    private InputStream streamOf( String s )
    {
        return new ByteArrayInputStream( s.getBytes( StandardCharsets.UTF_8 ) );
    }
}
