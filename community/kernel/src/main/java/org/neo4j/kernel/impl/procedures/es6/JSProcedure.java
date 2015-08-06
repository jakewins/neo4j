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

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;

import javax.script.ScriptContext;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;

/**
 * A callable javascript procedure.
 */
public class JSProcedure implements Procedure
{
    /** Map types in and out of javascript, this is tied to {@link #signature} */
    private final JSTyperMapper mapper;

    /** The public procedure signature we abide by */
    private final ProcedureSignature signature;

    /** The context we execute in (eg. access to global javascript vars etc.) */
    private final ScriptContext ctx;

    /** The compiled user code - a javascript function that yields a generator */
    private final ScriptFunction procedureFunc;

    public JSProcedure( ScriptContext ctx, ScriptFunction procedureFunc, JSTyperMapper mapper, ProcedureSignature signature )
    {
        this.ctx = ctx;
        this.procedureFunc = procedureFunc;
        this.mapper = mapper;
        this.signature = signature;
    }

    @Override
    public void call( Statement statement, Object[] args, Visitor<Object[],ProcedureException> visitor ) throws ProcedureException
    {
        try
        {
            Context.setGlobal( NashornUtil.unwrap( (ScriptObjectMirror) ctx.getAttribute( NashornScriptEngine.NASHORN_GLOBAL ) ) );
            ScriptRuntime.apply( procedureFunc, procedureFunc, procedureArguments( args, visitor ) );
        }
        catch ( Throwable e )
        {
            throw new ProcedureException( Status.Request.ProcedureCallError, e, "Failed to invoke `%s`: %s", signature, e.getMessage() );
        }
    }

    private Object[] procedureArguments( Object[] args, final Visitor<Object[],ProcedureException> visitor )
    {
        final Object[] argsPlusEmit = new Object[args.length + 1];
        final Object[] outputRecord = new Object[signature.outputSignature().size()];
        argsPlusEmit[0] = new EmitHandler() {
            @Override
            public void apply( Object record ) throws ProcedureException
            {
                if( record instanceof ScriptObject )
                {
                    mapper.translateRecord( (ScriptObject) record, outputRecord );
                    visitor.visit( outputRecord );
                }
                else
                {
                    throw new IllegalArgumentException( "Unable to emit " + record + ", unknown type `" + record.getClass() + "`." );
                }
            }
        };
        for ( int i = 1; i < argsPlusEmit.length; i++ )
        {
            argsPlusEmit[i] = args[i-1];
        }
        return argsPlusEmit;
    }

    public interface EmitHandler
    {
        void apply( Object record ) throws ProcedureException;
    }
}
