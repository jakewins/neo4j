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

import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.neo4j.concurrent.CompletableFuture;
import org.neo4j.helpers.Exceptions;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.impl.store.Neo4jTypes;

/**
 * Cross-compiles ES6 javascript to ES5, which can then be handled by Nashorn.
 */
public class ES6Transpiler
{
    private final CompletableFuture<ScriptEngine> engineFuture = new CompletableFuture<>();

    private static final String PROCEDURE_BOILERPLATE = "%s = function %s { " +
                                                     // We include a procedure-specific "record" function. This is a placeholder so that we in the future
                                                     // can inject a java function with the exact type signature for records of this procedure. By doing that,
                                                     // we get extremely optimized bytecode that pushes primitives directly out of javascript for us, and
                                                     // we get an easier time with type checking. We also are able to re-use record objects, cutting down on
                                                     // one of the big sources of allocation in procedures.
                                                     // We have this here for now so that the API is in place, but for now just returning an array.
                                                     "  function record() { return arguments; }; " +
//                                                     "  try {" +
                                                     "    %s " +
//                                                     "  } catch( e ) { " +
//                                                     // Nashorn rewrites the stack trace of exceptions when they are rethrown, and the ES6 libraries
//                                                     // catch type errors and rethrow them, meaning they get rewritten to look like they came from the ES6
//                                                     // runtime rather than user code. This catch statement captures them and saves the line info so we can
//                                                     // give the user a sensible error message.
//                                                     "    throw new "+WrappedECMAException.class.getCanonicalName()+"( e );" +
//                                                     "  }" +
                                                     "}";
    private static ES6Transpiler globalInstance;

    /**
     * The result of cross-compilation. Contains the new source code, as well as mapping utilities to map line and column numbers back to the original source
     * on errors.
     */
    public static class TranspiledSource
    {
        public static class SourcePosition
        {
            private final int line;
            private final int column;

            public SourcePosition( int line, int column )
            {
                this.line = line;
                this.column = column;
            }

            public int line() { return line; }
            public int column() { return column; }
        }

        private String source;

        public String compiledSource()
        {
            return source;
        }

        public SourcePosition originalPositionFor( int line, int column )
        {
            return null;
        }
    }

    /**
     * Global singleton, as loading the compiler library is very, very slow, so we want to avoid doing this as long as possible.
     * @see #ES6Transpiler(Executor)
     */
    public static synchronized ES6Transpiler globalInstance( Executor backgroundLoader ) throws ScriptException
    {
        if(globalInstance == null)
        {
            globalInstance = new ES6Transpiler(backgroundLoader);
        }
        return globalInstance;
    }

    /**
     * @param backgroundLoader used once, to initialize the javascript transpiler in the background
     */
    public ES6Transpiler( Executor backgroundLoader )
    {
        // Load the traceur compiler in the background, otherwise we delay system startup with several seconds.
        // This is preferred over lazilly initializing the runtime, since then we'd have the user take the load hit, and we'd have the
        // same concurrency complexity with multiple requests wanting transpiling service at the same time.
        backgroundLoader.execute( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ScriptEngineManager em = new ScriptEngineManager();
                    ScriptEngine engine = em.getEngineByName( "nashorn" );
                    engine.eval( new InputStreamReader( getClass().getClassLoader().getResourceAsStream( "js/traceur.js" ) ) );
                    engine.eval( new InputStreamReader( getClass().getClassLoader().getResourceAsStream( "js/traceur-shim.js" ) ) );

                    engineFuture.complete( engine );
                }
                catch ( Throwable e )
                {
                    engineFuture.completeExceptionally( e );
                }
            }
        } );
    }

    public synchronized String translate( ProcedureSignature signature, String es6Script ) throws ScriptException, ProcedureException
    {
        String namespaceDef = namespaceDefinition( signature );
        String jsSignature = argSignature( signature );
        try
        {
            // First try compiling the user code as an ES6 generator
            String generatorSignature = "*" + jsSignature;
            return compile( String.format( PROCEDURE_BOILERPLATE, namespaceDef, generatorSignature, es6Script), signature );
        }
        catch( ProcedureException e )
        {
            try
            {
                // If that doesn't work, fall back to compiling as a regular JS function
                return compile( String.format( PROCEDURE_BOILERPLATE, namespaceDef, jsSignature, es6Script ), signature );
            }
            catch( ProcedureException inner )
            {
                throw Exceptions.withSuppressed(e, inner);
            }
        }
    }

    private String argSignature( ProcedureSignature signature )
    {
        StringBuilder out = new StringBuilder( );

        out.append( "( " );
        boolean first = true;
        for ( Pair<String,Neo4jTypes.AnyType> sig : signature.inputSignature() )
        {
            if(!first)
            {
                out.append( ", " );
            }
            first = false;
            out.append( sig.first() );
        }
        out.append( " )" );

        return out.toString();
    }

    private String namespaceDefinition( ProcedureSignature signature )
    {
        StringBuilder ns = new StringBuilder();
        StringBuilder out = new StringBuilder();
        for ( String s : signature.namespace() )
        {
            ns.append( s );
            out.append( String.format( "var %1$s=%1$s||{};", ns.toString() ) );
            ns.append( "." );
        }
        out.append( ns ).append( signature.name() );
        return out.toString();
    }

    private String compile( String es6Script, ProcedureSignature signature ) throws ScriptException, ProcedureException
    {
        ScriptObjectMirror output = (ScriptObjectMirror) engine().eval( "ES6ToES5(script, scriptName);", newCompilerScope( es6Script, signature ) );
        if( output.containsKey( "errors" ) )
        {
            throw procedureCompileError( signature, (ScriptObjectMirror) output.get( "errors" ) );
        }
        return (String) output.get( "result" );
    }

    private SimpleScriptContext newCompilerScope( String script, ProcedureSignature signature ) throws ProcedureException
    {
        SimpleScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings( engine().getBindings( ScriptContext.ENGINE_SCOPE ), ScriptContext.GLOBAL_SCOPE);
        Bindings localScope = ctx.getBindings( ScriptContext.ENGINE_SCOPE );

        localScope.put( "script", script );
        localScope.put( "scriptName", signature.name() );

        return ctx;
    }

    private ProcedureException procedureCompileError( ProcedureSignature signature, ScriptObjectMirror errors )
    {
        StringBuilder desc = new StringBuilder();
        for ( Object o : errors.values() )
        {
            desc.append( o ).append( "\n" );
        }
        return new ProcedureException( Status.Schema.ProcedureSyntaxError, "Failed to compile procedure `%s`:\n%s", signature, desc.toString() );
    }

    private ScriptEngine engine() throws ProcedureException
    {
        try
        {
            return engineFuture.get(280, TimeUnit.SECONDS);
        }
        catch ( RuntimeException | InterruptedException | ExecutionException | TimeoutException e )
        {
            throw new ProcedureException( Status.Schema.ProcedureInitializationError, e, "Failed to initialize ES6 compiler: %s", e.getMessage() );
        }
    }
}
