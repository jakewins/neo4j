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
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.LanguageHandler;
import org.neo4j.kernel.api.procedure.Procedure;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.store.Neo4jTypes;

/**
 * TODO
 */
public class CypherLanguageHandler
        implements LanguageHandler
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
            public RecordCursor call( Statement statement, Object[] args )
            {
                Map<String,Object> params = new HashMap<>();
                for ( int i = 0; i < signature.inputSignature().size(); i++ )
                {
                    Pair<String,Neo4jTypes.AnyType> arg =
                            signature.inputSignature().get( i );
                    params.put( arg.first(), args[i] );
                }

                Result result = gds.execute( code, params );
                return new CypherRecordCursor( result, signature );
            }
        };
    }

    private class CypherRecordCursor implements RecordCursor
    {
        private Result result;
        private ProcedureSignature signature;
        private Object[] record;

        public CypherRecordCursor( Result result, ProcedureSignature signature )
        {
            this.result = result;
            this.signature = signature;
            record = new Object[signature.outputSignature().size()];
        }

        @Override
        public Object[] record()
        {
            return record;
        }

        @Override
        public boolean next()
        {
            if ( result.hasNext() )
            {
                Map<String,Object> row = result.next();
                for ( int i = 0; i < signature.outputSignature().size(); i++ )
                {
                    Pair<String,Neo4jTypes.AnyType> arg =
                            signature.outputSignature().get( i );
                    record[i] = row.get( arg.first() );
                }
                return true;
            }
            else
            {
                return false;
            }
        }

        @Override
        public void close()
        {
            result.close();
        }
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
