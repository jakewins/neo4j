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
package org.neo4j.kernel.impl.procedures;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.impl.api.KernelStatement;

import static org.mockito.Mockito.mock;
import static org.neo4j.kernel.api.procedure.ProcedureSignature.procedureSignature;

public class ProcedureExecutorTest
{
    @Rule public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldValidateProcedures() throws Throwable
    {
        // Given
        ProcedureExecutor procs = new ProcedureExecutor();

        procs.addLanguageHandler( "mylang", new LanguageHandler()
        {
            @Override
            public Procedure compile( Statement statement, ProcedureSignature signature, String code ) throws ProcedureException
            {
                throw new ProcedureException( Status.General.UnknownFailure, "Nope" );
            }

            @Override
            public LanguageHandler register( String nameAndNamespace, Object service )
            {
                return null;
            }

            @Override
            public LanguageHandler unregister( String nameAndNamespace )
            {
                return null;
            }
        } );

        // Expect
        exception.expect( ProcedureException.class );

        // When
        procs.verify( mock( KernelStatement.class ), procedureSignature( "p" ).build(), "mylang", ".." );
    }

    @Test
    public void shouldValidateLanguage() throws Throwable
    {
        // Given
        ProcedureExecutor procs = new ProcedureExecutor();

        // Expect
        exception.expect( ProcedureException.class );

        // When
        procs.verify( mock( KernelStatement.class ), procedureSignature( "p" ).build(), "mylang", ".." );
    }
}