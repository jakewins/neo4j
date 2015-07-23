package org.neo4j.kernel.impl.procedures.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeIterator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.store.Neo4jTypes;

import static org.mozilla.javascript.Context.getCurrentContext;

/**
* TODO
*/
public class CursorIterator
        extends ScriptableObject
{
    private RecordCursor cursor;
    private ProcedureSignature signature;

    public CursorIterator()
    {
    }

    public CursorIterator( RecordCursor cursor, ProcedureSignature signature )
    {
        this.cursor = cursor;
        this.signature = signature;
    }

    @JSConstructor
    public static Object constructor( Context cx, Object[] args, Function ctorObj, boolean inNewExpr )
    {
        if ( !inNewExpr )
        {
            throw ScriptRuntime.constructError( "Error", "Call constructor with new keyword." );
        }
        if ( args.length != 2 )
        {
            throw ScriptRuntime.constructError( "Error", "Call constructor with two numbers." );
        }
        return new CursorIterator( (RecordCursor) args[0], (ProcedureSignature) args[1] );
    }

    /**
     * JavaScript method for accessing the next value in the range.  Throws
     * StopIteration when the range is exhausted.
     *
     * @return
     */
    @JSFunction
    public Scriptable next()
    {
        if ( !cursor.next() )
        {
            throw new JavaScriptException(
                    NativeIterator.getStopIterationObject( getParentScope() ), null, 0 );
        }

        final Scriptable result = getCurrentContext().newObject( this );
        for ( int i = 0; i < signature.outputSignature().size(); i++ )
        {
            Pair<String,Neo4jTypes.AnyType> arg = signature.outputSignature().get( i );
            result.put( arg.first(), result, Context.javaToJS( cursor.record()[i], this ) );
        }
        return result;
    }

    /**
     * Magic method to allow for...in syntax.
     *
     * @param b
     * @return
     */
    @JSFunction
    public Object __iterator__( boolean b )
    {
        return this;
    }

    @Override
    public String getClassName()
    {
        return "CursorIterator";
    }
}
