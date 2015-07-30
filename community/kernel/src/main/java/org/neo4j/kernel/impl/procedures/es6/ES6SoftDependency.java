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

import java.util.concurrent.Executor;

import org.neo4j.kernel.api.procedure.LanguageHandler;

/**
 * Nashorn is only available on JDK8+, so we can't directly link in Nashorn classes on JDK7
 */
public class ES6SoftDependency
{
    public static final String LANG_JS = "javascript";

    public static boolean es6LanguageHandlerAvailable()
    {
        try
        {
            Class.forName( "jdk.nashorn.api.scripting.ScriptObjectMirror" );
            return true;
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }

    public static LanguageHandler loadES6()
    {
        return loadES6( new Executor()
        {
            @Override
            public void execute( Runnable command )
            {
                command.run();
            }
        });
    }

    public static LanguageHandler loadES6( Executor executor )
    {
        try
        {
            Class<LanguageHandler> handler = (Class<LanguageHandler>) Class.forName( "org.neo4j.kernel.impl.procedures.es6.ES6LanguageHandler" );
            return handler.getDeclaredConstructor( Executor.class ).newInstance( executor );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Cannot load ES6 procedure engine on this platform, Nashorn JavaScript engine is only available from JDK 8 " +
                                             "and onwards.", e );
        }
    }

}
