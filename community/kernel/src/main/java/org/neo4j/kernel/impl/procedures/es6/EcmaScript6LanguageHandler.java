package org.neo4j.kernel.impl.procedures.es6;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.ScriptObject;

import java.io.InputStreamReader;
import java.util.Map;
import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;

public class EcmaScript6LanguageHandler implements LanguageHandler
{
    private final ScriptEngine engine;
    private final Compilable compiler;
    private final ES6Transpiler transpiler;

    public EcmaScript6LanguageHandler() throws ProcedureException
    {
        try
        {
            ScriptEngineManager em = new ScriptEngineManager();
            this.engine = em.getEngineByName( "nashorn" );
            this.compiler = (Compilable) engine;
            this.transpiler = new ES6Transpiler();

            // Init global context
            this.engine.eval( new InputStreamReader(getClass().getClassLoader().getResourceAsStream( "js/traceur-runtime.js" )) );

        } catch( ScriptException e )
        {
            throw new ProcedureException( Status.Schema.ProcedureInitializationError, e, "Unable to initialize the ES6 procedure compiler: %s.", e.getMessage() );
        }
    }

    @Override
    public Procedure compile( Statement statement, ProcedureSignature signature, String code ) throws ProcedureException
    {
        try
        {
            // Evaluate the script, adding the function to the global scope
//            SimpleScriptContext ctx = new SimpleScriptContext();
//            ctx.setBindings( engine.getBindings( ScriptContext.ENGINE_SCOPE ), ScriptContext.GLOBAL_SCOPE );
            String translate = transpiler.translate( signature, code );
            System.out.println( translate );
            final Invocable invocable = (Invocable) engine;

            NashornScriptEngine ne = (NashornScriptEngine) engine;
            ScriptObjectMirror eval = (ScriptObjectMirror) engine.eval( translate );

            ne.getInterface(  )

            ScriptObject ctx = Global.newEmptyInstance();
            ScriptObjectMirror call = (ScriptObjectMirror) eval.call( ctx );
            System.out.println(call);
            System.out.println(call.getClass());
            System.out.println(call.isFunction());
            System.out.println(call.hasMember( "next" ));

            System.out.println("NEXT");
            ScriptObjectMirror next = (ScriptObjectMirror) call.getMember( "next" );
            Object out = next.call( ctx );

            System.out.println(out);
            System.out.println(out.getClass());

            for ( Map.Entry<String,Object> stringObjectEntry : call.entrySet() )
            {
                System.out.println( stringObjectEntry.getKey() + "=>" + stringObjectEntry.getValue().getClass() );
            }


            // Compile a tiny shim that simply invokes the script
            return null;//new ES6Procedure( compiler, engine, signature );
        }
        catch ( Exception e )
        {
            throw new ProcedureException( Status.Schema.ProcedureCompilationError, e, "Failed to compile javascript: '%s'", code );
        }
    }
}
