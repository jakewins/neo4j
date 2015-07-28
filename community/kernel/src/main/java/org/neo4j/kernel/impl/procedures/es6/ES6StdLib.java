package org.neo4j.kernel.impl.procedures.es6;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.api.procedure.ProcedureException;

import static java.lang.invoke.MethodType.methodType;
import static org.neo4j.kernel.impl.procedures.es6.NashornUtil.asJSFunction;

public class ES6StdLib implements Visitor<Bindings,ProcedureException>
{
    private final Map<String,Object> services = new HashMap<>();

    public ES6StdLib() throws NoSuchMethodException, IllegalAccessException
    {
        // Always available
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        register( "relType",
                asJSFunction( lookup.findStatic( DynamicRelationshipType.class, "withName", methodType( DynamicRelationshipType.class, String.class ) )));
        register( "label",
                asJSFunction( lookup.findStatic( DynamicLabel.class, "label", methodType( Label.class, String.class ) )));
    }


    @Override
    public boolean visit( Bindings bindings ) throws ProcedureException
    {
        for ( Map.Entry<String,Object> entry : services.entrySet() )
        {
            bind( bindings, entry );
        }
        return true;
    }

    private void bind( Bindings bindings, Map.Entry<String,Object> entry )
    {
        String[] keys = entry.getKey().split("\\.");
        Map<String, Object> level = bindings;
        for ( int i = 0; i < keys.length; i++ )
        {
            String namespace = keys[i];
            if( i < keys.length - 1 )
            {
                if( !level.containsKey( namespace ) )
                {
                    level.put( namespace, new HashMap<String, Object>() );
                }

                Object next = level.get( namespace );
                if( !(next instanceof Map))
                {
                    throw new IllegalStateException(
                            String.format("Service uses name that another service declares as namespace: '%s' in '%s'.", namespace, entry.getKey()) );
                }

                level = (Map<String, Object>)next;
            }
            else
            {
                level.put( namespace, entry.getValue() );
            }
        }
    }

    /**
     * Register a global service to be made available to ES6 procedures. You can use dots to separate namespaces in the name. The service object can be any
     * java object, it will be exposed as-is via Nashorns transparent java->javascript mapping mechanisms.
     */
    public ES6StdLib register( String nameAndNamespace, Object service )
    {
        if(services.containsKey( nameAndNamespace ))
        {
            throw new IllegalArgumentException( String.format("'%s' is already a registered procedure service.", nameAndNamespace) );
        }
        services.put( nameAndNamespace, service );
        return this;
    }

    public void unregister( String nameAndNamespace )
    {
        services.remove( nameAndNamespace );
    }
}
