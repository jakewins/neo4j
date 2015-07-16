package org.neo4j.kernel.impl.procedures.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.helpers.ThisShouldNotHappenError;
import org.neo4j.helpers.collection.Visitor;

/**
 * TODO
 */
public class Neo4jRhinoStdLib implements Visitor<Scriptable,RuntimeException>
{
    private final Map<String, Object> bindings = new HashMap<>();

    public Object require( String key )
    {
        Object o = bindings.get( key );
        if(o == null)
        {
            throw new IllegalArgumentException( String.format("'%s' cannot be found by require().", key) );
        }

        return o;
    }

    /** Make an object available to 'require' in javascript-land. */
    public Neo4jRhinoStdLib bind( String name, Object instance )
    {
        bindings.put( name, instance );
        return this;
    }

    @Override
    public boolean visit( Scriptable scope )
    {
        try
        {
            scope.put( "require", scope,
                    new FunctionObject("require", getClass().getMethod( "require", String.class ), scope){
                        @Override
                        public Object call( Context cx, Scriptable scope, Scriptable thisObj, Object[] args )
                        {
                            return require( (String) args[0] );
                        }
                    } );
            scope.put( "label", scope,
                    new FunctionObject("label", DynamicLabel.class.getMethod( "label", String.class ), scope));
        }
        catch ( NoSuchMethodException e )
        {
            throw new ThisShouldNotHappenError( "jake", "This function must exist at this point." );
        }
        return false;
    }
}