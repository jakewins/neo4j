package org.neo4j.kernel.impl.api;

import org.junit.Test;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.nioneo.store.NeoStore;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreTransactionContext;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreTransactionContextSupplier;
import org.neo4j.kernel.impl.transaction.xaframework.TransactionHeaderInformationFactory;
import org.neo4j.kernel.impl.transaction.xaframework.TransactionMonitorImpl;
import org.neo4j.kernel.lifecycle.LifeSupport;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KernelTransactionsTest
{

    @Test
    public void shouldListActiveTransactions() throws Exception
    {
        // Given
        LifeSupport life = new LifeSupport();
        life.start();

        Locks locks = mock( Locks.class );
        when(locks.newClient()).thenReturn( mock( Locks.Client.class ) );

        KernelTransactions registry = new KernelTransactions(
                new MockContextSupplier(), mock(NeoStore.class), locks, null, null, null, null, null, null,
                null, null, TransactionHeaderInformationFactory.DEFAULT, null, null, null, null, null,
                new TransactionHooks(), new TransactionMonitorImpl(), life,
                false );

        // When
        KernelTransaction first  = registry.newInstance();
        KernelTransaction second = registry.newInstance();
        KernelTransaction third  = registry.newInstance();

        first.close();

        // Then
        assertThat( registry.activeTransactions(), equalTo(asList(second, third)) );
    }

    private static class MockContextSupplier extends NeoStoreTransactionContextSupplier
    {
        public MockContextSupplier()
        {
            super( null );
        }

        @Override
        protected NeoStoreTransactionContext create()
        {
            return mock(NeoStoreTransactionContext.class);
        }
    }
}
