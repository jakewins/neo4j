package org.neo4j.kernel.impl.procedures.es6;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Cross-compiles ES6 javascript to a more compatible format.
 */
public class ES6Transpiler
{
    private final ScriptEngine engine;
    private final Compilable compiler;

    public ES6Transpiler() throws ScriptException, FileNotFoundException
    {

        ScriptEngineManager em = new ScriptEngineManager();
        this.engine = em.getEngineByName( "nashorn" );
        this.compiler = (Compilable) engine;

        ClassLoader loader = getClass().getClassLoader();
        URL resource = loader.getResource( "js/jvm-npm.js" );
        System.out.println(resource);

        InputStream stream = getClass().getClassLoader().getResourceAsStream( "js/jvm-npm.js" );
        FileInputStream fileInputStream = new FileInputStream( "/Users/jake/Code/traceur-compiler/bin/traceur.js" );
        engine.eval( new InputStreamReader( fileInputStream ));

    }

    public void translate( String script ) throws ScriptException
    {
        Object eval = engine.eval( String.format( "babel.transform(\"%s\").code;", script ) );
        System.out.println(eval);
    }

    public static void main(String ... args) throws ScriptException, FileNotFoundException
    {
        ES6Transpiler piler = new ES6Transpiler();
        piler.translate( "class Test {}" );
    }
}
