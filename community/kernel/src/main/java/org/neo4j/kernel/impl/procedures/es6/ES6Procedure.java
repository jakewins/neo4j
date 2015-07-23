package org.neo4j.kernel.impl.procedures.es6;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.store.Neo4jTypes;

public class ES6Procedure implements Procedure
{
    private final CompiledScript compiled;
    private final ScriptEngine engine;
    private final ProcedureSignature signature;
    private final Bindings globals;

    public ES6Procedure( CompiledScript compiled, ScriptEngine engine, ProcedureSignature signature )
    {
        this.compiled = compiled;
        this.engine = engine;
        this.signature = signature;
        this.globals = engine.getBindings( ScriptContext.ENGINE_SCOPE );
    }

    @Override
    public RecordCursor call( Statement statement, Object[] args ) throws ProcedureException
    {
        try
        {
            ScriptObjectMirror rs = (ScriptObjectMirror) compiled.eval( createCallContext( args ) );

            for ( Map.Entry<String,Object> stringObjectEntry : rs.entrySet() )
            {
                System.out.println(stringObjectEntry.getKey());
                System.out.println(stringObjectEntry.getValue().getClass());
            }

            System.out.println(rs);
            System.out.println(rs.getClass());
        }
        catch ( ScriptException e )
        {
            throw new ProcedureException( Status.Request.ProcedureCallError, e, "Failed to invoke `%s`: %s", signature, e.getMessage() );
        }

        return new ES6RecordCursor();
    }

    private ScriptContext createCallContext( Object[] args )
    {
        Bindings locals = engine.createBindings();

        List<Pair<String,Neo4jTypes.AnyType>> inputSig = signature.inputSignature();
        for ( int i = 0; i < args.length; i++ )
        {
            locals.put( inputSig.get( i ).first(), args[i] );
        }

        SimpleScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings( globals, ScriptContext.GLOBAL_SCOPE );
        ctx.setBindings( locals, ScriptContext.ENGINE_SCOPE );
        return ctx;
    }

    private class ES6RecordCursor implements RecordCursor
    {
        @Override
        public Object[] getRecord()
        {
            return new Object[0];
        }

        @Override
        public boolean next()
        {
            return false;
        }

        @Override
        public void close()
        {

        }
    }
}
