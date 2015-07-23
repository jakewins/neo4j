package org.neo4j.kernel.impl.procedures.es6;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTInteger;

public class ES6Test
{

    @Test
    public void shouldCompileGeneratorSyntax() throws Throwable
    {
        assertThat( exec( procedureSignature( "f" ).out( "number", NTInteger ).build(), "yield [1];" ), yields( record( 1l ) ) );
    }

    @Test
    public void shouldAcceptArguments() throws Throwable
    {
        assertThat( exec( procedureSignature( "f" ).in("arg", NTInteger ).out( "number", NTInteger ).build(), "yield [arg];", 6l ), yields( record( 6l ) ) );
    }

    public static List<List<Object>> exec( ProcedureSignature sig, String script, Object ... args ) throws ProcedureException
    {
        Statement statement = mock(Statement.class);

        RecordCursor cursor = new EcmaScript6LanguageHandler().compile( null, sig, script ).call( statement, args );
        List<List<Object>> records = new LinkedList<>();
        while(cursor.next())
        {
            records.add( Arrays.asList( cursor.record() ) );
        }
        return records;
    }

    public static Matcher<List<List<Object>>> yields( final Matcher<List<Object>>... records )
    {
        return new TypeSafeMatcher<List<List<Object>>>()
        {
            @Override
            protected boolean matchesSafely( List<List<Object>> item )
            {
                int idx = 0;
                for ( List<Object> record : item )
                {
                    if( idx >= records.length )
                    {
                        return false;
                    }
                    if( !records[idx++].matches( record ) )
                    {
                        return false;
                    }
                }
                return idx == records.length;
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendList( "[", ",", "]", asList( records ) );
            }
        };
    }

    public static Matcher<List<Object>> record( final Object ... expected )
    {
        return equalTo( asList( expected ) );
    }


}