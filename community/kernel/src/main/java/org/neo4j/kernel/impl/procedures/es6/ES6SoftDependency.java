package org.neo4j.kernel.impl.procedures.es6;

import org.neo4j.kernel.api.procedure.LanguageHandler;

/**
 * Nashorn is only available on JDK8+, so we can't directly link in Nashorn classes on JDK7
 */
public class ES6SoftDependency
{
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
        try
        {
            Class<LanguageHandler> handler = (Class<LanguageHandler>) Class.forName( "org.neo4j.kernel.impl.procedures.es6.ES6LanguageHandler" );
            return handler.getDeclaredConstructor().newInstance();
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Cannot load ES6 procedure engine on this platform, Nashorn JavaScript engine is only available from JDK 8 " +
                                             "and onwards." );
        }
    }

}
