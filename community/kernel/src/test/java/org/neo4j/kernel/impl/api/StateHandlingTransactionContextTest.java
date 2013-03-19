package org.neo4j.kernel.impl.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

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

        TransactionContext inner = mock(TransactionContext.class);
        PersistenceCache persistenceCache = null;
        TransactionState transactionState = null;
        SchemaCache schemaCache = null;
        KernelSchemaStateHolder schemaStateHolder = new KernelSchemaStateHolder();
        StateHandlingTransactionContext transactionContext =
                new StateHandlingTransactionContext( inner, persistenceCache, transactionState, schemaCache,
                        schemaStateHolder );

        // WHEN
        StatementContext statementContext = transactionContext.newStatementContext();
        statementContext.getOrCreateFromSchemaState( "key", String.class, new Function<String, String>()
        {
            @Override
            public String apply( String key )
            {
                return "value";
            }
        } );

        // THEN
        String result = schemaStateHolder.getOrCreate( "key", String.class, new Function<String, String>()
        {
            @Override
            public String apply( String key )
            {
                return "default";
            }
        } );

        assertEquals("default", result);
    }
}
