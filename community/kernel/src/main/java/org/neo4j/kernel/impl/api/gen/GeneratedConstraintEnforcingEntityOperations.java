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

import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.ConstraintValidationKernelException;
import org.neo4j.kernel.api.exceptions.schema.IndexBrokenKernelException;
import org.neo4j.kernel.api.exceptions.schema.UnableToValidateConstraintKernelException;
import org.neo4j.kernel.api.exceptions.schema.UniqueConstraintViolationKernelException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.api.properties.DefinedProperty;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.impl.api.KernelStatement;
import org.neo4j.kernel.impl.api.LockHolder;
import org.neo4j.kernel.impl.api.ReleasableLock;
import org.neo4j.kernel.impl.api.operations.StatementLayer;
import org.neo4j.kernel.impl.util.PrimitiveIntIterator;
import org.neo4j.kernel.impl.util.PrimitiveLongIterator;

import static org.neo4j.kernel.api.StatementConstants.NO_SUCH_NODE;

public final class GeneratedConstraintEnforcingEntityOperations implements StatementLayer
{
    public GeneratedConstraintEnforcingEntityOperations(
            org.neo4j.kernel.impl.api.StateHandlingStatementOperations __StateHandlingStatementOperations) {
        this.__StateHandlingStatementOperations = __StateHandlingStatementOperations;
    }

    private final org.neo4j.kernel.impl.api.operations.StatementLayer __StateHandlingStatementOperations;

