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

import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;

import javax.script.ScriptContext;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;

public class ES6Procedure implements Procedure
{
    private final ES6TypeMapper mapper;
    private final ProcedureSignature signature;
    private final ScriptContext ctx;
    private final ScriptFunction createGenerator;

    public ES6Procedure( ScriptContext ctx, ScriptFunction createGenerator, ES6TypeMapper mapper, ProcedureSignature signature )
    {
        this.ctx = ctx;
        this.createGenerator = createGenerator;
        this.mapper = mapper;
        this.signature = signature;
    }

    @Override
    public RecordCursor call( Statement statement, Object[] args ) throws ProcedureException
    {
        try
        {
            Context.setGlobal( NashornUtil.unwrap( (ScriptObjectMirror) ctx.getAttribute( NashornScriptEngine.NASHORN_GLOBAL ) ) );

            ScriptObject generator = (ScriptObject) ScriptRuntime.apply( createGenerator, createGenerator, args );
            ScriptFunction nextFunction = (ScriptFunction) generator.get( "next" );

            return new ES6RecordCursor( generator, nextFunction, mapper, signature );
        }
        catch ( Throwable e )
        {
            throw new ProcedureException( Status.Request.ProcedureCallError, e, "Failed to invoke `%s`: %s", signature, e.getMessage() );
        }
    }

    private class ES6RecordCursor implements RecordCursor
    {
        private final ScriptObject generator;
        private final ScriptFunction nextFunction;
        private final ES6TypeMapper mapper;

        private final Object[] record;

        public ES6RecordCursor( ScriptObject generator, ScriptFunction nextFunction, ES6TypeMapper mapper, ProcedureSignature signature )
        {
            this.generator = generator;
            this.nextFunction = nextFunction;
            this.mapper = mapper;
            this.record = new Object[signature.outputSignature().size()];
        }

        @Override
        public Object[] record()
        {
            return record;
        }

        @Override
        public boolean next()
        {
            try
            {
                ScriptObject yielded = (ScriptObject) ScriptRuntime.apply( nextFunction, generator );

                if( (Boolean)yielded.get( "done" ))
                {
                    return false;
                }

                mapper.translateRecord( yielded.get( "value" ), record );
            }
            catch( RuntimeException e )
            {
                // TODO
                if( e.getCause() instanceof WrappedECMAException )
                {
                    Object mystery = ((WrappedECMAException) e.getCause()).unwrap();
                    System.out.println("Mystery: " + mystery.getClass());
                    if( mystery instanceof NashornException )
                    {
                        NashornException original1 = (NashornException) mystery;
                        System.out.println("UNWRAPPED: " + original1.getMessage());
                        original1.printStackTrace();
                    }
                }
                throw e;
            }
            catch ( ProcedureException e )
            {
                // TODO: Modify cursors to allow throwing exceptions
                throw new RuntimeException( e );
            }


            return true;
        }

        @Override
        public void close()
        {

        }
    }
}
