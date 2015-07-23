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
package org.neo4j.kernel.impl.procedures.javascript;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.lang.reflect.Method;
import java.util.Iterator;

import org.neo4j.helpers.Pair;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;

import static org.mozilla.javascript.Context.exit;
import static org.neo4j.kernel.impl.store.Neo4jTypes.AnyType;

/**
 * TODO
 */
public class JavaScriptLanguageHandler
        implements LanguageHandler
{
    public static final String LANG_JS = "javascript";

    private final Scriptable parentScope;

    public JavaScriptLanguageHandler( Visitor<Scriptable,RuntimeException> stdLibraryProvider )
    {
        Context context = ctx();
        parentScope = context.initStandardObjects();

        try
        {
            Method recordMethod = getClass().getMethod( "record", Object.class );
            parentScope.put( "record", parentScope,
                    new FunctionObject( "record", recordMethod, parentScope )
                    {
                        @Override
                        public Object call( Context cx, Scriptable scope, Scriptable thisObj, Object[] args )
                        {
                            return record( args );
                        }
                    } );

            ScriptableObject.defineClass( parentScope, CursorIterator.class );
            ScriptableObject.defineClass( parentScope, NativeJavaIterator.class );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }


        stdLibraryProvider.visit( parentScope );
    }

    public Object[] record( Object arg )
    {
        Object[] args = (Object[]) arg;
        for ( int i = 0; i < args.length; i++ )
        {
            args[i] = Context.jsToJava( args[i], Object.class );
        }
        return args;
    }

    private Context ctx()
    {
        // Rhino uses thread-local contexts for javascript for some inexplicable reason. There is very little
        // documentation about them or why the hell they would tie them to threads. In any case, this is a
        // hack to ensure the current thread has a context associated with it.
        Context ctx = Context.getCurrentContext();
        if ( ctx == null )
        {
            ctx = Context.enter();
            ctx.setLanguageVersion( Context.VERSION_1_8 );
            ctx.setOptimizationLevel( 0 );
            return ctx;
        }
        else
        {
            return ctx;
        }
    }

    @Override
    public Procedure compile( Statement statement, ProcedureSignature signature, String code ) throws
            ProcedureException
    {
        StringBuilder header = new StringBuilder();
        header.append( "function procedure(" );
        for ( int i = 0; i < signature.inputSignature().size(); i++ )
        {
            Pair<String,AnyType> arg = signature.inputSignature().get( i );
            if ( i > 0 )
            { header.append( ',' ); }
            header.append( arg.first() );
        }
        header.append( "){" );

        code = header.toString() + code + "\n}";

        // Import all functions into scope
        Context ctx = ctx();
        Scriptable proceduresScope;
        Function script;
        try
        {
            proceduresScope = ctx.newObject( parentScope );
            proceduresScope.setPrototype( parentScope );
            proceduresScope.setParentScope( null );

            Iterator<ProcedureSignature> procedures = statement.readOperations().proceduresGetAll();
            while ( procedures.hasNext() )
            {
                ProcedureSignature proc = procedures.next();

                StringBuilder name = new StringBuilder();
                for ( String nameSpace : proc.namespace() )
                {
                    name.append( nameSpace ).append( '_' );
                }
                name.append( proc.name() );

                proceduresScope.put( name.toString(), proceduresScope, new ProcedureFunction( proc ) );
            }

            script = ctx().compileFunction( proceduresScope, code, signature.name(), 0, null );
        }
        catch ( EvaluatorException e )
        {
            throw new ProcedureException( Status.Schema.ProcedureSyntaxError, e, e.getMessage() );
        }
        finally
        {
            exit();
        }

        return new JavaScriptProcedure( script, proceduresScope );
    }

    private class ProcedureFunction
            extends BaseFunction
    {
        private ProcedureSignature signature;

        public ProcedureFunction( ProcedureSignature signature )
        {
            this.signature = signature;
        }

        @Override
        public Object call( final Context cx, final Scriptable scope, Scriptable thisObj, Object[] args )
        {
            try
            {
                Statement statement = (Statement) cx.getThreadLocal( "__statement" );

                final RecordCursor cursor = statement.readOperations().procedureCall( signature, args );

                return cx.newObject( scope, "CursorIterator", new Object[]{cursor, signature} );
            }
            catch ( ProcedureException e )
            {
                throw new RuntimeException( e );
            }
        }

        @Override
        public int getArity()
        {
            return signature.inputSignature().size();
        }
    }

    private class JavaScriptProcedure implements Procedure
    {
        private final Function function;
        private Scriptable proceduresScope;

        public JavaScriptProcedure( Function function, Scriptable proceduresScope )
        {
            this.function = function;
            this.proceduresScope = proceduresScope;
        }

        @Override
        public RecordCursor call( Statement statement, Object[] args )
        {
            Context ctx = ctx();

            // Create a sub-scope so that side-effects of the function do not pollute the global scope
            Scriptable scope = ctx.newObject( proceduresScope );
            scope.setPrototype( proceduresScope );
            scope.setParentScope( null );

            ctx.putThreadLocal( "__statement", statement );

            NativeGenerator generator = (NativeGenerator) function.call( ctx, scope, scope, args );

            return new JavaScriptRecordCursor( generator, ctx, scope );
        }
    }

    private class JavaScriptRecordCursor implements RecordCursor
    {
        private final IdFunctionObject FUNC_CLOSE = new IdFunctionObject( null, "Generator", 1, 0 );
        private final IdFunctionObject FUNC_NEXT = new IdFunctionObject( null, "Generator", 2, 0 );

        private final NativeGenerator generator;
        private final Context ctx;
        private final Scriptable scope;
        private Object[] nextRecord;

        public JavaScriptRecordCursor( NativeGenerator generator, Context ctx, Scriptable scope )
        {
            this.generator = generator;
            this.ctx = ctx;
            this.scope = scope;
        }

        @Override
        public Object[] record()
        {
            return nextRecord;
        }

        @Override
        public boolean next()
        {
            try
            {
                nextRecord = (Object[]) generator.execIdCall( FUNC_NEXT, ctx, scope, generator, new Object[]{} );

                return true;
            }
            catch ( JavaScriptException e )
            {
                if ( e.details().equals( "[object StopIteration]" ) )
                {
                    return false;
                }

                throw new RuntimeException( "Failed to evaluate javascript user function: " + e.getMessage(), e );
            }
        }

        @Override
        public void close()
        {
            generator.execIdCall( FUNC_CLOSE, ctx, scope, generator, new Object[]{} );
//            exit();
        }
    }
}
