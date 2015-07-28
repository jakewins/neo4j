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
package org.neo4j.kernel.api.procedure;

import org.neo4j.kernel.api.Statement;

/**
 * Used by the stored procedures service to compile procedures in arbitrary languages. Implement one of these and register
 * it with the procedures service to make new languages available.
 */
public interface LanguageHandler
{
    /**
     * Given source code and a signature, compile a procedure object that can later be invoked.
     * @param statement the statement context this operation is executed in, may be used to perform data operations as part of compilation
     * @param signature the procedure input/output signature, the compiled procedure *must* abide by this
     * @param code the source code for the procedure, language handler specific
     * @return a runnable procedure
     * @throws ProcedureException
     */
    Procedure compile(Statement statement, ProcedureSignature signature, String code) throws ProcedureException;

    /**
     * Register a service to be made available to procedures in this language handler. This operation is optional and how the service is exposed is
     * language-handler specific.
     * @param nameAndNamespace dot-separated namespace and name of the service
     * @param service the service instance, any java object
     * @return this language handler instance
     */
    LanguageHandler register( String nameAndNamespace, Object service );

    /**
     * Unregister a service.
     * @see #register(String, Object)
     * @param nameAndNamespace dot-separated namespace and name of the service
     * @return this language handler instance
     */
    LanguageHandler unregister( String nameAndNamespace );
}
