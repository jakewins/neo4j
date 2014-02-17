/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.kernel.impl.api.gen;

import java.util.Iterator;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.AddIndexFailureException;
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException;
import org.neo4j.kernel.api.exceptions.schema.AlreadyIndexedException;
import org.neo4j.kernel.api.exceptions.schema.ConstraintValidationKernelException;
import org.neo4j.kernel.api.exceptions.schema.CreateConstraintFailureException;
import org.neo4j.kernel.api.exceptions.schema.DropConstraintFailureException;
import org.neo4j.kernel.api.exceptions.schema.DropIndexFailureException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.api.properties.DefinedProperty;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.impl.api.KernelStatement;
import org.neo4j.kernel.impl.api.operations.StatementLayer;
import org.neo4j.kernel.impl.nioneo.store.SchemaStorage;

public final class GeneratedLockingStatementOperations implements StatementLayer
{
    public GeneratedLockingStatementOperations(
            GeneratedConstraintEnforcingEntityOperations __ConstraintEnforcingEntityOperations,
            GeneratedStateHandlingStatementOperations __StateHandlingStatementOperations,
            GeneratedSchemaStateConcern __SchemaStateConcern,
            GeneratedCacheLayer __CacheLayer) {
        this.__ConstraintEnforcingEntityOperations = __ConstraintEnforcingEntityOperations;
        this.__StateHandlingStatementOperations = __StateHandlingStatementOperations;
        this.__SchemaStateConcern = __SchemaStateConcern;
        this.__CacheLayer = __CacheLayer;
    }

    private final GeneratedConstraintEnforcingEntityOperations __ConstraintEnforcingEntityOperations;
    private final GeneratedStateHandlingStatementOperations __StateHandlingStatementOperations;
    private final GeneratedSchemaStateConcern __SchemaStateConcern;
    private final GeneratedCacheLayer __CacheLayer;

    @Override
    public boolean nodeAddLabel( KernelStatement state, long nodeId, int labelId )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        // TODO (BBC, 22/11/13):
        // In order to enforce constraints we need to check whether this change violates constraints; we therefore need
        // the schema lock to ensure that our view of constraints is consistent.
        //
        // We would like this locking to be done naturally when ConstraintEnforcingEntityOperations calls
        // SchemaReadOperations#constraintsGetForLabel, but the SchemaReadOperations object that
        // ConstraintEnforcingEntityOperations has a reference to does not lock because of the way the cake is
        // constructed.
        //
        // It would be cleaner if the schema and data cakes were separated so that the SchemaReadOperations object used
        // by ConstraintEnforcingEntityOperations included the full cake, with locking included.
        state.locks().acquireSchemaReadLock();

