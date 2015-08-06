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
package org.neo4j.kernel.impl.procedures.cypher;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;

/**
 * A language handler for cypher, allowing users to create cypher procedures.
 */
public class CypherLanguageHandler implements LanguageHandler
{
    public static final String LANG_CYPHER = "cypher";

    private GraphDatabaseService gds;

    public CypherLanguageHandler( GraphDatabaseService gds )
    {
        this.gds = gds;
    }

    @Override
    public Procedure compile( Statement statement, final ProcedureSignature signature, final String code ) throws
            ProcedureException
    {
        return new Procedure()
        {
            @Override
            public void call( Statement statement, Object[] args, final Visitor<Object[],ProcedureException> visitor ) throws ProcedureException
            {
                Map<String,Object> params = new HashMap<>();
                for ( int i = 0; i < signature.inputSignature().size(); i++ )
                {
                    params.put( signature.inputSignature().get( i ).first(), args[i] );
                }

                gds.execute( code, params ).accept( new Result.ResultVisitor<ProcedureException>()
                {
                    private Object[] record = new Object[signature.outputSignature().size()];

                    @Override
                    public boolean visit( Result.ResultRow row ) throws ProcedureException
                    {
                        for ( int i = 0; i < signature.outputSignature().size(); i++ )
                        {
                            record[i] = row.get( signature.outputSignature().get( i ).first() );
                        }
                        return visitor.visit( record );
                    }
                } );
            }
        };
    }

    @Override
    public LanguageHandler register( String nameAndNamespace, Object service )
    {
        return this;
    }

    @Override
    public LanguageHandler unregister( String nameAndNamespace )
    {
        return this;
    }
}
