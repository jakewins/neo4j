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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.function.Function;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.LanguageHandlers;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureDescriptor;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.api.KernelStatement;
import org.neo4j.kernel.impl.api.operations.ProcedureExecutionOperations;

/**
 * In charge of invoking procedures. Delegates to {@link LanguageHandler} to compile procedures and to schema components to load and cache procedures.
 */
public class ProcedureExecutor
        implements LanguageHandlers, ProcedureExecutionOperations
{
    public static final Function<String,Map<ProcedureSignature,Procedure>>
            NEW_CONCURRENT_HASHMAP = new Function<String,Map<ProcedureSignature,Procedure>>()
    {
        @Override
        public Map<ProcedureSignature,Procedure> apply( String s )
        {
            return new ConcurrentHashMap<>();
        }
    };

    private Map<String,LanguageHandler> languageHandlers = new ConcurrentHashMap<>();

    @Override
    public void addLanguageHandler( String language, LanguageHandler handler )
    {
        if ( languageHandlers.containsKey( language ) )
        {
            throw new IllegalArgumentException( String.format( "Language %s already registered", language ) );
        }

        languageHandlers.put( language, handler );
    }

    @Override
    public void verify( KernelStatement statement, ProcedureSignature signature, String language, String code ) throws ProcedureException
    {
        LanguageHandler handler = languageHandlers.get( language );
        if( handler == null )
        {
            throw new ProcedureException( Status.Schema.ProcedureSyntaxError, "'%s' cannot be created, because `%s` is not a supported language. " +
                                                                              "Supported languages are: %s. You can add custom languages by installing " +
                                                                              "community built extensions to the database.", signature, language, languageHandlers.keySet() );
        }
        handler.compile( statement, signature, code );
    }

    @Override
    public RecordCursor call( KernelStatement statement, ProcedureSignature signature, Object[] args )
            throws ProcedureException
    {
        Map<ProcedureSignature,Procedure> procedures = statement.readOperations().schemaStateGetOrCreate( "procedures", NEW_CONCURRENT_HASHMAP );

        Procedure procedure = procedures.get( signature );
        if ( procedure == null )
        {
            synchronized(this)
            {
                procedure = procedures.get( signature );
                if( procedure == null )
                {
                    ProcedureDescriptor proc = statement.readOperations().procedureGet( signature );

                    LanguageHandler languageHandler = languageHandlers.get( proc.language() );
                    procedure = languageHandler.compile( statement, proc.signature(), proc.procedureBody() );
                    procedures.put( signature, procedure );
                }
            }
        }

        return procedure.call( statement, args );
    }
}
