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

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.function.Function;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.LanguageHandlers;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureDescriptor;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.api.KernelStatement;
import org.neo4j.kernel.impl.api.operations.ProcedureExecutionOperations;
import org.neo4j.kernel.impl.api.operations.SchemaReadOperations;
import org.neo4j.kernel.impl.api.operations.SchemaStateOperations;

/**
 * TODO
 */
public class ProcedureExecutor
        implements LanguageHandlers, ProcedureExecutionOperations
{
    private Map<String,LanguageHandler> languageHandlers = new ConcurrentHashMap<>();

    private SchemaReadOperations schemaReadOperations;
    private SchemaStateOperations schemaStateOperations;

    public ProcedureExecutor( SchemaReadOperations schemaReadOperations, SchemaStateOperations schemaStateOperations )
    {
        this.schemaReadOperations = schemaReadOperations;
        this.schemaStateOperations = schemaStateOperations;
    }

    @Override
    public void addLanguageHandler( String language, LanguageHandler handler )
    {
        if ( languageHandlers.containsKey( language ) )
        { throw new IllegalArgumentException( String.format( "Language %s already registered", language ) ); }

        languageHandlers.put( language, handler );
    }

    @Override
    public void removeLanguageHandler( String language )
    {
        languageHandlers.remove( language );
    }

    @Override
    public void verify( ProcedureSignature signature, String language, String code ) throws ProcedureException
    {
        LanguageHandler languageHandler = languageHandlers.get( language );

        Procedure procedure = languageHandler.compile( signature, code );
    }

    @Override
    public RecordCursor call( KernelStatement statement, ProcedureSignature signature, Object[] args )
            throws ProcedureException
    {
        Map<ProcedureSignature,Procedure> procedures = schemaStateOperations.schemaStateGetOrCreate(
                statement, "procedures",
                new Function<String,Map<ProcedureSignature,Procedure>>()
                {
                    @Override
                    public Map<ProcedureSignature,Procedure> apply( String s )
                    {
                        return new ConcurrentHashMap<>();
                    }
                } );

        Procedure procedure = procedures.get( signature );
        if ( procedure == null )
        {
            ProcedureDescriptor procedureDescriptor = schemaReadOperations.procedureGetBySignature( statement,
                    signature );

            LanguageHandler languageHandler = languageHandlers.get( procedureDescriptor.language() );
            procedure = languageHandler.compile( signature, procedureDescriptor.procedureBody() );
            procedures.put( signature, procedure );
        }

        return procedure.call( statement, args );
    }
}
