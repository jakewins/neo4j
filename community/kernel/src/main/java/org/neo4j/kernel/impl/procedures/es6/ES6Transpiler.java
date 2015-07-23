package org.neo4j.kernel.impl.procedures.es6;

import java.io.InputStreamReader;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.impl.store.Neo4jTypes;

/**
 * Cross-compiles ES6 javascript to ES5, which can then be handled by Nashorn.
 */
public class ES6Transpiler
{
    private final ScriptEngine engine;
    private final Bindings globals;
    private static final String GENERATOR_TEMPLATE = "function *%s { %s }";
    private static final String FUNCTION_TEMPLATE = "function %s { %s }";

    public ES6Transpiler() throws ScriptException
    {
        ScriptEngineManager em = new ScriptEngineManager();
        this.engine = em.getEngineByName( "nashorn" );
        this.globals = engine.getBindings( ScriptContext.ENGINE_SCOPE );

        // Load the traceur compiler
        engine.eval( new InputStreamReader( getClass().getClassLoader().getResourceAsStream( "js/traceur.js" ) ) );
        engine.eval( new InputStreamReader( getClass().getClassLoader().getResourceAsStream( "js/traceur-shim.js" ) ) );
    }

    public String translate( ProcedureSignature signature, String es6Script ) throws ScriptException
    {
        String jsSignature = jsSignature( signature );
        try
        {
            String format = String.format( GENERATOR_TEMPLATE, jsSignature, es6Script );
            System.out.println(format);
            return compile( format );
        }
        catch( ScriptException e )
        {
            return compile( String.format( FUNCTION_TEMPLATE, jsSignature, es6Script ) );
        }
    }

    private String jsSignature( ProcedureSignature signature )
    {
        StringBuilder out = new StringBuilder( );
        StringBuilder ns = new StringBuilder();
        for ( String s : signature.namespace() )
        {
            ns.append( s );
            out.append( String.format("var %1$s=%1$s || {};", ns.toString() ) );
            ns.append( "." );
        }
//        out.append(ns.toString()).append( signature.name() ).append( "( " );

        out.append( "__procedure(" );
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
        out.append( ")" );

        return out.toString();
    }

    private String compile( String es6Script ) throws ScriptException
    {
        return (String) engine.eval( "ES6ToES5(script, scriptName);", newCompilerScope( es6Script ) );
    }

    private SimpleScriptContext newCompilerScope( String script )
    {
        SimpleScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings( globals, ScriptContext.GLOBAL_SCOPE);
        Bindings localScope = ctx.getBindings( ScriptContext.ENGINE_SCOPE );

        localScope.put( "script", script );
        localScope.put( "scriptName", "procedure" ); // TODO
        return ctx;
    }
}
