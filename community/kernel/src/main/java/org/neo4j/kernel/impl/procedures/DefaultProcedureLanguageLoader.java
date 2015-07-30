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
package org.neo4j.kernel.impl.procedures;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.LanguageHandlers;
import org.neo4j.kernel.impl.procedures.cypher.CypherLanguageHandler;
import org.neo4j.kernel.impl.procedures.es6.ES6SoftDependency;
import org.neo4j.kernel.impl.util.JobScheduler;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;

/**
 * Loads and initializes default procedure languages at database start time.
 */
public class DefaultProcedureLanguageLoader extends LifecycleAdapter
{
    private final LanguageHandlers procedureExecutor;
    private final JobScheduler scheduler;
    private final GraphDatabaseService gds;

    public DefaultProcedureLanguageLoader( LanguageHandlers procedureExecutor, JobScheduler scheduler, GraphDatabaseService gds )
    {
        this.procedureExecutor = procedureExecutor;
        this.scheduler = scheduler;
        this.gds = gds;
    }

    @Override
    public void start() throws Throwable
    {
        procedureExecutor.addLanguageHandler( CypherLanguageHandler.LANG_CYPHER, new CypherLanguageHandler( gds ) );
        if( ES6SoftDependency.es6LanguageHandlerAvailable() )
        {
            LanguageHandler es6 = ES6SoftDependency.loadES6( scheduler.executor( JobScheduler.Groups.loadProcedureCompiler ) );
            es6.register( "neo4j.db", gds );
            procedureExecutor.addLanguageHandler( ES6SoftDependency.LANG_JS, es6 );
        }
    }
}
