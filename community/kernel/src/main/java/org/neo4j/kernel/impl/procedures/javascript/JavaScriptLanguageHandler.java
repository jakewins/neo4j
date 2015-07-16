package org.neo4j.kernel.impl.procedures.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeGenerator;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.neo4j.helpers.Pair;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;

/**
 * TODO
 */
public class JavaScriptLanguageHandler
    implements LanguageHandler
{
    public static final String LANG_JS = "javascript";

    private final Scriptable parentScope;

    public JavaScriptLanguageHandler()
    {
        this( new Visitor<Scriptable,RuntimeException>()
        {
            @Override
            public boolean visit( Scriptable element )
            {
                return false;
            }
        } );
    }

    public JavaScriptLanguageHandler(Visitor<Scriptable, RuntimeException> stdLibraryProvider)
    {
        Context context = ctx();
        parentScope = context.initStandardObjects();

        try
        {
            Method recordMethod = getClass().getMethod( "record", Object.class );
            parentScope.put( "record", parentScope,
                    new FunctionObject("record", recordMethod, parentScope){
                        @Override
                        public Object call( Context cx, Scriptable scope, Scriptable thisObj, Object[] args )
                        {
                            return record( args );
                        }
                    } );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException(e);
        }

        stdLibraryProvider.visit( parentScope );
    }

    public Object record( Object args )
    {
        return args;
    }

    private Context ctx()
    {
        // Rhino uses thread-local contexts for javascript for some inexplicable reason. There is very little
        // documentation about them or why the hell they would tie them to threads. In any case, this is a
        // hack to ensure the current thread has a context associated with it.
        Context ctx = Context.getCurrentContext();
        if(ctx == null)
        {
            ctx = Context.enter();
            ctx.setLanguageVersion( Context.VERSION_1_8 );
            ctx.setOptimizationLevel( 9 );
            return ctx;
        }
        else
        {
            return ctx;
        }
    }

    @Override
    public Procedure compile( ProcedureSignature signature, InputStream codeStream ) throws ProcedureException
    {
        try
        {
            String code = readAsString( codeStream );

            StringBuilder header = new StringBuilder(  );
            header.append( "function procedure(" );
            for ( int i = 0; i < signature.getInputSignature().size(); i++ )
            {
                Pair<String,ProcedureSignature.Neo4jType> arg =
                        signature.getInputSignature().get( i );
                if (i > 0)
                    header.append( ',' );
                header.append( arg.first() );
            }
            header.append( ")\n{\n" );

            code = header.toString()+code+"\n}";

            Function script = ctx().compileFunction( parentScope, code, "myFile.js", 0,
                    null );

            return new JavaScriptProcedure( signature, script );
        }
        catch ( RuntimeException e )
        {
            throw new ProcedureException( Status.Schema.ProcedureCompilationError, e, "Could not compile the " +
                                                                                      "JavaScript" );
        }
    }

    private static String readAsString( InputStream input )
    {
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader reader = null;
        try
        {
            reader = new InputStreamReader( input, StandardCharsets.UTF_8 );
            int read;
            do
            {
                read = reader.read( buffer, 0, buffer.length );
                if ( read > 0 )
                {
                    out.append( buffer, 0, read );
                }
            }
            while ( read >= 0 );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            if ( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch ( IOException e )
                {
                    // OK
                }
            }
        }
        return out.toString();
    }


    private class JavaScriptProcedure implements Procedure
    {
        private ProcedureSignature signature;
        private final Function function;

        public JavaScriptProcedure( ProcedureSignature signature, Function function )
        {
            this.signature = signature;
            this.function = function;
        }

        @Override
        public RecordCursor call( Statement statement, Object[] args )
        {
            Context ctx = ctx();

            // Create a sub-scope so that side-effects of the function do not pollute the global scope
            Scriptable scope = ctx.newObject(parentScope);
            scope.setPrototype( parentScope );
            scope.setParentScope( null );

            NativeGenerator generator = (NativeGenerator) function.call( ctx, scope, null, args );

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
        public Object[] getRecord()
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
            catch( JavaScriptException e )
            {
                if(e.details().equals( "[object StopIteration]" ))
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
        }
    }
}
