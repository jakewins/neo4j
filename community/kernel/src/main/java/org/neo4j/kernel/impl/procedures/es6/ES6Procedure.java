package org.neo4j.kernel.impl.procedures.es6;

import java.util.List;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

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

    public ES6Procedure( CompiledScript compiled, ScriptEngine engine, ProcedureSignature signature )
    {
        this.compiled = compiled;
        this.engine = engine;
        this.signature = signature;
    }

    @Override
    public RecordCursor call( Statement statement, Object[] args ) throws ProcedureException
    {
        try
        {
            Object rs = compiled.eval( createCallContext( args ) );
            System.out.println(rs);
            System.out.println(rs.getClass());
        }
        catch ( ScriptException e )
        {
            throw new ProcedureException( Status.Request.ProcedureCallError, e, "Failed to invoke `%s`: %s", signature, e.getMessage() );
        }

        return new ES6RecordCursor();
    }

    private Bindings createCallContext( Object[] args )
    {
        Bindings callBindings = engine.createBindings();

        List<Pair<String,Neo4jTypes.AnyType>> inputSig = signature.inputSignature();
        for ( int i = 0; i < args.length; i++ )
        {
            callBindings.put( inputSig.get( i ).first(), args[i] );
        }
        return callBindings;
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
