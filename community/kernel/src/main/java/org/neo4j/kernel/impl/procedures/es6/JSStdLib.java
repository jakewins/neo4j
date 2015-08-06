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
package org.neo4j.kernel.impl.procedures.es6;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.api.procedure.ProcedureException;

import static java.lang.invoke.MethodType.methodType;
import static org.neo4j.kernel.impl.procedures.es6.NashornUtil.asJSFunction;

public class JSStdLib implements Visitor<Bindings,ProcedureException>
{
    private final Map<String,Object> services = new HashMap<>();

    public JSStdLib() throws NoSuchMethodException, IllegalAccessException
    {
        // Always available
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        register( "relType",
                asJSFunction( lookup.findStatic( DynamicRelationshipType.class, "withName", methodType( DynamicRelationshipType.class, String.class ) )));
        register( "label", asJSFunction( lookup.findStatic( DynamicLabel.class, "label", methodType( Label.class, String.class ) )));
        register( "neo4j.OUTGOING", Direction.OUTGOING );
        register( "neo4j.INCOMING", Direction.INCOMING );
        register( "neo4j.BOTH", Direction.BOTH );
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
    public JSStdLib register( String nameAndNamespace, Object service )
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