        state.locks().acquireNodeWriteLock( nodeId );
        return this.__ConstraintEnforcingEntityOperations.nodeAddLabel( state, nodeId, labelId );
    }

    @Override
    public boolean nodeRemoveLabel( KernelStatement state, long nodeId, int labelId ) throws EntityNotFoundException
    {
        state.locks().acquireNodeWriteLock( nodeId );
        return this.__StateHandlingStatementOperations.nodeRemoveLabel( state, nodeId, labelId );
    }

    @Override
    public IndexDescriptor indexCreate( KernelStatement state, int labelId, int propertyKey )
            throws AddIndexFailureException, AlreadyIndexedException, AlreadyConstrainedException
    {
        state.locks().acquireSchemaWriteLock();
        return this.__StateHandlingStatementOperations.indexCreate( state, labelId, propertyKey );
    }

    @Override
    public void indexDrop( KernelStatement state, IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        state.locks().acquireSchemaWriteLock();
        this.__StateHandlingStatementOperations.indexDrop( state, descriptor );
    }

    @Override
    public void uniqueIndexDrop( KernelStatement state, IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        state.locks().acquireSchemaWriteLock();
        this.__StateHandlingStatementOperations.uniqueIndexDrop( state, descriptor );
    }

    @Override
    public <K, V> V schemaStateGetOrCreate( KernelStatement state, K key, Function<K, V> creator )
    {
        state.locks().acquireSchemaReadLock();
        return this.__SchemaStateConcern.schemaStateGetOrCreate( state, key, creator );
    }

    @Override
    public <K> boolean schemaStateContains( KernelStatement state, K key )
    {
        state.locks().acquireSchemaReadLock();
        return this.__SchemaStateConcern.schemaStateContains( state, key );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetForLabel( KernelStatement state, int labelId )
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.indexesGetForLabel( state, labelId );
    }

    @Override
    public IndexDescriptor indexesGetForLabelAndPropertyKey( KernelStatement state, int labelId, int propertyKey )
            throws SchemaRuleNotFoundException
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.indexesGetForLabelAndPropertyKey( state, labelId, propertyKey );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetAll( KernelStatement state )
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.indexesGetAll( state );
    }

    @Override
    public InternalIndexState indexGetState( KernelStatement state, IndexDescriptor descriptor ) throws IndexNotFoundKernelException
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.indexGetState( state, descriptor );
    }

    @Override
    public Long indexGetOwningUniquenessConstraintId( KernelStatement state, IndexDescriptor index ) throws SchemaRuleNotFoundException
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.indexGetOwningUniquenessConstraintId( state, index );
    }

    @Override
    public long indexGetCommittedId( KernelStatement state, IndexDescriptor index, SchemaStorage.IndexRuleKind kind )
            throws SchemaRuleNotFoundException
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.indexGetCommittedId( state, index, kind );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetForLabel( KernelStatement state, int labelId )
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.uniqueIndexesGetForLabel( state, labelId );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetAll( KernelStatement state )
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.uniqueIndexesGetAll( state );
    }

    @Override
    public void nodeDelete( KernelStatement state, long nodeId )
    {
        state.locks().acquireNodeWriteLock( nodeId );
        this.__StateHandlingStatementOperations.nodeDelete( state, nodeId );
    }

    @Override
    public long relationshipCreate( KernelStatement state, int relationshipTypeId, long startNodeId, long endNodeId )
    {
        state.locks().acquireNodeWriteLock( startNodeId );
        state.locks().acquireNodeWriteLock( endNodeId );
        return this.__StateHandlingStatementOperations.relationshipCreate( state, relationshipTypeId, startNodeId, endNodeId );
    }

    @Override
    public void relationshipDelete( KernelStatement state, long relationshipId )
    {
        state.locks().acquireRelationshipWriteLock( relationshipId );
        this.__StateHandlingStatementOperations.relationshipDelete( state, relationshipId );
    }

    @Override
    public UniquenessConstraint uniquenessConstraintCreate( KernelStatement state, int labelId, int propertyKeyId )
            throws CreateConstraintFailureException, AlreadyConstrainedException, AlreadyIndexedException
    {
        state.locks().acquireSchemaWriteLock();
        return this.__StateHandlingStatementOperations.uniquenessConstraintCreate( state, labelId, propertyKeyId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabelAndPropertyKey( KernelStatement state, int labelId, int propertyKeyId )
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.constraintsGetForLabelAndPropertyKey( state, labelId, propertyKeyId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabel( KernelStatement state, int labelId )
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.constraintsGetForLabel( state, labelId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetAll( KernelStatement state )
    {
        state.locks().acquireSchemaReadLock();
        return this.__StateHandlingStatementOperations.constraintsGetAll( state );
    }

    @Override
    public void constraintDrop( KernelStatement state, UniquenessConstraint constraint )
            throws DropConstraintFailureException
    {
        state.locks().acquireSchemaWriteLock();
        this.__StateHandlingStatementOperations.constraintDrop( state, constraint );
    }

    @Override
    public Property nodeSetProperty( KernelStatement state, long nodeId, DefinedProperty property )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        // TODO (BBC, 22/11/13):
        // In order to enforce constraints we need to check whether this change violates constraints; we therefore need
        // the schema lock to ensure that our view of constraints is consistent.
        //
        // We would like this locking to be done naturally when ConstraintEnforcingEntityOperations calls
        // SchemaReadOperations#constraintsGetForLabel, but the SchemaReadOperations object that
        // ConstraintEnforcingEntityOperations has a reference to does not lock because of the way the cake is
        // constructed.
        //
        // It would be cleaner if the schema and data cakes were separated so that the SchemaReadOperations object used
        // by ConstraintEnforcingEntityOperations included the full cake, with locking included.
        state.locks().acquireSchemaReadLock();

        state.locks().acquireNodeWriteLock( nodeId );
        return this.__ConstraintEnforcingEntityOperations.nodeSetProperty( state, nodeId, property );
    }

    @Override
    public Property nodeRemoveProperty( KernelStatement state, long nodeId, int propertyKeyId )
            throws EntityNotFoundException
    {
        state.locks().acquireNodeWriteLock( nodeId );
        return this.__StateHandlingStatementOperations.nodeRemoveProperty( state, nodeId, propertyKeyId );
    }

    @Override
    public Property relationshipSetProperty( KernelStatement state, long relationshipId, DefinedProperty property )
            throws EntityNotFoundException
    {
        state.locks().acquireRelationshipWriteLock( relationshipId );
        return this.__StateHandlingStatementOperations.relationshipSetProperty( state, relationshipId, property );
    }

    @Override
    public Property relationshipRemoveProperty( KernelStatement state, long relationshipId, int propertyKeyId )
            throws EntityNotFoundException
    {
        state.locks().acquireRelationshipWriteLock( relationshipId );
        return this.__StateHandlingStatementOperations.relationshipRemoveProperty( state, relationshipId, propertyKeyId );
    }

    @Override
    public Property graphSetProperty( KernelStatement state, DefinedProperty property )
    {
        state.locks().acquireGraphWriteLock();
        return this.__StateHandlingStatementOperations.graphSetProperty( state, property );
    }

    @Override
    public Property graphRemoveProperty( KernelStatement state, int propertyKeyId )
    {
        state.locks().acquireGraphWriteLock();
        return this.__StateHandlingStatementOperations.graphRemoveProperty( state, propertyKeyId );
    }

    public final java.lang.String propertyKeyGetName(
            org.neo4j.kernel.api.Statement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveLongIterator nodesGetFromIndexLookup(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1,
            java.lang.Object _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final boolean nodeHasLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int nodeGetDegree(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.graphdb.Direction _ignore2, int _ignore3) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator labelsGetAllTokens(
            org.neo4j.kernel.api.Statement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property nodeGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final long nodeGetUniqueFromIndexLookup(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1,
            java.lang.Object _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveIntIterator nodeGetRelationshipTypes(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int propertyKeyGetForName(
            org.neo4j.kernel.api.Statement _ignore0, java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int labelGetForName(org.neo4j.kernel.api.Statement _ignore0,
            java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property relationshipGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property graphGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveLongIterator nodesGetForLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveLongIterator nodeGetRelationships(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.graphdb.Direction _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator nodeGetAllProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveLongIterator nodeGetRelationships(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.graphdb.Direction _ignore2, int[] _ignore3) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.lang.String indexGetFailure(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveIntIterator nodeGetLabels(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int relationshipTypeGetForName(
            org.neo4j.kernel.api.Statement _ignore0, java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator graphGetAllProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveLongIterator graphGetPropertyKeys(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int relationshipTypeGetOrCreateForName(
            org.neo4j.kernel.api.Statement _ignore0, java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator propertyKeyGetAllTokens(
            org.neo4j.kernel.api.Statement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.lang.String relationshipTypeGetName(
            org.neo4j.kernel.api.Statement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int labelGetOrCreateForName(
            org.neo4j.kernel.api.Statement _ignore0, java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int propertyKeyGetOrCreateForName(
            org.neo4j.kernel.api.Statement _ignore0, java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.api.operations.StatementLayer delegate() {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.lang.String labelGetName(
            org.neo4j.kernel.api.Statement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator relationshipGetAllProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int nodeGetDegree(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.graphdb.Direction _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveLongIterator nodeGetPropertyKeys(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.util.PrimitiveLongIterator relationshipGetPropertyKeys(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }
}
