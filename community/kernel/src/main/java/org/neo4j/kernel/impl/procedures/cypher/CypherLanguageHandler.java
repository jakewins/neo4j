package org.neo4j.kernel.impl.procedures.cypher;

import java.io.InputStream;
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

import static org.neo4j.kernel.impl.util.IoPrimitiveUtils.readAsString;

/**
 * TODO
 */
public class CypherLanguageHandler
        implements LanguageHandler
{
    public static final String CYPHER_JS = "cypher";

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
                for ( int i = 0; i < signature.getInputSignature().size(); i++ )
                {
                    Pair<String,Neo4jTypes.AnyType> arg =
                            signature.getInputSignature().get( i );
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
            record = new Object[signature.getOutputSignature().size()];
        }

        @Override
        public Object[] getRecord()
        {
            return record;
        }

        @Override
        public boolean next()
        {
            if ( result.hasNext() )
            {
                Map<String,Object> row = result.next();
                for ( int i = 0; i < signature.getOutputSignature().size(); i++ )
                {
                    Pair<String,Neo4jTypes.AnyType> arg =
                            signature.getOutputSignature().get( i );
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
}
