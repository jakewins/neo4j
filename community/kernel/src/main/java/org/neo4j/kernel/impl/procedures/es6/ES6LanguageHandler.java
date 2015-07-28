package org.neo4j.kernel.impl.procedures.es6;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.ScriptFunction;

import java.io.InputStream;
import java.io.InputStreamReader;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;

public class ES6LanguageHandler implements LanguageHandler
{
    private final ScriptEngine engine;
    private final ES6Transpiler transpiler;
    private final ES6StdLib stdLib;

    public ES6LanguageHandler( ) throws NoSuchMethodException, IllegalAccessException, ProcedureException
    {
        this(new ES6StdLib());
    }

    public ES6LanguageHandler( ES6StdLib stdLib ) throws ProcedureException
    {
        this.stdLib = stdLib;
        try
        {
            ScriptEngineManager em = new ScriptEngineManager();
            this.engine = em.getEngineByName( "nashorn" );
            this.transpiler = ES6Transpiler.globalInstance();
        }
        catch( ScriptException e )
        {
            throw new ProcedureException( Status.Schema.ProcedureInitializationError, e, "Unable to initialize the ES6 procedure compiler: %s.", e.getMessage() );
        }
    }

    @Override
    public synchronized Procedure compile( Statement statement, final ProcedureSignature signature, String code ) throws ProcedureException
    {
        try
        {
            // Translate ES6 to ES5
            String translate = transpiler.translate( signature, code );

            // Init procedure-local context
            ScriptContext ctx = new SimpleScriptContext();
            engine.eval( new InputStreamReader( runtimeAsStream() ), ctx );
            stdLib.visit( ctx.getBindings( ScriptContext.ENGINE_SCOPE ) );

            // Compile the ES5 generator function
            ScriptFunction createGenerator = (ScriptFunction) NashornUtil.unwrap( (ScriptObjectMirror) engine.eval( translate, ctx ) );

            return new ES6Procedure( ctx, createGenerator, new ES6TypeMapper(signature), signature );
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

    /**
     * The ES6->ES5 compiler we use requires a little runtime library, which is available as a stream through here.
     */
    private InputStream runtimeAsStream()
    {
        return getClass().getClassLoader().getResourceAsStream( "js/traceur-runtime.js" );
    }
}
