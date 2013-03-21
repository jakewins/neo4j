package org.neo4j.kernel.impl.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;

public class SchemaStateOperationsTest
{
    private SchemaStateOperations inner;
    private TransactionalSchemaState stateHolder;

    @Test
    public void addIndexRuleShouldFlushStateHolder() throws ConstraintViolationKernelException
    {
        // GIVEN
        SchemaStateOperations stateOperations = new SchemaStateOperations( inner, stateHolder );

        // WHEN
        stateOperations.addIndexRule( 0L, 1L );

        // THEN
        verifyZeroInteractions( stateOperations );
    }

    @Test
    public void dropIndexRuleShouldFlushStateHolder() throws ConstraintViolationKernelException
    {
        // GIVEN
        SchemaStateOperations stateOperations = new SchemaStateOperations( inner, stateHolder );
        IndexRule rule = stateOperations.addIndexRule( 0L, 1L );

        // WHEN
        stateOperations.dropIndexRule( rule );

        // THEN
        verify( stateHolder).flush();
    }

    @Before
    public void before()
    {
        inner = mock( SchemaStateOperations.class );
        stateHolder = mock( TransactionalSchemaState.class );
    }
}
