/**
 * Copyright (c) 2002-2013 "Neo Technology,"
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
package org.neo4j.kernel.api;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.transaction.RollbackException;

import org.neo4j.helpers.Exceptions;
import org.neo4j.helpers.ThisShouldNotHappenError;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.InvalidTransactionTypeKernelException;
import org.neo4j.kernel.api.exceptions.ReleaseLocksFailedKernelException;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.api.exceptions.TransactionalException;
import org.neo4j.kernel.api.exceptions.schema.DropIndexFailureException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.SchemaIndexProvider;
import org.neo4j.kernel.api.labelscan.LabelScanStore;
import org.neo4j.kernel.api.operations.LegacyKernelOperations;
import org.neo4j.kernel.impl.api.IndexReaderFactory;
import org.neo4j.kernel.impl.api.LockHolder;
import org.neo4j.kernel.impl.api.PersistenceCache;
import org.neo4j.kernel.impl.api.SchemaStorage;
import org.neo4j.kernel.impl.api.SchemaWriteGuard;
import org.neo4j.kernel.impl.api.UpdateableSchemaState;
import org.neo4j.kernel.impl.api.constraints.ConstraintIndexCreator;
import org.neo4j.kernel.impl.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.api.index.IndexingService;
import org.neo4j.kernel.impl.api.index.SchemaIndexProviderMap;
import org.neo4j.kernel.impl.api.state.OldTxStateBridge;
import org.neo4j.kernel.impl.api.state.OldTxStateBridgeImpl;
import org.neo4j.kernel.impl.api.state.TxState;
import org.neo4j.kernel.impl.api.state.TxStateImpl;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.core.TransactionState;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;
import org.neo4j.kernel.impl.nioneo.store.NeoStore;
import org.neo4j.kernel.impl.nioneo.store.UniquenessConstraintRule;
import org.neo4j.kernel.impl.persistence.PersistenceManager;
import org.neo4j.kernel.impl.transaction.AbstractTransactionManager;

/**
 * This class should replace the {@link KernelTransaction} interface, and take its name, as soon as
 * {@code TransitionalTxManagementKernelTransaction} is gone from {@code server}.
 */
public class KernelTransactionImplementation implements KernelTransaction, TxState.Holder
{
    private final SchemaWriteGuard schemaWriteGuard;
    private final IndexingService indexService;
    private final LockHolder lockHolder;
    private final LabelScanStore labelScanStore;
    private final SchemaStorage schemaStorage;
    private final ConstraintIndexCreator constraintIndexCreator;
    private final PersistenceCache persistenceCache;
    private final PersistenceManager persistenceManager;
    private final SchemaIndexProviderMap providerMap;
    private final UpdateableSchemaState schemaState;
    private final OldTxStateBridge legacyStateBridge;
    private final LegacyKernelOperations legacyKernelOperations;
    private final StatementOperationParts operations;
    private final boolean readOnly;

    private TransactionType transactionType = TransactionType.ANY;
    private boolean closing, closed;
    private TxStateImpl txState;

    public KernelTransactionImplementation( StatementOperationParts operations,
                                            LegacyKernelOperations legacyKernelOperations, boolean readOnly,
                                            SchemaWriteGuard schemaWriteGuard, LabelScanStore labelScanStore,
                                            IndexingService indexService,
                                            AbstractTransactionManager transactionManager, NodeManager nodeManager,
                                            PersistenceCache persistenceCache, UpdateableSchemaState schemaState,
                                            LockHolder lockHolder, PersistenceManager persistenceManager,
                                            SchemaIndexProviderMap providerMap, NeoStore neoStore,
                                            TransactionState legacyTxState )
    {
        this.operations = operations;
        this.legacyKernelOperations = legacyKernelOperations;
        this.readOnly = readOnly;
        this.schemaWriteGuard = schemaWriteGuard;
        this.labelScanStore = labelScanStore;
        this.indexService = indexService;
        this.providerMap = providerMap;
        this.persistenceCache = persistenceCache;
        this.schemaState = schemaState;
        this.persistenceManager = persistenceManager;
        this.lockHolder = lockHolder;

        constraintIndexCreator = new ConstraintIndexCreator( new Transactor( transactionManager, persistenceManager ),
                this.indexService );
        schemaStorage = new SchemaStorage( neoStore.getSchemaStore() );
        legacyStateBridge = new OldTxStateBridgeImpl( nodeManager, legacyTxState );
    }

    public void prepare()
    {
        beginClose();
        try
        {
            createTransactionCommands();
        }
        finally
        {
            closing = false;
        }
    }

