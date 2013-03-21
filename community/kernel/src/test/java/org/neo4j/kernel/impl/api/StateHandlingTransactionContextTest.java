package org.neo4j.kernel.impl.api;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.neo4j.helpers.Functions.constant;

import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.api.StatementContext;
import org.neo4j.kernel.api.TransactionContext;
import org.neo4j.kernel.impl.core.TransactionState;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;

public class StateHandlingTransactionContextTest
{
    @Test
    public void should_not_apply_schema_state_changes_until_commit()
    {
        // GIVEN A STATE HOLDER
        final Function<Object,String> valueCreator = constant( "created_value" );
        KernelSchemaStateStore schemaStateStore = mock( KernelSchemaStateStore.class );
        when( schemaStateStore.get( "key" ) ).thenReturn( null );

        // GIVEN A TRANSACTION CONTEXT
        TransactionContext inner = mock(TransactionContext.class);
        PersistenceCache persistenceCache = mock(PersistenceCache.class);
        TransactionState txState = null;
        SchemaCache schemaCache = null;

        StateHandlingTransactionContext transactionContext =
            new StateHandlingTransactionContext( inner, persistenceCache, txState, schemaCache, schemaStateStore );

        // GIVEN A STATEMENT CONTEXT
        StatementContext statementContext = transactionContext.newStatementContext();

        // WHEN
        statementContext.getOrCreateFromSchemaState( "key", valueCreator );

        // THEN
        verify( schemaStateStore ).get( "key" );
        verifyNoMoreInteractions( schemaStateStore );

        // WHEN
        transactionContext.commit();

        // THEN
        verify( schemaStateStore ).apply( eq( MapUtil.stringMap( "key", "created_value" ) ) );
    }

    @Test
    public void should_not_flush_schema_state_changes_until_commit() throws ConstraintViolationKernelException
    {
        // GIVEN A STATE HOLDER
        KernelSchemaStateStore stateHolder = mock( KernelSchemaStateStore.class );

        // GIVEN AN INNER STATEMENT CONTEXT
        IndexRule rule = new IndexRule( 0L, 0L, 1L );
        StatementContext innerStatementContext = mock( StatementContext.class );

        // GIVEN A TRANSACTION CONTEXT
        TransactionContext inner = mock(TransactionContext.class);
        when( inner.newStatementContext() ).thenReturn( innerStatementContext );
        PersistenceCache persistenceCache = mock(PersistenceCache.class);
        TransactionState txState = null;
        SchemaCache schemaCache = null;

        StateHandlingTransactionContext transactionContext =
            new StateHandlingTransactionContext( inner, persistenceCache, txState, schemaCache, stateHolder );

        // GIVEN A STATEMENT CONTEXT DERIVED FROM THE TRANSACTION CONTEXT
        StatementContext statementContext = transactionContext.newStatementContext();

        // WHEN UPDATING THE SCHEMA
        statementContext.dropIndexRule( rule );

        // THEN
        verifyZeroInteractions( stateHolder );

        // WHEN
        transactionContext.commit();

        // THEN
        verify( stateHolder ).flush();
    }

    private static class UpdateHolderMap implements Answer<String>
    {
        @Override
        public String answer( InvocationOnMock invocation ) throws Throwable
        {
            Object[] arguments = invocation.getArguments();
            String key = (String) arguments[0];
            Function<String, String> creator = (Function<String, String>) arguments[2];
            Map<Object, Object> targetMap = (Map<Object, Object>) arguments[3];

            String value = creator.apply( key );
            targetMap.put( key, value );
            return value;
        }
    }
}
