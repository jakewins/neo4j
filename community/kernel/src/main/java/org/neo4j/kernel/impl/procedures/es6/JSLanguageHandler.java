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

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.ScriptFunction;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;

public class JSLanguageHandler implements LanguageHandler
{
    private final ScriptEngine engine;
    private final JSStdLib stdLib;
    private final JSProcedureBoilerplate procedureBoilerplate = new JSProcedureBoilerplate();

    /**
     * Used via reflection.
     * @see JSSoftDependency
     */
    public JSLanguageHandler() throws NoSuchMethodException, IllegalAccessException, ProcedureException
    {
        this(new JSStdLib() );
    }

    /**
     * @param stdLib services made available to procedures
     * @throws ProcedureException
     */
    public JSLanguageHandler( JSStdLib stdLib ) throws ProcedureException
    {
        this.stdLib = stdLib;
        this.engine = new ScriptEngineManager().getEngineByName( "nashorn" );
    }

    @Override
    public synchronized Procedure compile( Statement statement, final ProcedureSignature signature, String code ) throws ProcedureException
    {
        try
        {
            // Init procedure-local context
            ScriptContext ctx = new SimpleScriptContext();
            stdLib.visit( ctx.getBindings( ScriptContext.ENGINE_SCOPE ) );

            // Wrap user code in boilerplate signature
            code = procedureBoilerplate.wrapAsProcedureFunction( signature, code );
            System.out.println(code);

            // Compile the ES5 generator function
            ScriptFunction procedureFunc = (ScriptFunction) NashornUtil.unwrap( (ScriptObjectMirror) engine.eval( code, ctx ) );

            return new JSProcedure( ctx, procedureFunc, new JSTyperMapper(signature), signature );
        }
        catch ( Throwable e )
        {
            throw new ProcedureException( Status.Schema.ProcedureSyntaxError, e, "Failed to compile javascript: '%s'", code );
        }
    }

    @Override
    public LanguageHandler register( String nameAndNamespace, Object service )
    {
        stdLib.register( nameAndNamespace, service );
        return this;
    }

    @Override
    public LanguageHandler unregister( String nameAndNamespace )
    {
        stdLib.unregister( nameAndNamespace );
        return null;
    }
}