    public void commit() throws TransactionFailureException
    {
        try
        {
            lockHolder.releaseLocks();
            close();
        }
        catch ( ReleaseLocksFailedKernelException e )
        {
            throw new TransactionFailureException( new RuntimeException(e.getMessage(), e) );
        }
        finally
        {
            closing = false;
        }
    }

    public void rollback() throws TransactionFailureException
    {
        beginClose();
        try
        {
            try
            {
                dropCreatedConstraintIndexes();
            }
            catch ( IllegalStateException e )
            {
                throw new TransactionFailureException( e );
            }
            catch ( SecurityException e )
            {
                throw new TransactionFailureException( e );
            }
            finally
            {
                try
                {
                    lockHolder.releaseLocks();
                }
                catch ( ReleaseLocksFailedKernelException e )
                {
                    throw new TransactionFailureException(
                            Exceptions.withCause( new RollbackException( e.getMessage() ), e ) );
                }
            }
            close();
        }
        finally
        {
            closing = false;
        }
    }

    /** Implements reusing the same underlying {@link KernelStatement} for overlapping statements. */
    private KernelStatement currentStatement;

    @Override
    public KernelStatement acquireStatement()
    {
        assertOpen();
        if ( currentStatement == null )
        {
            currentStatement = new KernelStatement( this, new IndexReaderFactory.Caching( indexService ),labelScanStore,
                    this, lockHolder, legacyKernelOperations, operations );
        }
        currentStatement.acquire();
        return currentStatement;
    }

    public void releaseStatement( Statement statement )
    {
        assert currentStatement == statement;
        currentStatement = null;
    }

    public void upgradeToDataTransaction() throws InvalidTransactionTypeKernelException, ReadOnlyDatabaseKernelException
    {
        assertDatabaseWritable();
        transactionType = transactionType.upgradeToDataTransaction();
    }

    public void upgradeToSchemaTransaction() throws InvalidTransactionTypeKernelException, ReadOnlyDatabaseKernelException
    {
        doUpgradeToSchemaTransaction();
        transactionType = transactionType.upgradeToSchemaTransaction();
    }

    public void doUpgradeToSchemaTransaction() throws InvalidTransactionTypeKernelException, ReadOnlyDatabaseKernelException
    {
        assertDatabaseWritable();
        schemaWriteGuard.assertSchemaWritesAllowed();
    }

    private void assertDatabaseWritable() throws ReadOnlyDatabaseKernelException
    {
        if ( readOnly )
        {
            throw new ReadOnlyDatabaseKernelException();
        }
    }

    public void assertTokenWriteAllowed() throws ReadOnlyDatabaseKernelException
    {
        assertDatabaseWritable();
    }

    private void dropCreatedConstraintIndexes() throws TransactionFailureException
    {
        if ( hasTxStateWithChanges() )
        {
            for ( IndexDescriptor createdConstraintIndex : txState().constraintIndexesCreatedInTx() )
            {
                try
                {
                    // TODO logically, which statement should this operation be performed on?
                    constraintIndexCreator.dropUniquenessConstraintIndex( createdConstraintIndex );
                }
                catch ( DropIndexFailureException e )
                {
                    throw new IllegalStateException( "Constraint index that was created in a transaction should " +
                            "be " +
                            "possible to drop during rollback of that transaction.", e );
                }
                catch ( TransactionFailureException e )
                {
                    throw e;
                }
                catch ( TransactionalException e )
                {
                    throw new IllegalStateException( "The transaction manager could not fulfill the transaction " +
                            "for " +
                            "dropping the constraint.", e );
                }
            }
        }
    }

    @Override
    public TxState txState()
    {
        if ( !hasTxState() )
        {
            txState = new TxStateImpl( legacyStateBridge, persistenceManager, null );
        }
        return txState;
    }

    @Override
    public boolean hasTxState()
    {
        return null != txState;
    }

    @Override
    public boolean hasTxStateWithChanges()
    {
        return legacyStateBridge.hasChanges() || (hasTxState() && txState.hasChanges());
    }

    private void close()
    {
        assertOpen();
        closed = true;
        if ( currentStatement != null )
        {
            currentStatement.forceClose();
            currentStatement = null;
        }
    }

    private void beginClose()
    {
        assertOpen();
        if ( closing )
        {
            throw new IllegalStateException( "This transaction is already being closed." );
        }
        if ( currentStatement != null )
        {
            currentStatement.forceClose();
            currentStatement = null;
        }
        closing = true;
    }

