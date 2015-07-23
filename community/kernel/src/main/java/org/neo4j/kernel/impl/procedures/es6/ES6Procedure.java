package org.neo4j.kernel.impl.procedures.es6;

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

            ScriptObject generator = (ScriptObject) ScriptRuntime.apply( createGenerator, createGenerator );
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
            ScriptObject yielded = (ScriptObject) ScriptRuntime.apply( nextFunction, generator );

            try
            {
                if( (Boolean)yielded.get( "done" ))
                {
                    return false;
                }

                mapper.translateRecord( yielded.get( "value" ), record );
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
