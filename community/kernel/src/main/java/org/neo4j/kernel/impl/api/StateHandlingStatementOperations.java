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
import java.util.Set;

import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.PropertyKeyIdNotFoundException;
import org.neo4j.kernel.api.exceptions.PropertyNotFoundException;
import org.neo4j.kernel.api.exceptions.TransactionalException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.ConstraintCreationKernelException;
import org.neo4j.kernel.api.exceptions.schema.DropIndexFailureException;
import org.neo4j.kernel.api.exceptions.schema.SchemaKernelException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.api.operations.EntityReadOperations;
import org.neo4j.kernel.api.operations.EntityWriteOperations;
import org.neo4j.kernel.api.operations.SchemaReadOperations;
import org.neo4j.kernel.api.operations.SchemaWriteOperations;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.api.properties.PropertyKeyIdIterator;
import org.neo4j.kernel.impl.api.constraints.ConstraintIndexCreator;
import org.neo4j.kernel.impl.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.api.state.TxState;

import static java.util.Collections.emptyList;

import static org.neo4j.helpers.collection.Iterables.option;
import static org.neo4j.helpers.collection.IteratorUtil.singleOrNull;
import static org.neo4j.helpers.collection.IteratorUtil.toPrimitiveLongIterator;

public class StateHandlingStatementOperations implements
    EntityReadOperations,
    EntityWriteOperations,
    SchemaReadOperations,
    SchemaWriteOperations
{
    private final EntityReadOperations entityReadDelegate;
    private final SchemaReadOperations schemaReadDelegate;
    private final AuxiliaryStoreOperations auxStoreOps;
    private final ConstraintIndexCreator constraintIndexCreator;
    private final TxState transactionState;

    public StateHandlingStatementOperations(
            TxState transactionState,
            EntityReadOperations entityReadDelegate,
            SchemaReadOperations schemaReadDelegate,
            AuxiliaryStoreOperations auxStoreOps, ConstraintIndexCreator constraintIndexCreator )
    {
        this.transactionState = transactionState;
        this.entityReadDelegate = entityReadDelegate;
        this.schemaReadDelegate = schemaReadDelegate;
        this.auxStoreOps = auxStoreOps;
        this.constraintIndexCreator = constraintIndexCreator;
    }

    @Override
    public void nodeDelete( long nodeId )
    {
        auxStoreOps.nodeDelete( nodeId );
        transactionState.nodeDoDelete( nodeId );
    }

    @Override
    public void relationshipDelete( long relationshipId )
    {
        auxStoreOps.relationshipDelete( relationshipId );
        transactionState.relationshipDoDelete( relationshipId );
    }

    @Override
    public boolean nodeHasLabel( long nodeId, long labelId ) throws EntityNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            if ( transactionState.nodeIsDeletedInThisTx( nodeId ) )
            {
                return false;
            }

            if ( transactionState.nodeIsAddedInThisTx( nodeId ) )
            {
                TxState.UpdateTriState labelState = transactionState.labelState( nodeId, labelId );
                return labelState.isTouched() && labelState.isAdded();
            }

            TxState.UpdateTriState labelState = transactionState.labelState( nodeId, labelId );
            if ( labelState.isTouched() )
            {
                return labelState.isAdded();
            }
        }

        return entityReadDelegate.nodeHasLabel( nodeId, labelId );
    }

    @Override
    public PrimitiveLongIterator nodeGetLabels( long nodeId ) throws EntityNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            if ( transactionState.nodeIsDeletedInThisTx( nodeId ) )
            {
                return IteratorUtil.emptyPrimitiveLongIterator();
            }

            if ( transactionState.nodeIsAddedInThisTx( nodeId ) )
            {
                return
                    toPrimitiveLongIterator( transactionState.nodeStateLabelDiffSets( nodeId ).getAdded().iterator() );
            }

            return transactionState.nodeStateLabelDiffSets( nodeId ).applyPrimitiveLongIterator(
                    entityReadDelegate.nodeGetLabels( nodeId ) );
        }

        return entityReadDelegate.nodeGetLabels( nodeId );
    }

    @Override
    public boolean nodeAddLabel( long nodeId, long labelId ) throws EntityNotFoundException
    {
        if ( nodeHasLabel( nodeId, labelId ) )
        {
            // Label is already in state or in store, no-op
            return false;
        }

        transactionState.nodeDoAddLabel( labelId, nodeId );
        return true;
    }

    @Override
    public boolean nodeRemoveLabel( long nodeId, long labelId ) throws EntityNotFoundException
    {
        if ( !nodeHasLabel( nodeId, labelId ) )
        {
            // Label does not exist in state nor in store, no-op
            return false;
        }

        transactionState.nodeDoRemoveLabel( labelId, nodeId );

        return true;
    }

    @Override
    public PrimitiveLongIterator nodesGetForLabel( long labelId )
    {
        if ( transactionState.hasChanges() )
        {
            PrimitiveLongIterator wLabelChanges =
                    transactionState.nodesWithLabelChanged( labelId ).applyPrimitiveLongIterator(
                            entityReadDelegate.nodesGetForLabel( labelId ) );
            return transactionState.nodesDeletedInTx().applyPrimitiveLongIterator( wLabelChanges );
        }

        return entityReadDelegate.nodesGetForLabel( labelId );
    }

    @Override
    public IndexDescriptor indexCreate( long labelId, long propertyKey )
            throws SchemaKernelException
    {
        IndexDescriptor rule = new IndexDescriptor( labelId, propertyKey );
        transactionState.indexRuleDoAdd( rule );
        return rule;
    }

    @Override
    public IndexDescriptor uniqueIndexCreate( long labelId, long propertyKey )
            throws SchemaKernelException
    {
        IndexDescriptor rule = new IndexDescriptor( labelId, propertyKey );
        transactionState.constraintIndexRuleDoAdd( rule );
        return rule;
    }

    @Override
    public void indexDrop( IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        transactionState.indexDoDrop( descriptor );
    }

    @Override
    public void uniqueIndexDrop( IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        transactionState.constraintIndexDoDrop( descriptor );
    }

    @Override
    public UniquenessConstraint uniquenessConstraintCreate( long labelId, long propertyKeyId )
            throws SchemaKernelException
    {
        UniquenessConstraint constraint = new UniquenessConstraint( labelId, propertyKeyId );
        if ( !transactionState.constraintDoUnRemove( constraint ) )
        {
            for ( Iterator<UniquenessConstraint> it = schemaReadDelegate.constraintsGetForLabelAndPropertyKey(
                    labelId, propertyKeyId ); it.hasNext(); )
            {
                if ( it.next().equals( labelId, propertyKeyId ) )
                {
                    return constraint;
                }
            }
            
            try
            {
                long indexId = constraintIndexCreator.createUniquenessConstraintIndex(
                        this, labelId, propertyKeyId );
                transactionState.constraintDoAdd( constraint, indexId );
            }
            catch ( TransactionalException | KernelException e )
            {
                throw new ConstraintCreationKernelException( constraint, e );
            }
        }
        return constraint;
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabelAndPropertyKey( long labelId, long propertyKeyId )
    {
        return applyConstraintsDiff( schemaReadDelegate.constraintsGetForLabelAndPropertyKey(
                labelId, propertyKeyId ), labelId, propertyKeyId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabel( long labelId )
    {
        return applyConstraintsDiff( schemaReadDelegate.constraintsGetForLabel( labelId ), labelId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetAll()
    {
        return applyConstraintsDiff( schemaReadDelegate.constraintsGetAll() );
    }

    private Iterator<UniquenessConstraint> applyConstraintsDiff(
            Iterator<UniquenessConstraint> constraints, long labelId, long propertyKeyId )
    {
        if ( transactionState.hasChanges() )
        {
            DiffSets<UniquenessConstraint> diff =
                    transactionState.constraintsChangesForLabelAndProperty( labelId, propertyKeyId );
            if ( diff != null )
            {
                return diff.apply( constraints );
            }
        }

        return constraints;
    }

    private Iterator<UniquenessConstraint> applyConstraintsDiff(
            Iterator<UniquenessConstraint> constraints, long labelId )
    {
        if ( transactionState.hasChanges() )
        {
            DiffSets<UniquenessConstraint> diff = transactionState.constraintsChangesForLabel( labelId );
            if ( diff != null )
            {
                return diff.apply( constraints );
            }
        }

        return constraints;
    }

    private Iterator<UniquenessConstraint> applyConstraintsDiff( Iterator<UniquenessConstraint> constraints )
    {
        if ( transactionState.hasChanges() )
        {
            DiffSets<UniquenessConstraint> diff = transactionState.constraintsChanges();
            if ( diff != null )
            {
                return diff.apply( constraints );
            }
        }

        return constraints;
    }

    @Override
    public void constraintDrop( UniquenessConstraint constraint )
    {
        transactionState.constraintDoDrop( constraint );
    }

    @Override
    public IndexDescriptor indexesGetForLabelAndPropertyKey( long labelId, long propertyKey ) throws SchemaRuleNotFoundException
    {
        Iterable<IndexDescriptor> committedRules;
        try
        {
            committedRules = option( schemaReadDelegate.indexesGetForLabelAndPropertyKey( labelId, propertyKey ) );
        }
        catch ( SchemaRuleNotFoundException e )
        {
            committedRules = emptyList();
        }
        DiffSets<IndexDescriptor> ruleDiffSet = transactionState.indexDiffSetsByLabel( labelId );

        Iterator<IndexDescriptor> rules =
            transactionState.hasChanges() ? ruleDiffSet.apply( committedRules.iterator() ) : committedRules.iterator();
        IndexDescriptor single = singleOrNull( rules );
        if ( single == null )
        {
            throw new SchemaRuleNotFoundException( "Index rule for label:" + labelId + " and property:" +
                                                   propertyKey + " not found" );
        }
        return single;
    }

    @Override
    public InternalIndexState indexGetState( IndexDescriptor descriptor ) throws IndexNotFoundKernelException
    {
        // If index is in our then return populating
        if ( transactionState.hasChanges() )
        {
            if ( checkIndexState( descriptor, transactionState.indexDiffSetsByLabel( descriptor.getLabelId() ) ) )
            {
                return InternalIndexState.POPULATING;
            }
            if ( checkIndexState( descriptor, transactionState.constraintIndexDiffSetsByLabel( descriptor.getLabelId() ) ) )
            {
                return InternalIndexState.POPULATING;
            }
        }

        return schemaReadDelegate.indexGetState( descriptor );
    }

    private boolean checkIndexState( IndexDescriptor indexRule, DiffSets<IndexDescriptor> diffSet )
            throws IndexNotFoundKernelException
    {
        if ( diffSet.isAdded( indexRule ) )
        {
            return true;
        }
        if ( diffSet.isRemoved( indexRule ) )
        {
            throw new IndexNotFoundKernelException( String.format( "Index for label id %d on property id %d has been " +
                                                                   "dropped in this transaction.",
                                                                   indexRule.getLabelId(),
                                                                   indexRule.getPropertyKeyId() ) );
        }
        return false;
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetForLabel( long labelId )
    {
        if ( transactionState.hasChanges() )
        {
            return transactionState.indexDiffSetsByLabel( labelId )
                    .apply( schemaReadDelegate.indexesGetForLabel( labelId ) );
        }

        return schemaReadDelegate.indexesGetForLabel( labelId );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetAll()
    {
        if ( transactionState.hasChanges() )
        {
            return transactionState.indexChanges().apply( schemaReadDelegate.indexesGetAll() );
        }

        return schemaReadDelegate.indexesGetAll();
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetForLabel( long labelId )
    {
        if ( transactionState.hasChanges() )
        {
            return transactionState.constraintIndexDiffSetsByLabel( labelId )
                    .apply( schemaReadDelegate.uniqueIndexesGetForLabel( labelId ) );
        }

        return schemaReadDelegate.uniqueIndexesGetForLabel( labelId );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetAll( )
    {
        if ( transactionState.hasChanges() )
        {
            return transactionState.constraintIndexChanges()
                    .apply( schemaReadDelegate.uniqueIndexesGetAll( ) );
        }

        return schemaReadDelegate.uniqueIndexesGetAll( );
    }

    @Override
    public PrimitiveLongIterator nodesGetFromIndexLookup( IndexDescriptor index, final Object value )
            throws IndexNotFoundKernelException
    {
        if ( transactionState.hasChanges() )
        {
            // Start with nodes where the given property has changed
            DiffSets<Long> diff = transactionState.nodesWithChangedProperty( index.getPropertyKeyId(), value );

            // Ensure remaining nodes have the correct label
            diff = diff.filterAdded( new HasLabelFilter( index.getLabelId() ) );

            // Include newly labeled nodes that already had the correct property
            HasPropertyFilter hasPropertyFilter = new HasPropertyFilter( index.getPropertyKeyId(), value );
            Iterator<Long> addedNodesWithLabel = transactionState.nodesWithLabelAdded( index.getLabelId() ).iterator();
            diff.addAll( Iterables.filter( hasPropertyFilter, addedNodesWithLabel ) );

            // Remove de-labeled nodes that had the correct value before
            Set<Long> removedNodesWithLabel = transactionState.nodesWithLabelChanged( index.getLabelId() ).getRemoved();
            diff.removeAll( Iterables.filter( hasPropertyFilter, removedNodesWithLabel.iterator() ) );

            // Apply to actual index lookup
            PrimitiveLongIterator committed = entityReadDelegate.nodesGetFromIndexLookup( index, value );
            return transactionState
                    .nodesDeletedInTx().applyPrimitiveLongIterator( diff.applyPrimitiveLongIterator( committed ) );
        }

        return entityReadDelegate.nodesGetFromIndexLookup( index, value );
    }

    @Override
    public Property nodeSetProperty( long nodeId, Property property )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        try
        {
            Property existingProperty = nodeGetProperty( nodeId, property.propertyKeyId() );
            if ( existingProperty.isNoProperty() )
            {
                auxStoreOps.nodeAddStoreProperty( nodeId, property );
            }
            else
            {
                auxStoreOps.nodeChangeStoreProperty( nodeId, existingProperty, property );
            }
            transactionState.nodeDoReplaceProperty( nodeId, existingProperty, property );
            return existingProperty;
        }
        catch ( PropertyNotFoundException e )
        {
            throw new IllegalArgumentException( "Property used for setting should not be NoProperty", e );
        }
    }

    @Override
    public Property relationshipSetProperty( long relationshipId, Property property )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        try
        {
            Property existingProperty = relationshipGetProperty( relationshipId, property.propertyKeyId() );
            if ( existingProperty.isNoProperty() )
            {
                auxStoreOps.relationshipAddStoreProperty( relationshipId, property );
            }
            else
            {
                auxStoreOps.relationshipChangeStoreProperty( relationshipId, existingProperty, property );
            }
            transactionState.relationshipDoReplaceProperty( relationshipId, existingProperty, property );
            return existingProperty;
        }
        catch ( PropertyNotFoundException e )
        {
            throw new IllegalArgumentException( "Property used for setting should not be NoProperty", e );
        }
    }
    
    @Override
    public Property graphSetProperty( Property property ) throws PropertyKeyIdNotFoundException
    {
        try
        {
            Property existingProperty = graphGetProperty( property.propertyKeyId() );
            if ( existingProperty.isNoProperty() )
            {
                auxStoreOps.graphAddStoreProperty( property );
            }
            else
            {
                auxStoreOps.graphChangeStoreProperty( existingProperty, property );
            }
            transactionState.graphDoReplaceProperty( existingProperty, property );
            return existingProperty;
        }
        catch ( PropertyNotFoundException e )
        {
            throw new IllegalArgumentException( "Property used for setting should not be NoProperty", e );
        }
    }

    @Override
    public Property nodeRemoveProperty( long nodeId, long propertyKeyId )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        try
        {
            Property existingProperty = nodeGetProperty( nodeId, propertyKeyId );
            if ( !existingProperty.isNoProperty() )
            {
                auxStoreOps.nodeRemoveStoreProperty( nodeId, existingProperty );
            }
            transactionState.nodeDoRemoveProperty( nodeId, existingProperty );
            return existingProperty;
        }
        catch ( PropertyNotFoundException e )
        {
            throw new IllegalArgumentException( "Property used for setting should not be NoProperty", e );
        }
    }

    @Override
    public Property relationshipRemoveProperty( long relationshipId, long propertyKeyId )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        try
        {
            Property existingProperty = relationshipGetProperty( relationshipId, propertyKeyId );
            if ( !existingProperty.isNoProperty() )
            {
                auxStoreOps.relationshipRemoveStoreProperty( relationshipId, existingProperty );
            }
            transactionState.relationshipDoRemoveProperty( relationshipId, existingProperty );
            return existingProperty;
        }
        catch ( PropertyNotFoundException e )
        {
            throw new IllegalArgumentException( "Property used for setting should not be NoProperty", e );
        }
    }
    
    @Override
    public Property graphRemoveProperty( long propertyKeyId )
            throws PropertyKeyIdNotFoundException
    {
        try
        {
            Property existingProperty = graphGetProperty( propertyKeyId );
            if ( !existingProperty.isNoProperty() )
            {
                auxStoreOps.graphRemoveStoreProperty( existingProperty );
            }
            transactionState.graphDoRemoveProperty( existingProperty );
            return existingProperty;
        }
        catch ( PropertyNotFoundException e )
        {
            throw new IllegalArgumentException( "Property used for setting should not be NoProperty", e );
        }
    }
    
    @Override
    public PrimitiveLongIterator nodeGetPropertyKeys( long nodeId ) throws EntityNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            return new PropertyKeyIdIterator( nodeGetAllProperties( nodeId ) );
        }
        
        return entityReadDelegate.nodeGetPropertyKeys( nodeId );
    }
    
    @Override
    public Property nodeGetProperty( long nodeId, long propertyKeyId )
            throws EntityNotFoundException, PropertyKeyIdNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            Iterator<Property> properties = nodeGetAllProperties( nodeId );
            while ( properties.hasNext() )
            {
                Property property = properties.next();
                if ( property.propertyKeyId() == propertyKeyId )
                {
                    return property;
                }
            }
            return Property.noNodeProperty( nodeId, propertyKeyId );
        }
        
        return entityReadDelegate.nodeGetProperty( nodeId, propertyKeyId );
    }
    
    @Override
    public boolean nodeHasProperty( long nodeId, long propertyKeyId )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        return !nodeGetProperty( nodeId, propertyKeyId ).isNoProperty();
    }

    @Override
    public Iterator<Property> nodeGetAllProperties( long nodeId ) throws EntityNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            if ( transactionState.nodeIsAddedInThisTx( nodeId ) )
            {
                return transactionState.nodePropertyDiffSets( nodeId ).getAdded().iterator();
            }
            if ( transactionState.nodeIsDeletedInThisTx( nodeId ) )
            {
                // TODO Throw IllegalStateException to conform with beans API. We may want to introduce
                // EntityDeletedException instead and use it instead of returning empty values in similar places
                throw new IllegalStateException( "Node " + nodeId + " has been deleted" );
            }
            return transactionState.nodePropertyDiffSets( nodeId )
                    .apply( entityReadDelegate.nodeGetAllProperties( nodeId ) );
        }

        return entityReadDelegate.nodeGetAllProperties( nodeId );
    }
    
    @Override
    public PrimitiveLongIterator relationshipGetPropertyKeys( long relationshipId )
            throws EntityNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            return new PropertyKeyIdIterator( relationshipGetAllProperties( relationshipId ) );
        }
        
        return entityReadDelegate.relationshipGetPropertyKeys( relationshipId );
    }
    
    @Override
    public Property relationshipGetProperty( long relationshipId, long propertyKeyId )
            throws EntityNotFoundException, PropertyKeyIdNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            Iterator<Property> properties = relationshipGetAllProperties( relationshipId );
            while ( properties.hasNext() )
            {
                Property property = properties.next();
                if ( property.propertyKeyId() == propertyKeyId )
                {
                    return property;
                }
            }
            return Property.noRelationshipProperty( relationshipId, propertyKeyId );
        }
        return entityReadDelegate.relationshipGetProperty( relationshipId, propertyKeyId );
    }
    
    @Override
    public boolean relationshipHasProperty( long relationshipId, long propertyKeyId )
            throws PropertyKeyIdNotFoundException, EntityNotFoundException
    {
        return !relationshipGetProperty( relationshipId, propertyKeyId ).isNoProperty();
    }

    @Override
    public Iterator<Property> relationshipGetAllProperties( long relationshipId ) throws EntityNotFoundException
    {
        if ( transactionState.hasChanges() )
        {
            if ( transactionState.relationshipIsAddedInThisTx( relationshipId ) )
            {
                return transactionState.relationshipPropertyDiffSets( relationshipId ).getAdded().iterator();
            }
            if ( transactionState.relationshipIsDeletedInThisTx( relationshipId ) )
            {
                // TODO Throw IllegalStateException to conform with beans API. We may want to introduce
                // EntityDeletedException instead and use it instead of returning empty values in similar places
                throw new IllegalStateException( "Relationship " + relationshipId + " has been deleted" );
            }
            return transactionState.relationshipPropertyDiffSets( relationshipId )
                    .apply( entityReadDelegate.relationshipGetAllProperties( relationshipId ) );
        }
        else
        {
            return entityReadDelegate.relationshipGetAllProperties( relationshipId );
        }
    }
    
    @Override
    public PrimitiveLongIterator graphGetPropertyKeys()
    {
        if ( transactionState.hasChanges() )
        {
            return new PropertyKeyIdIterator( graphGetAllProperties() );
        }
        
        return entityReadDelegate.graphGetPropertyKeys();
    }
    
    @Override
    public Property graphGetProperty( long propertyKeyId )
    {
        Iterator<Property> properties = graphGetAllProperties( );
        while ( properties.hasNext() )
        {
            Property property = properties.next();
            if ( property.propertyKeyId() == propertyKeyId )
            {
                return property;
            }
        }
        return Property.noGraphProperty( propertyKeyId );
    }
    
    @Override
    public boolean graphHasProperty( long propertyKeyId ) throws PropertyKeyIdNotFoundException
    {
        return !graphGetProperty( propertyKeyId ).isNoProperty();
    }
    
    @Override
    public Iterator<Property> graphGetAllProperties()
    {
        if ( transactionState.hasChanges() )
        {
            return transactionState.graphPropertyDiffSets().apply( entityReadDelegate.graphGetAllProperties() );
        }

        return entityReadDelegate.graphGetAllProperties();
    }

    private class HasPropertyFilter implements Predicate<Long>
    {
        private final Object value;
        private final long propertyKeyId;

        public HasPropertyFilter( long propertyKeyId, Object value )
        {
            this.value = value;
            this.propertyKeyId = propertyKeyId;
        }

        @Override
        public boolean accept( Long nodeId )
        {
            try
            {
                if ( transactionState.hasChanges() && transactionState.nodeIsDeletedInThisTx( nodeId ) )
                {
                    return false;
                }
                Property property = nodeGetProperty( nodeId, propertyKeyId );
                return !property.isNoProperty() && property.valueEquals( value );
            }
            catch ( EntityNotFoundException | PropertyKeyIdNotFoundException e )
            {
                return false;
            }
        }
    }

    private class HasLabelFilter implements Predicate<Long>
    {
        private final long labelId;

        public HasLabelFilter( long labelId )
        {
            this.labelId = labelId;
        }

        @Override
        public boolean accept( Long nodeId )
        {
            try
            {
                return nodeHasLabel( nodeId, labelId );
            }
            catch ( EntityNotFoundException e )
            {
                return false;
            }
        }
    }
    
    // === TODO Below is unnecessary delegate methods

    @Override
    public Long indexGetOwningUniquenessConstraintId( IndexDescriptor index )
            throws SchemaRuleNotFoundException
    {
        return schemaReadDelegate.indexGetOwningUniquenessConstraintId( index );
    }

    @Override
    public long indexGetCommittedId( IndexDescriptor index )
            throws SchemaRuleNotFoundException
    {
        return schemaReadDelegate.indexGetCommittedId( index );
    }
    
    @Override
    public String indexGetFailure( IndexDescriptor descriptor )
            throws IndexNotFoundKernelException
    {
        return schemaReadDelegate.indexGetFailure( descriptor );
    }
}
