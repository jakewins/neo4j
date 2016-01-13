package org.neo4j.proc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ProcedureSignatureTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private final ProcedureSignature signature = ProcedureSignature.procedureSignature( "asd" ).in( "a", Neo4jTypes.NTAny ).build();

    @Test
    public void inputSignatureShouldNotBeModifiable() throws Throwable
    {
        // Expect
        exception.expect( UnsupportedOperationException.class );

        // When
        signature.inputSignature().add( new ProcedureSignature.FieldSignature( "b", Neo4jTypes.NTAny ) );
    }

    @Test
    public void outputSignatureShouldNotBeModifiable() throws Throwable
    {
        // Expect
        exception.expect( UnsupportedOperationException.class );

        // When
        signature.outputSignature().add( new ProcedureSignature.FieldSignature( "b", Neo4jTypes.NTAny ) );
    }
}