    private void createTransactionCommands()
    {
        if ( hasTxStateWithChanges() )
        {
            final AtomicBoolean clearState = new AtomicBoolean( false );
            txState().accept( new TxState.Visitor()
            {
                @Override
                public void visitNodeLabelChanges( long id, Set<Integer> added, Set<Integer> removed )
                {
                    // TODO: move store level changes here.
                }

                @Override
                public void visitAddedIndex( IndexDescriptor element, boolean isConstraintIndex )
                {
                    SchemaIndexProvider.Descriptor providerDescriptor = providerMap.getDefaultProvider()
                            .getProviderDescriptor();
                    IndexRule rule;
                    if ( isConstraintIndex )
                    {
                        rule = IndexRule.constraintIndexRule( schemaStorage.newRuleId(), element.getLabelId(),
                                element.getPropertyKeyId(), providerDescriptor,
                                null );
                    }
                    else
                    {
                        rule = IndexRule.indexRule( schemaStorage.newRuleId(), element.getLabelId(),
                                element.getPropertyKeyId(), providerDescriptor );
                    }
                    persistenceManager.createSchemaRule( rule );
                }

                @Override
                public void visitRemovedIndex( IndexDescriptor element, boolean isConstraintIndex )
                {
                    try
                    {
                        IndexRule rule = schemaStorage
                                .indexRule( element.getLabelId(), element.getPropertyKeyId() );
                        persistenceManager.dropSchemaRule( rule );
                    }
                    catch ( SchemaRuleNotFoundException e )
                    {
                        throw new ThisShouldNotHappenError(
                                "Tobias Lindaaker",
                                "Index to be removed should exist, since its existence should have " +
                                        "been validated earlier and the schema should have been locked." );
                    }
                }

                @Override
                public void visitAddedConstraint( UniquenessConstraint element )
                {
                    clearState.set( true );
                    long constraintId = schemaStorage.newRuleId();
                    IndexRule indexRule;
                    try
                    {
                        indexRule = schemaStorage.indexRule( element.label(), element.propertyKeyId() );
                    }
                    catch ( SchemaRuleNotFoundException e )
                    {
                        throw new ThisShouldNotHappenError(
                                "Jacob Hansson",
                                "Index is always created for the constraint before this point.");
                    }
                    persistenceManager.createSchemaRule( UniquenessConstraintRule.uniquenessConstraintRule(
                            constraintId, element.label(), element.propertyKeyId(), indexRule.getId() ) );
                    persistenceManager.setConstraintIndexOwner( indexRule, constraintId );
                }

                @Override
                public void visitRemovedConstraint( UniquenessConstraint element )
                {
                    try
                    {
                        clearState.set( true );
                        UniquenessConstraintRule rule = schemaStorage
                                .uniquenessConstraint( element.label(), element.propertyKeyId() );
                        persistenceManager.dropSchemaRule( rule );
                    }
                    catch ( SchemaRuleNotFoundException e )
                    {
                        throw new ThisShouldNotHappenError(
                                "Tobias Lindaaker",
                                "Constraint to be removed should exist, since its existence should " +
                                        "have been validated earlier and the schema should have been locked." );
                    }
                    // Remove the index for the constraint as well
                    visitRemovedIndex( new IndexDescriptor( element.label(), element.propertyKeyId() ), true );
                }
            } );
            if ( clearState.get() )
            {
                schemaState.clear();
            }
        }
    }

    private void assertOpen()
    {
        if ( closed )
        {
            throw new IllegalStateException( "This transaction has already been completed." );
        }
    }

    public boolean isReadOnly()
    {
        return !hasTxState() || !txState.hasChanges();
    }

    private enum TransactionType
    {
        ANY,
        DATA
                {
                    @Override
                    TransactionType upgradeToSchemaTransaction() throws InvalidTransactionTypeKernelException
                    {
                        throw new InvalidTransactionTypeKernelException(
                                "Cannot perform schema updates in a transaction that has performed data updates." );
                    }
                },
        SCHEMA
                {
                    @Override
                    TransactionType upgradeToDataTransaction() throws InvalidTransactionTypeKernelException
                    {
                        throw new InvalidTransactionTypeKernelException(
                                "Cannot perform data updates in a transaction that has performed schema updates." );
                    }
                };

        TransactionType upgradeToDataTransaction() throws InvalidTransactionTypeKernelException
        {
            return DATA;
        }

        TransactionType upgradeToSchemaTransaction() throws InvalidTransactionTypeKernelException
        {
            return SCHEMA;
        }
    }
}
