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
package org.neo4j.kernel.impl.api;

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
import org.neo4j.kernel.impl.api.operations.StatementLayer;
import org.neo4j.kernel.impl.util.PrimitiveIntIterator;
import org.neo4j.kernel.impl.util.PrimitiveLongIterator;

import static org.neo4j.kernel.api.StatementConstants.NO_SUCH_NODE;

public abstract class ConstraintEnforcingEntityOperations implements StatementLayer
{
    @Override
    public boolean nodeAddLabel( KernelStatement state, long nodeId, int labelId )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        Iterator<UniquenessConstraint> constraints = delegate().constraintsGetForLabel( state, labelId );
        while ( constraints.hasNext() )
        {
            UniquenessConstraint constraint = constraints.next();
            int propertyKeyId = constraint.propertyKeyId();
            Property property = delegate().nodeGetProperty( state, nodeId, propertyKeyId );
            if ( property.isDefined() )
            {
                validateNoExistingNodeWithLabelAndProperty( state, labelId, (DefinedProperty) property, nodeId );
            }
        }
        return delegate().nodeAddLabel( state, nodeId, labelId );
    }

    @Override
    public Property nodeSetProperty( KernelStatement state, long nodeId, DefinedProperty property )
            throws EntityNotFoundException, ConstraintValidationKernelException
    {
        PrimitiveIntIterator labelIds = delegate().nodeGetLabels( state, nodeId );
        while ( labelIds.hasNext() )
        {
            int labelId = labelIds.next();
            int propertyKeyId = property.propertyKeyId();
            Iterator<UniquenessConstraint> constraintIterator =
                    delegate().constraintsGetForLabelAndPropertyKey( state, labelId, propertyKeyId );
            if ( constraintIterator.hasNext() )
            {
                validateNoExistingNodeWithLabelAndProperty( state, labelId, property, nodeId );
            }
        }
        return delegate().nodeSetProperty( state, nodeId, property );
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
        switch ( delegate().indexGetState( state, indexDescriptor ) )
        {
            case ONLINE:
                return;
            default:
                throw new IndexBrokenKernelException( delegate().indexGetFailure( state, indexDescriptor ) );
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
}
