package org.neo4j.kernel.impl.procedures.es6;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.RecordCursor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.procedures.es6.ES6SoftDependency.loadES6;

public class ES6ErrorTest
{
    @Test
    public void syntaxError() throws Throwable
    {
        compiling( "this makes no sense", yieldsError( "" ) );
        compiling("\n\nthis makes no sense", yieldsError(""));
    }

    @Test
    public void runtimeError() throws Throwable
    {
        running( "yield aMethodThatDoesNotExist();", yieldsError( "" ) );
    }

    private void running( String script, Matcher<ProcedureException> expectedException ) throws ProcedureException
    {
        Procedure proc = compile( script );
        try
        {
            RecordCursor cursor = proc.call( mock( Statement.class ), new Object[0] );
            cursor.next();
            assertThat(null, expectedException);
        }
        catch( ProcedureException e )
        {
            assertThat(e, expectedException);
        }
    }

    private void compiling( String script, Matcher<ProcedureException> errorMatcher )
    {
        try
        {
            compile( script );
            assertThat( null, errorMatcher );
        }
        catch ( ProcedureException e )
        {
            assertThat( e, errorMatcher );
        }
    }

    private Procedure compile( String script ) throws ProcedureException
    {
        LanguageHandler js = loadES6();

        Statement stmt = mock( Statement.class );
        return js.compile( stmt, procedureSignature( "p" ).build(), script );
    }

    private Matcher<ProcedureException> yieldsError( final String message )
    {
        return new TypeSafeMatcher<ProcedureException>()
        {
            @Override
            protected boolean matchesSafely( ProcedureException item )
            {
                return item.getMessage().equals( message );
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendText( String.format("ProcedureException['%s']", message) );
            }
        };
    }
}