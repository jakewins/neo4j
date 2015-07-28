package org.neo4j.kernel.impl.procedures.es6;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.procedures.es6.ES6SoftDependency.es6LanguageHandlerAvailable;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTInteger;

public class ES6Test
{

    @Test
    public void shouldCompileGeneratorSyntax() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( ProcedureMatchers.exec( procedureSignature( "f" ).out( "number", NTInteger ).build(), "yield [1];" ), ProcedureMatchers
                .yields( ProcedureMatchers.record( 1l ) ) );
    }

    @Test
    public void shouldAcceptArguments() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( ProcedureMatchers.exec( procedureSignature( "f" ).in( "arg", NTInteger ).out( "number", NTInteger ).build(), "yield [arg];", 6l ), ProcedureMatchers
                .yields( ProcedureMatchers.record( 6l ) ) );
    }


}