package org.neo4j.kernel.impl.procedures.es6;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;

public class EcmaScript6LanguageHandler implements LanguageHandler
{
    private final ScriptEngine engine;
    private final Compilable compiler;

    public EcmaScript6LanguageHandler()
    {
        ScriptEngineManager em = new ScriptEngineManager();
        this.engine = em.getEngineByName( "nashorn" );
        this.compiler = (Compilable) engine;
    }

    @Override
    public Procedure compile( Statement statement, ProcedureSignature signature, String code ) throws ProcedureException
    {
        try
        {
            return new ES6Procedure( compiler.compile( code ), engine, signature );
        }
        catch ( Exception e )
        {
            throw new ProcedureException( Status.Schema.ProcedureCompilationError, e, "Failed to compile javascript: '%s'", code );
        }
    }
}
