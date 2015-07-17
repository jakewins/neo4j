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

import java.util.Iterator;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.store.Neo4jTypes;

import static org.mozilla.javascript.Context.getCurrentContext;

/**
* TODO
*/
public class NativeJavaIterator
        extends ScriptableObject
{
    private Iterator iterator;

    public NativeJavaIterator()
    {
    }

    public NativeJavaIterator( Iterator iterator )
    {
        this.iterator = iterator;
    }

    @JSConstructor
    public static Object constructor( Context cx, Object[] args, Function ctorObj, boolean inNewExpr )
    {
        if ( !inNewExpr )
        {
            throw ScriptRuntime.constructError( "Error", "Call constructor with new keyword." );
        }
        if ( args.length != 1 )
        {
            throw ScriptRuntime.constructError( "Error", "Call constructor with an iterator." );
        }
        return new NativeJavaIterator( (Iterator) args[0] );
    }

    /**
     * JavaScript method for accessing the next value in the range.  Throws
     * StopIteration when the range is exhausted.
     *
     * @return
     */
    @JSFunction
    public Object next()
    {
        if ( !iterator.hasNext() )
        {
            throw new JavaScriptException(
                    NativeIterator.getStopIterationObject( getParentScope() ), null, 0 );
        }

        return Context.javaToJS( iterator.next(), this );
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
        return "NativeJavaIterator";
    }
}
