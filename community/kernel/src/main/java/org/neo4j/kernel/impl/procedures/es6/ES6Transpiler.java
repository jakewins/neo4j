package org.neo4j.kernel.impl.procedures.es6;

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

import org.neo4j.helpers.Pair;
import org.neo4j.helpers.SimpleFuture;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.impl.store.Neo4jTypes;

/**
 * Cross-compiles ES6 javascript to ES5, which can then be handled by Nashorn.
 */
public class ES6Transpiler
{
    private final SimpleFuture<ScriptEngine> engineFuture = new SimpleFuture<>();

    private static final String GENERATOR_TEMPLATE = "function *%s { %s }";
    private static final String FUNCTION_TEMPLATE = "function %s { %s }";

    private static ES6Transpiler globalInstance;

    public static ES6Transpiler globalInstance() throws ScriptException
    {
        return globalInstance( new Executor()
        {
            @Override
            public void execute( Runnable command )
            {
                command.run();
            }
        });
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
    public ES6Transpiler(Executor backgroundLoader)
    {
        // Load the traceur compiler in the background, otherwise we delay system startup with several seconds.
        // This is preferred over lazilly initializing the runtime as well, since then we'd have the user take the initial load hit, and we'd have the
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
                    engine.eval( new InputStreamReader( getClass().getClassLoader().getResourceAsStream( "js/traceur-shim.js" ) ) );
                    engine.eval( new InputStreamReader( getClass().getClassLoader().getResourceAsStream( "js/traceur.js" ) ) );

                    engineFuture.fulfill( engine );
                }
                catch ( Throwable e )
                {
                    engineFuture.fail( e );
                }
            }
        } );
    }

    public synchronized String translate( ProcedureSignature signature, String es6Script ) throws ScriptException, ProcedureException
    {
        String jsSignature = jsSignature( signature );
        try
        {
            return compile( String.format( GENERATOR_TEMPLATE, jsSignature, es6Script), signature );
        }
        catch( ScriptException e )
        {
            return compile( String.format( FUNCTION_TEMPLATE, jsSignature, es6Script ), signature );
        }
    }

    private String jsSignature( ProcedureSignature signature )
    {
        StringBuilder out = new StringBuilder( );

        // Namespace
        StringBuilder ns = new StringBuilder();
        for ( String s : signature.namespace() )
        {
            ns.append( s );
            out.append( String.format("var %1$s=%1$s || {};", ns.toString() ) );
            ns.append( "." );
        }
        out.append( ns.toString() );

        // Method name
        out.append( signature.name() ).append( "( " );

        // Argument signature
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

    private String compile( String es6Script, ProcedureSignature signature ) throws ScriptException, ProcedureException
    {
        return (String) engine().eval( "traceur.Compiler.script(script);", newCompilerScope( es6Script, signature ) );
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
