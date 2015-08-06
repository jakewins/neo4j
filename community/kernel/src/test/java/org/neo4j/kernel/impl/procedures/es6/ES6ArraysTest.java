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

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.neo4j.kernel.api.procedure.ProcedureException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assume.assumeTrue;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.procedures.es6.JSSoftDependency.es6LanguageHandlerAvailable;
import static org.neo4j.kernel.impl.procedures.es6.ProcedureMatchers.exec;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTAny;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTInteger;
import static org.neo4j.kernel.impl.store.Neo4jTypes.NTList;

public class ES6ArraysTest
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldSupportJSCreatedPrimitiveArrays() throws Throwable
    {
        // TODO: Below indicates how Nashorn does not concern itself with primitive arrays. This is going to be an API issue.
        // IMO this is a heavy weight in the bucket for a specialized API, rather than the raw embedded API. We could also provide utility methods for
        // conversion, of course. However, a thin JS-oriented veneer over the regular java API would be brilliant. Then we could also hide all the functionality
        // we do not want to expose directly to procedures (gdb.shutdown(), for instance).
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).out( "out", NTAny ).build(), "var primitive = new (Java.type('int[]'))(1);" +
                                                                                 "primitive[0] = 1;" +
                                                                                 "yield [primitive]" ),
                contains( /* record that */ contains( equalTo( (Object)new int[]{1} ) )));
    }

    @Test
    public void shouldPassThroughPrimitiveArrays() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );
        assertThat( exec( procedureSignature( "f" ).in( "arg", NTAny ).out( "out", NTAny ).build(), "yield [arg]", new int[0] ),
                contains( /* record that */ contains( instanceOf( int[].class ) )));
    }

    @Test
    public void shouldThrowTypeErrorIfYieldingArrayOfWrongType() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );

        // Expect
        // TODO we assert on cause here because Cursor does not allow throwing KernelException. Need to move KernelException to some module where Cursor can expose it
        exception.expectCause( Matchers.<Throwable>instanceOf( ProcedureException.class ) );

        // When
        exec( procedureSignature( "f" ).in( "arg", NTAny ).out( "out", NTList( NTInteger ) ).build(), "yield [arg]", new boolean[0] );
    }

    @Test
    public void shouldThrowTypeErrorIfYieldingCollectionOfWrongType() throws Throwable
    {
        assumeTrue( es6LanguageHandlerAvailable() );

        // Expect
        exception.expectCause( Matchers.<Throwable>instanceOf( ProcedureException.class ) );

        // When
        exec( procedureSignature( "f" ).out( "out", NTList( NTInteger ) ).build(), "yield [[true,false]]" );
    }
}
