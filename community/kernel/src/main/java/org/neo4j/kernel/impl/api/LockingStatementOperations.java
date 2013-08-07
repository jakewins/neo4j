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
package org.neo4j.kernel.impl.api;

import java.util.Iterator;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.PropertyKeyIdNotFoundException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.DropIndexFailureException;
import org.neo4j.kernel.api.exceptions.schema.SchemaKernelException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.api.operations.EntityWriteOperations;
import org.neo4j.kernel.api.operations.SchemaReadOperations;
import org.neo4j.kernel.api.operations.SchemaStateOperations;
import org.neo4j.kernel.api.operations.SchemaWriteOperations;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.impl.api.index.IndexDescriptor;

public class LockingStatementOperations implements
    EntityWriteOperations,
    SchemaReadOperations,
    SchemaWriteOperations,
    SchemaStateOperations
{
    private final EntityWriteOperations entityWriteDelegate;
    private final SchemaReadOperations schemaReadDelegate;
    private final SchemaWriteOperations schemaWriteDelegate;
    private final SchemaStateOperations schemaStateDelegate;
    private final LockHolder locks;

    public LockingStatementOperations( LockHolder locks, SchemaReadOperations schemaReadDelegate,
                                 SchemaWriteOperations schemaWriteDelegate, SchemaStateOperations schemaStateDelegate,
                                 EntityWriteOperations entityWriteDelegate )
    {
        this.locks = locks;
        this.entityWriteDelegate = entityWriteDelegate;
        this.schemaReadDelegate = schemaReadDelegate;
        this.schemaWriteDelegate = schemaWriteDelegate;
        this.schemaStateDelegate = schemaStateDelegate;
    }

    @Override
    public boolean nodeAddLabel( long nodeId, long labelId ) throws EntityNotFoundException
    {
        locks.acquireNodeWriteLock( nodeId );
        return entityWriteDelegate.nodeAddLabel( nodeId, labelId );
    }

    @Override
    public boolean nodeRemoveLabel( long nodeId, long labelId ) throws EntityNotFoundException
    {
        locks.acquireNodeWriteLock( nodeId );
        return entityWriteDelegate.nodeRemoveLabel( nodeId, labelId );
    }

    @Override
    public IndexDescriptor indexCreate( long labelId, long propertyKey ) throws SchemaKernelException
    {
        locks.acquireSchemaWriteLock();
        return schemaWriteDelegate.indexCreate( labelId, propertyKey );
    }

    @Override
    public IndexDescriptor uniqueIndexCreate( long labelId, long propertyKey ) throws SchemaKernelException
    {
        locks.acquireSchemaWriteLock();
        return schemaWriteDelegate.uniqueIndexCreate( labelId, propertyKey );
    }

    @Override
    public void indexDrop( IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        locks.acquireSchemaWriteLock();
        schemaWriteDelegate.indexDrop( descriptor );
    }

    @Override
    public void uniqueIndexDrop( IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        locks.acquireSchemaWriteLock();
        schemaWriteDelegate.uniqueIndexDrop( descriptor );
    }

    @Override
    public <K, V> V schemaStateGetOrCreate( K key, Function<K, V> creator )
    {
        locks.acquireSchemaReadLock();
        return schemaStateDelegate.schemaStateGetOrCreate( key, creator );
    }

    @Override
    public <K> boolean schemaStateContains( K key )
    {
        locks.acquireSchemaReadLock();
        return schemaStateDelegate.schemaStateContains( key );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetForLabel( long labelId )
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.indexesGetForLabel( labelId );
    }
    
    @Override
    public IndexDescriptor indexesGetForLabelAndPropertyKey( long labelId, long propertyKey )
            throws SchemaRuleNotFoundException
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.indexesGetForLabelAndPropertyKey( labelId, propertyKey );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetAll()
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.indexesGetAll();
    }
    
    @Override
    public InternalIndexState indexGetState( IndexDescriptor descriptor ) throws IndexNotFoundKernelException
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.indexGetState( descriptor );
    }

    @Override
    public Long indexGetOwningUniquenessConstraintId( IndexDescriptor index ) throws SchemaRuleNotFoundException
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.indexGetOwningUniquenessConstraintId( index );
    }

    @Override
    public long indexGetCommittedId( IndexDescriptor index ) throws SchemaRuleNotFoundException
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.indexGetCommittedId( index );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetForLabel( long labelId )
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.uniqueIndexesGetForLabel( labelId );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetAll( )
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.uniqueIndexesGetAll( );
    }

    @Override
    public void nodeDelete( long nodeId )
    {
        locks.acquireNodeWriteLock( nodeId );
        entityWriteDelegate.nodeDelete( nodeId );
    }
    
    @Override
    public void relationshipDelete( long relationshipId )
    {
        locks.acquireRelationshipWriteLock( relationshipId );
        entityWriteDelegate.relationshipDelete( relationshipId );
    }

    @Override
    public UniquenessConstraint uniquenessConstraintCreate( long labelId, long propertyKeyId )
            throws SchemaKernelException
    {
        locks.acquireSchemaWriteLock();
        return schemaWriteDelegate.uniquenessConstraintCreate( labelId, propertyKeyId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabelAndPropertyKey( long labelId, long propertyKeyId )
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.constraintsGetForLabelAndPropertyKey( labelId, propertyKeyId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabel( long labelId )
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.constraintsGetForLabel( labelId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetAll()
    {
        locks.acquireSchemaReadLock();
        return schemaReadDelegate.constraintsGetAll();
    }

    @Override
    public void constraintDrop( UniquenessConstraint constraint )
    {
        locks.acquireSchemaWriteLock();
        schemaWriteDelegate.constraintDrop( constraint );
    }
    
    @Override
    public Property nodeSetProperty( long nodeId, Property property ) throws PropertyKeyIdNotFoundException,
            EntityNotFoundException
    {
        locks.acquireNodeWriteLock( nodeId );
        return entityWriteDelegate.nodeSetProperty( nodeId, property );
    }
    
    @Override
    public Property nodeRemoveProperty( long nodeId, long propertyKeyId ) throws PropertyKeyIdNotFoundException,
            EntityNotFoundException
    {
        locks.acquireNodeWriteLock( nodeId );
        return entityWriteDelegate.nodeRemoveProperty( nodeId, propertyKeyId );
    }
    
    @Override
    public Property relationshipSetProperty( long relationshipId, Property property )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        locks.acquireRelationshipWriteLock( relationshipId );
        return entityWriteDelegate.relationshipSetProperty( relationshipId, property );
    }
    
    @Override
    public Property relationshipRemoveProperty( long relationshipId, long propertyKeyId )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        locks.acquireRelationshipWriteLock( relationshipId );
        return entityWriteDelegate.relationshipRemoveProperty( relationshipId, propertyKeyId );
    }
    
    @Override
    public Property graphSetProperty( Property property ) throws PropertyKeyIdNotFoundException
    {
        locks.acquireGraphWriteLock();
        return entityWriteDelegate.graphSetProperty( property );
    }
    
    @Override
    public Property graphRemoveProperty( long propertyKeyId ) throws PropertyKeyIdNotFoundException
    {
        locks.acquireGraphWriteLock();
        return entityWriteDelegate.graphRemoveProperty( propertyKeyId );
    }
    
    // === TODO Below is unnecessary delegate methods
    @Override
    public String indexGetFailure( IndexDescriptor descriptor ) throws IndexNotFoundKernelException
    {
        return schemaReadDelegate.indexGetFailure( descriptor );
    }
}
