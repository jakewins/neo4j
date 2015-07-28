package org.neo4j.kernel.impl.procedures.es6;

import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.ScriptObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class NashornUtil
{
    private static final MethodHandle getScriptObject = initGetScriptObject();

    /**
     * This is a bit awful - but the Nashorn API exposes a heavily wrapped objects to us. Every single field access (such as reading a single integer) from
     * java means accessing a thread-local, creating a callable object and boxing the integer. For our use case (streaming hundreds of thousands of records
     * per second for months or years on end) this is completely unacceptable.
     *
     * However, Nashorn's internal representation of JavaScript objects has extensive support for reading primitives. This unwraps the protective
     * layer (which isn't good) and allows us efficient access to javascript objects (which is good). The safety of this is hinged on test coverage for
     * procedure output.
     */
    public static ScriptObject unwrap(ScriptObjectMirror mirror) throws Throwable
    {
        return (ScriptObject) getScriptObject.invokeExact( mirror );
    }

    private static MethodHandle initGetScriptObject()
    {
        try
        {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Method method = ScriptObjectMirror.class.getDeclaredMethod( "getScriptObject" );
            method.setAccessible( true );
            return lookup.unreflect( method );
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    /** Expose a method handle in a way nashorn understands */
    public static JSObject asJSFunction( final MethodHandle method )
    {
        return new AbstractJSObject()
        {
            @Override
            public boolean isFunction()
            {
                return true;
            }

            @Override
            public Object call( Object thiz, Object... args )
            {
                try
                {
                    return method.invokeWithArguments( args );
                }
                catch ( Throwable throwable )
                {
                    throw new RuntimeException( throwable );
                }
            }
        };
    }
}
