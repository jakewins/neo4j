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