    @Override
    public boolean nodeAddLabel( KernelStatement state, long nodeId, int labelId )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        Iterator<UniquenessConstraint> constraints = this.__StateHandlingStatementOperations.constraintsGetForLabel( state, labelId );
        while ( constraints.hasNext() )
        {
            UniquenessConstraint constraint = constraints.next();
            int propertyKeyId = constraint.propertyKeyId();
            Property property = this.__StateHandlingStatementOperations.nodeGetProperty( state, nodeId, propertyKeyId );
            if ( property.isDefined() )
            {
                validateNoExistingNodeWithLabelAndProperty( state, labelId, (DefinedProperty) property, nodeId );
            }
        }
        return this.__StateHandlingStatementOperations.nodeAddLabel( state, nodeId, labelId );
    }

    @Override
    public Property nodeSetProperty( KernelStatement state, long nodeId, DefinedProperty property )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        PrimitiveIntIterator labelIds = this.__StateHandlingStatementOperations.nodeGetLabels( state, nodeId );
        while ( labelIds.hasNext() )
        {
            int labelId = labelIds.next();
            int propertyKeyId = property.propertyKeyId();
            Iterator<UniquenessConstraint> constraintIterator =
                    this.__StateHandlingStatementOperations.constraintsGetForLabelAndPropertyKey( state, labelId, propertyKeyId );
            if ( constraintIterator.hasNext() )
            {
                validateNoExistingNodeWithLabelAndProperty( state, labelId, property, nodeId );
            }
        }
        return this.__StateHandlingStatementOperations.nodeSetProperty( state, nodeId, property );
    }

    private void validateNoExistingNodeWithLabelAndProperty( KernelStatement state, int labelId,
                                                             DefinedProperty property, long modifiedNode )
            throws ConstraintValidationKernelException
    {
        try
        {
            Object value = property.value();
            IndexDescriptor indexDescriptor = new IndexDescriptor( labelId, property.propertyKeyId() );
            assertIndexOnline( state, indexDescriptor );
            state.locks().acquireIndexEntryWriteLock( labelId, property.propertyKeyId(), property.valueAsString() );
            PrimitiveLongIterator existingNodes = delegate().nodesGetFromIndexLookup(
                    state, indexDescriptor, value );
            while ( existingNodes.hasNext() )
            {
                long existingNode = existingNodes.next();
                if ( existingNode != modifiedNode )
                {
                    throw new UniqueConstraintViolationKernelException( labelId, property.propertyKeyId(), value,
                            existingNode );
                }
            }
        }
        catch ( IndexNotFoundKernelException | IndexBrokenKernelException e )
        {
            throw new UnableToValidateConstraintKernelException( e );
        }
    }

    private void assertIndexOnline( KernelStatement state, IndexDescriptor indexDescriptor )
            throws IndexNotFoundKernelException, IndexBrokenKernelException
    {
        switch ( this.__None.indexGetState( state, indexDescriptor ) )
        {
            case ONLINE:
                return;
            default:
                throw new IndexBrokenKernelException( this.__None.indexGetFailure( state, indexDescriptor ) );
        }
    }

    @Override
    public long nodeGetUniqueFromIndexLookup(
            KernelStatement state,
            IndexDescriptor index,
            Object value )
            throws IndexNotFoundKernelException, IndexBrokenKernelException
    {
        assertIndexOnline( state, index );

        int labelId = index.getLabelId();
        int propertyKeyId = index.getPropertyKeyId();
        String stringVal = "";
        if ( null != value )
        {
            DefinedProperty property = Property.property( propertyKeyId, value );
            stringVal = property.valueAsString();
        }

        // If we find the node - hold a READ lock. If we don't find a node - hold a WRITE lock.
        LockHolder holder = state.locks();
        try ( ReleasableLock r = holder.getReleasableIndexEntryReadLock( labelId, propertyKeyId, stringVal ) )
        {
            long nodeId = delegate().nodeGetUniqueFromIndexLookup( state, index, value );
            if ( NO_SUCH_NODE == nodeId )
            {
                r.release(); // and change to a WRITE lock
                try ( ReleasableLock w = holder.getReleasableIndexEntryWriteLock( labelId, propertyKeyId, stringVal ) )
                {
                    nodeId = delegate().nodeGetUniqueFromIndexLookup( state, index, value );
                    if ( NO_SUCH_NODE != nodeId ) // we found it under the WRITE lock
                    { // downgrade to a READ lock
                        holder.getReleasableIndexEntryReadLock( labelId, propertyKeyId, stringVal )
                                .registerWithTransaction();
                        w.release();
                    }
                }
            }
            return nodeId;
        }
    }

    void indexDrop(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                   org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property relationshipGetCommittedProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.lang.String propertyKeyGetName(
            org.neo4j.kernel.api.Statement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveLongIterator nodesGetFromIndexLookup(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1,
            java.lang.Object _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    boolean nodeHasLabel(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                         long _ignore1, int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator constraintsGetAll(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator constraintsGetForLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.constraints.UniquenessConstraint uniquenessConstraintCreate(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property graphSetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.properties.DefinedProperty _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    void relationshipDelete(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                            long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    int nodeGetDegree(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                      long _ignore1, org.neo4j.graphdb.Direction _ignore2, int _ignore3) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator labelsGetAllTokens(
            org.neo4j.kernel.api.Statement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property nodeGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    long relationshipCreate(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                            int _ignore1, long _ignore2, long _ignore3) {
        throw new java.lang.UnsupportedOperationException();
    }

    void nodeDelete(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                    long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveIntIterator nodeGetRelationshipTypes(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    int propertyKeyGetForName(org.neo4j.kernel.api.Statement _ignore0,
                              java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    int labelGetForName(org.neo4j.kernel.api.Statement _ignore0,
                        java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    void constraintDrop(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                        org.neo4j.kernel.api.constraints.UniquenessConstraint _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property relationshipGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    boolean nodeRemoveLabel(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                            long _ignore1, int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property nodeRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property graphGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator indexesGetForLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveLongIterator nodesGetForLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveLongIterator nodeGetRelationships(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.graphdb.Direction _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator nodeGetAllProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator uniqueIndexesGetAll(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveLongIterator nodeGetRelationships(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.graphdb.Direction _ignore2, int[] _ignore3) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator relationshipGetAllCommittedProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property graphRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.index.InternalIndexState indexGetState(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    long nodeCreate(org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property relationshipRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveIntIterator nodeGetCommittedLabels(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveIntIterator nodeGetLabels(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    int relationshipTypeGetForName(org.neo4j.kernel.api.Statement _ignore0,
                                   java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator graphGetAllProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveLongIterator graphGetPropertyKeys(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    int relationshipTypeGetOrCreateForName(
            org.neo4j.kernel.api.Statement _ignore0, java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.lang.Long indexGetOwningUniquenessConstraintId(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator propertyKeyGetAllTokens(
            org.neo4j.kernel.api.Statement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.lang.String relationshipTypeGetName(
            org.neo4j.kernel.api.Statement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property nodeGetCommittedProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    int labelGetOrCreateForName(org.neo4j.kernel.api.Statement _ignore0,
                                java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator indexesGetAll(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    int propertyKeyGetOrCreateForName(org.neo4j.kernel.api.Statement _ignore0,
                                      java.lang.String _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.properties.Property relationshipSetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.kernel.api.properties.DefinedProperty _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.index.IndexDescriptor indexCreate(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.api.operations.StatementLayer delegate() {
        throw new java.lang.UnsupportedOperationException();
    }

    java.lang.String labelGetName(org.neo4j.kernel.api.Statement _ignore0,
                                  int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    long indexGetCommittedId(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1,
            org.neo4j.kernel.impl.nioneo.store.SchemaStorage.IndexRuleKind _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator relationshipGetAllProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator uniqueIndexesGetForLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator nodeGetAllCommittedProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.util.Iterator constraintsGetForLabelAndPropertyKey(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    void uniqueIndexDrop(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                         org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.api.index.IndexDescriptor indexesGetForLabelAndPropertyKey(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    int nodeGetDegree(org.neo4j.kernel.impl.api.KernelStatement _ignore0,
                      long _ignore1, org.neo4j.graphdb.Direction _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    java.lang.String indexGetFailure(org.neo4j.kernel.api.Statement _ignore0,
                                     org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveLongIterator nodeGetPropertyKeys(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    org.neo4j.kernel.impl.util.PrimitiveLongIterator relationshipGetPropertyKeys(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }
}