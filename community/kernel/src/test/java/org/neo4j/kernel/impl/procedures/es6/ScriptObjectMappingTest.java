package org.neo4j.kernel.impl.procedures.es6;

import org.junit.Test;

import java.util.Map;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.store.Neo4jTypes;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.procedures.es6.ES6SoftDependency.es6LanguageHandlerAvailable;
import static org.neo4j.kernel.impl.procedures.es6.ES6SoftDependency.loadES6;

public class ScriptObjectMappingTest
{
    @Test
    public void shouldConvertEmptyMap() throws Throwable
    {
        // Given
        assumeTrue( es6LanguageHandlerAvailable() );

        // When
        Map<String, Object> map = convertFromJS("{}");

        // Then
        assertEquals( 0, map.size() );
        assertEquals( true, map.isEmpty() );
        assertEquals( emptySet(), map.entrySet() );
        assertEquals( emptySet(), map.keySet() );
        assertEquals( emptyList(), map.values() );
    }

    @Test // Note that more extensive nested tests are located in ES6TypesTest
    public void shouldConvertMapWithSimpleKeys() throws Throwable
    {
        // Given
        assumeTrue( es6LanguageHandlerAvailable() );

        // When
        Map<String, Object> map = convertFromJS("{name:'Steven'}");

        // Then
        assertEquals( 1, map.size() );
        assertEquals( false, map.isEmpty() );
        assertEquals( "name", map.entrySet().iterator().next().getKey() );
        assertEquals( "Steven", map.entrySet().iterator().next().getValue() );
        assertEquals( "name", map.keySet().iterator().next() );
        assertEquals( "Steven", map.values().iterator().next() );
    }

    private Map<String,Object> convertFromJS( String javascript ) throws ProcedureException
    {
        LanguageHandler js = loadES6();

        Statement stmt = mock( Statement.class );
        Procedure proc = js.compile( stmt, procedureSignature( "p" ).out( "out", Neo4jTypes.NTMap ).build(), format( "yield [%s]", javascript ) );

        RecordCursor out = proc.call( stmt, new Object[0] );
        out.next();
        return (Map<String,Object>) out.record()[0];
    }
}