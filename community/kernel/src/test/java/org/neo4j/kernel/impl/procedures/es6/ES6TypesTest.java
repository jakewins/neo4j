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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import org.neo4j.kernel.impl.store.Neo4jTypes;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.procedures.es6.JSSoftDependency.es6LanguageHandlerAvailable;
import static org.neo4j.kernel.impl.procedures.es6.ProcedureMatchers.exec;
import static org.neo4j.kernel.impl.procedures.es6.ProcedureMatchers.node;
import static org.neo4j.kernel.impl.procedures.es6.ProcedureMatchers.path;
import static org.neo4j.kernel.impl.procedures.es6.ProcedureMatchers.record;
import static org.neo4j.kernel.impl.procedures.es6.ProcedureMatchers.rel;
import static org.neo4j.kernel.impl.procedures.es6.ProcedureMatchers.yields;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTAny;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTBoolean;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTFloat;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTInteger;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTList;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTMap;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTNode;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTNumber;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTPath;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTRelationship;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTText;

/**
 * This tests tests support of using Neo4j typed values as arguments to procedures, and retrieving them back out. It does not test type errors, nor does
 * it test storage of values and subsequent retrieval. This is still a huge (hundreds of permutations) and slow (lots of javascript compilation) test, but
 * it gives comprehensive coverage for all types users can express, ingest and output via the javascript extensions.
 */
@RunWith( Parameterized.class )
public class ES6TypesTest
{
    @Parameterized.Parameters(name="{0}: {1}")
    public static List<Object[]> parameters()
    {
        return asList(
            new Object[]{ NTInteger, "1", 1l },
            new Object[]{ NTInteger, "-9223372036854775808", Long.MIN_VALUE },
            new Object[]{ NTInteger, "9223372036854775807", Long.MAX_VALUE },

            new Object[]{ NTFloat, "1.0", 1.0d },
            new Object[]{ NTFloat, "-133.0", -133d },

            new Object[]{ NTNumber, "1.1", 1.1d },

            new Object[]{ NTBoolean, "true", true },
            new Object[]{ NTBoolean, "false", false },

            new Object[]{ NTText, "'hello, world!'", "hello, world!" },

            new Object[]{ NTNode, "neo4j.db.createNode()", node },
            new Object[]{ NTRelationship, "neo4j.db.createNode().createRelationshipTo( neo4j.db.createNode(), relType('K') )", rel },
            new Object[]{ NTPath, "new org.neo4j.kernel.impl.util.SingleNodePath(neo4j.db.createNode())", path },

            new Object[]{ NTMap, "{}", map() },
            new Object[]{ NTMap, "neo4j.db.createNode()", node },
            new Object[]{ NTMap, "neo4j.db.createNode().createRelationshipTo( neo4j.db.createNode(), relType('K') )", rel },

            new Object[]{ NTList( NTAny ), "[]", asList() },
            new Object[]{ NTList( NTInteger ), "[1,2,3]", asList(1l,2l,3l) }
        );
    }

    @Parameterized.Parameter( 0 )
    public Neo4jTypes.AnyType type;

    @Parameterized.Parameter( 1 )
    public String javascriptExpression;

    @Parameterized.Parameter( 2 )
    public Object javaExpression;

    @Test
    public void shouldHandleTypeAsInput() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).in( "arg", type ).out( "out", type ).build(), "yield [arg]", javaExpression ),
                  yields( record( javaExpression ) ));
    }

    @Test
    public void shouldHandleTypeAsOutput() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).out( "out", type ).build(), String.format( "yield [%s]", javascriptExpression ) ),
                yields( record( javaExpression ) ) );
    }

    @Test
    public void shouldHandleTypeAsInputWithAnyAsSignature() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).in( "arg", NTAny ).out( "out", NTAny ).build(), "yield [arg]", javaExpression ),
                yields( record( javaExpression ) ));
    }

    @Test
    public void shouldHandleTypeAsOutputWithAnyAsSignature() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        // TODO: Look into raising an issue/sending a patch to Nashorn to remove below assumption
        assumeTrue( "Nashorn cannot handle big negative longs, treats them as doubles",
                !(javaExpression instanceof Long && javaExpression.equals( Long.MIN_VALUE )) );
        assertThat( exec( procedureSignature( "f" ).out( "out", NTAny ).build(), String.format( "yield [%s]", javascriptExpression ) ),
                yields( record( javaExpression ) ) );
    }

    // Nesting inside List

    @Test
    public void shouldHandleTypeAsOutputNestedInList() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).out( "out", NTList( type ) ).build(), String.format( "yield [ [%s] ]", javascriptExpression ) ),
                yields( record( asList(javaExpression) ) ) );
    }

    @Test
    public void shouldHandleTypeAsInputNestedInList() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).in( "arg", NTList( type ) ).out( "out", NTList( type ) ).build(), "yield [arg]", asList( javaExpression ) ),
                yields( record( asList(javaExpression) ) ));
    }

    @Test
    public void shouldHandleTypeAsInputNestedInListWithAnyAsSignature() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).in( "arg", NTAny ).out( "out", NTAny ).build(), "yield [ arg ]", asList( javaExpression ) ),
                yields( record( asList(javaExpression) ) ));
    }

    @Test
    public void shouldHandleTypeAsOutputNestedInListWithAnyAsSignature() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assumeTrue( "Nashorn cannot handle big negative longs, treats them as doubles",
                !(javaExpression instanceof Long && javaExpression.equals( Long.MIN_VALUE )) );
        assertThat( exec( procedureSignature( "f" ).out( "out", NTAny ).build(), String.format( "yield [ [%s] ]", javascriptExpression ) ),
                yields( record( asList(javaExpression) ) ) );
    }

    // Nesting inside Map

    @Test
    public void shouldHandleTypeAsOutputNestedInMap() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assumeTrue( "Nashorn cannot handle big negative longs, treats them as doubles",
                !(javaExpression instanceof Long && javaExpression.equals( Long.MIN_VALUE )) );
        assertThat( exec( procedureSignature( "f" ).out( "out", NTMap ).build(), String.format( "yield [ {'k':%s} ]", javascriptExpression ) ),
                yields( record( map( "k", javaExpression ) ) ) );
    }

    @Test
    public void shouldHandleTypeAsInputNestedInMap() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).in( "arg", NTMap ).out( "out", NTMap ).build(), "yield [arg]", map( "k", javaExpression ) ),
                yields( record( map( "k", javaExpression ) ) ));
    }

    @Test
    public void shouldHandleTypeAsInputNestedInMapWithAnyAsSignature() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).in( "arg", NTAny ).out( "out", NTAny ).build(), "yield [arg]", map( "k", javaExpression ) ),
                yields( record( map( "k", javaExpression ) ) ));
    }

    @Test
    public void shouldHandleTypeAsOutputNestedInMapWithAnyAsSignature() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assumeTrue( "Nashorn cannot handle big negative longs, treats them as doubles",
                !(javaExpression instanceof Long && javaExpression.equals( Long.MIN_VALUE )) );
        assertThat( exec( procedureSignature( "f" ).out( "out", NTAny ).build(), String.format( "yield [ {'k':%s} ]", javascriptExpression ) ),
                yields( record( map( "k", javaExpression ) ) ) );
    }
}
