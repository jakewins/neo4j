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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;
import static org.neo4j.kernel.impl.procedures.es6.JSSoftDependency.loadES6;

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
            proc.call( mock( Statement.class ), new Object[0], new Visitor<Object[],ProcedureException>()
            {
                @Override
                public boolean visit( Object[] element ) throws ProcedureException
                {
                    return true;
                }
            });
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