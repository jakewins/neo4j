/*
 * Copyright (c) 2002-2016 "Neo Technology,"
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
package org.neo4j.proc;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.proc.ProcedureSignature.FieldSignature;

public class InputMappers
{
    //TODO this need to be some sort of singleton injected here
    private final TypeMappers typeMappers = new TypeMappers();

    public static class InputMapper
    {
        private final List<FieldSignature> signature;

        public InputMapper( List<FieldSignature> signature )
        {
            this.signature = signature;
        }

        public List<FieldSignature> signature()
        {
            return signature;
        }
    }

    public InputMapper mapper( Method method ) throws ProcedureException
    {
        Parameter[] params = method.getParameters();
        List<FieldSignature> signature = new ArrayList<>( params.length );

        for ( int i = 0; i < params.length; i++ )
        {
            Parameter param = params[i];

            if ( !param.isAnnotationPresent( FieldName.class ) )
            {
                throw new ProcedureException( Status.Procedure.FailedRegistration,
                        "Argument at position %d in method `%s` is missing an `@%s` annotation. " +
                        "Please add the annotation, recompile the class and try again.", i, method.getName(),
                        FieldName.class.getSimpleName() );
            }
            String name = param.getAnnotation( FieldName.class ).value();

            try
            {
                TypeMappers.ToNeoValue neoValue = typeMappers.javaToNeo( param.getType() );
                signature.add( new FieldSignature( name, neoValue.type() ) );
            }
            catch ( ProcedureException e )
            {
                throw new ProcedureException( e.status(),
                        "Argument `%s` at position %d in `%s` with type `%s` cannot be converted to a Neo4j type: %s",
                        name, i, method.getName(), param.getType().getSimpleName(), e.getLocalizedMessage() );
            }

        }

        return new InputMapper( signature );
    }
}
