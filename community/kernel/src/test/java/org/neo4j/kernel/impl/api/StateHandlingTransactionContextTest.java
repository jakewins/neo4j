package org.neo4j.kernel.impl.api;

import org.junit.Test;
import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.StatementContext;
import org.neo4j.kernel.api.TransactionContext;
import org.neo4j.kernel.impl.core.TransactionState;

public class StateHandlingTransactionContextTest
{
    @Test
    public void should_not_apply_schema_state_changes_until_commit()
    {
        //GIVEN

        TransactionContext inner = null;
        PersistenceCache persistenceCache = null;
        TransactionState transactionState = null;
        SchemaCache schemaCache = null;
        SchemaStateHolder schemaStateHolder = null;
        StateHandlingTransactionContext transactionContext =
                new StateHandlingTransactionContext( inner, persistenceCache, transactionState, schemaCache,
                        schemaStateHolder );

        // WHEN
        StatementContext statementContext = transactionContext.newStatementContext();
        statementContext.getOrCreateFromSchemaState( "key", new Function<Void, Object>()
        {
            @Override
            public Object apply( Void aVoid )
            {
                return "value";
            }
        } );


    }
}
