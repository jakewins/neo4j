package org.neo4j.kernel.impl.procedures.es6;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.util.Arrays;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;

public class ES6Test
{
    private final Statement statement = mock(Statement.class);

    @Test
    public void shouldCompileGeneratorSyntax() throws Throwable
    {
        assertThat( exec( procedureSignature("f").build(), "while(true) { yield 1; }" ), yields( record( 1l ) ) );
    }

    private RecordCursor exec( ProcedureSignature sig, String script, Object ... args ) throws ProcedureException
    {
        return new EcmaScript6LanguageHandler().compile( null, sig, script ).call( statement, args );
    }

    private Matcher<RecordCursor> yields( final Matcher<Object[]>... records )
    {
        return new TypeSafeMatcher<RecordCursor>()
        {
            @Override
            protected boolean matchesSafely( RecordCursor item )
            {
                int idx = 0;
                while(item.next())
                {
                    if( idx >= records.length )
                    {
                        return false;
                    }
                    if( !records[idx++].matches( item.getRecord() ) )
                    {
                        return false;
                    }
                }
                return idx == records.length;
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendList( "RecordCursor[", ",", "]", asList( records ) );
            }
        };
    }

    private Matcher<Object[]> record( final Object ... expected )
    {
        return new TypeSafeMatcher<Object[]>()
        {
            @Override
            public void describeTo( Description description )
            {
                description.appendValueList( "record[", ",", "]", expected );
            }

            @Override
            protected boolean matchesSafely( Object[] item )
            {
                return Arrays.equals( expected, item );
            }
        };
    }
}