package org.neo4j.kernel.impl.procedures.es6;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.util.SingleNodePath;

import static java.util.Arrays.asList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcedureMatchers
{
    public static final Node node = mock(Node.class);
    public static final Relationship rel = mock(Relationship.class);
    public static final Path path = new SingleNodePath( node );

    public static List<List<Object>> exec( ProcedureSignature sig, String script, Object ... args ) throws Throwable
    {
        Statement statement = mock(Statement.class);
        GraphDatabaseService gds = mock( GraphDatabaseService.class );

        // Basic stubs to access graph primitives, for type system tests
        when(gds.createNode()).thenReturn( node );
        when( node.createRelationshipTo( any( Node.class ), any( RelationshipType.class ) ) ).thenReturn( rel );

        RecordCursor cursor = new ES6LanguageHandler( new ES6StdLib().register( "neo4j.db", gds )).compile( null, sig, script ).call( statement, args );
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
