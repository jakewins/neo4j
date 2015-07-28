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