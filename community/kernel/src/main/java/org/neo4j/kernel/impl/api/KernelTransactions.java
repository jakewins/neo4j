package org.neo4j.kernel.impl.api;

import org.neo4j.collection.pool.LinkedQueuePool;
import org.neo4j.collection.pool.MarshlandPool;
import org.neo4j.collection.pool.Pool;
import org.neo4j.function.Factory;
import org.neo4j.graphdb.DatabaseShutdownException;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.labelscan.LabelScanStore;
import org.neo4j.kernel.impl.api.index.IndexingService;
import org.neo4j.kernel.impl.api.index.SchemaIndexProviderMap;
import org.neo4j.kernel.impl.api.state.ConstraintIndexCreator;
import org.neo4j.kernel.impl.api.state.LegacyIndexTransactionState;
import org.neo4j.kernel.impl.api.store.PersistenceCache;
import org.neo4j.kernel.impl.api.store.StoreReadLayer;
import org.neo4j.kernel.impl.index.IndexConfigStore;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.nioneo.store.NeoStore;
import org.neo4j.kernel.impl.nioneo.xa.IntegrityValidator;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreTransactionContext;
import org.neo4j.kernel.impl.nioneo.xa.NeoStoreTransactionContextSupplier;
import org.neo4j.kernel.impl.nioneo.xa.TransactionRecordState;
import org.neo4j.kernel.impl.transaction.xaframework.TransactionHeaderInformationFactory;
import org.neo4j.kernel.impl.transaction.xaframework.TransactionMonitor;
import org.neo4j.kernel.lifecycle.LifeSupport;

/**
 * Central source of transactions in the database.
 *
 * This class maintains a pool of passive kernel transactions, and provides capabilities for listing all active
 * transactions.
 */
public class KernelTransactions implements Factory<KernelTransaction>
{
    private final Factory<KernelTransactionImplementation> factory = new Factory<KernelTransactionImplementation>()
    {
        @Override
        public KernelTransactionImplementation newInstance()
        {
            NeoStoreTransactionContext context = neoStoreTransactionContextSupplier.acquire();
            Locks.Client locksClient = locks.newClient();
            context.bind( locksClient );
            TransactionRecordState neoStoreTransaction = new TransactionRecordState(
                    neoStore.getLastCommittingTransactionId(), neoStore, integrityValidator, context );
            LegacyIndexTransactionState legacyIndexTransactionState =
                    new LegacyIndexTransactionState( indexConfigStore, legacyIndexProviderLookup );
            return new KernelTransactionImplementation( statementOperations, readOnly, schemaWriteGuard,
                    labelScanStore, indexingService, updateableSchemaState, neoStoreTransaction, providerMap,
                    neoStore, locksClient, hooks, constraintIndexCreator, transactionHeaderInformationFactory.create(),
                    commitProcess, transactionMonitor, neoStore, persistenceCache, storeLayer,
                    legacyIndexTransactionState, txPool );
        }
    };

    private final Pool<KernelTransactionImplementation> txPool = new MarshlandPool<>(
            new LinkedQueuePool<KernelTransactionImplementation>(8, factory )
    {
        @Override
        protected void dispose( KernelTransactionImplementation tx )
        {
            tx.dispose();
            super.dispose( tx );
        }
    });

    private final NeoStoreTransactionContextSupplier neoStoreTransactionContextSupplier;
    private final NeoStore neoStore;
    private final Locks locks;
    private final IntegrityValidator integrityValidator;
    private final ConstraintIndexCreator constraintIndexCreator;
    private final IndexingService indexingService;
    private final LabelScanStore labelScanStore;
    private final StatementOperationParts statementOperations;
    private final UpdateableSchemaState updateableSchemaState;
    private final SchemaWriteGuard schemaWriteGuard;
    private final SchemaIndexProviderMap providerMap;
    private final TransactionHeaderInformationFactory transactionHeaderInformationFactory;
    private final PersistenceCache persistenceCache;
    private final StoreReadLayer storeLayer;
    private final TransactionCommitProcess commitProcess;
    private final IndexConfigStore indexConfigStore;
    private final LegacyIndexApplier.ProviderLookup legacyIndexProviderLookup;
    private final TransactionHooks hooks;
    private final TransactionMonitor transactionMonitor;
    private final LifeSupport dataSourceLife;
    private final boolean readOnly;

    public KernelTransactions( NeoStoreTransactionContextSupplier neoStoreTransactionContextSupplier,
                               NeoStore neoStore, Locks locks, IntegrityValidator integrityValidator,
                               ConstraintIndexCreator constraintIndexCreator,
                               IndexingService indexingService, LabelScanStore labelScanStore,
                               StatementOperationParts statementOperations,
                               UpdateableSchemaState updateableSchemaState, SchemaWriteGuard schemaWriteGuard,
                               SchemaIndexProviderMap providerMap, TransactionHeaderInformationFactory txHeaderFactory,
                               PersistenceCache persistenceCache, StoreReadLayer storeLayer,
                               TransactionCommitProcess commitProcess,
                               IndexConfigStore indexConfigStore, LegacyIndexApplier.ProviderLookup legacyIndexProviderLookup,
                               TransactionHooks hooks, TransactionMonitor transactionMonitor, LifeSupport dataSourceLife, boolean readOnly )
    {
        this.neoStoreTransactionContextSupplier = neoStoreTransactionContextSupplier;
        this.neoStore = neoStore;
        this.locks = locks;
        this.integrityValidator = integrityValidator;
        this.constraintIndexCreator = constraintIndexCreator;
        this.indexingService = indexingService;
        this.labelScanStore = labelScanStore;
        this.statementOperations = statementOperations;
        this.updateableSchemaState = updateableSchemaState;
        this.schemaWriteGuard = schemaWriteGuard;
        this.providerMap = providerMap;
        this.transactionHeaderInformationFactory = txHeaderFactory;
        this.persistenceCache = persistenceCache;
        this.storeLayer = storeLayer;
        this.commitProcess = commitProcess;
        this.indexConfigStore = indexConfigStore;
        this.legacyIndexProviderLookup = legacyIndexProviderLookup;
        this.hooks = hooks;
        this.transactionMonitor = transactionMonitor;
        this.dataSourceLife = dataSourceLife;
        this.readOnly = readOnly;
    }

    @Override
    public KernelTransaction newInstance()
    {
        assertDatabaseIsRunning();
        return txPool.acquire().initialize(transactionHeaderInformationFactory.create());
    }

    private void assertDatabaseIsRunning()
    {
        // TODO: Copied over from original source in NeoXADS - this should probably use DBAvailability, rather than this.
        if ( !dataSourceLife.isRunning() )
        {
            throw new DatabaseShutdownException();
        }
    }
}
