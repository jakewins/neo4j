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

import javax.script.ScriptException;

import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.ProcedureSignature.Argument;

/**
 * Wraps a raw javascript script in a function block with a signature matching the procedure signature.
 */
public class JSProcedureBoilerplate
{
    private static final String EMIT = "$emit";
    private static final String PROCEDURE_BOILERPLATE =
            "%s = function %s { " +
              "function emit() { $emit.apply( arguments ); } " +
              "%s " +
            "}";

    public String wrapAsProcedureFunction( ProcedureSignature signature, String functionBody ) throws ScriptException, ProcedureException
    {
        String namespaceDef = namespaceDefinition( signature );
        String jsSignature = argSignature( signature );

        return String.format( PROCEDURE_BOILERPLATE, namespaceDef, jsSignature, functionBody);
    }

    private String argSignature( ProcedureSignature signature )
    {
        StringBuilder out = new StringBuilder( );

        out.append( "( " );

        // Special argument containing the emit function, injected for each invocation of the procedure
        out.append( EMIT );

        for ( Argument sig : signature.inputSignature() )
        {
            out.append( ", " ).append( sig.name() );
        }
        out.append( " )" );

        return out.toString();
    }

    private String namespaceDefinition( ProcedureSignature signature )
    {
        StringBuilder ns = new StringBuilder();
        StringBuilder out = new StringBuilder();
        for ( String s : signature.name().namespace() )
        {
            ns.append( s );
            out.append( String.format( "var %1$s=%1$s||{};", ns.toString() ) );
            ns.append( "." );
        }
        out.append( ns ).append( signature.name().name() );
        return out.toString();
    }
}
