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

import org.neo4j.graphdb.Direction;
import org.neo4j.helpers.Function;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.LabelNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.PropertyKeyIdNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.RelationshipTypeIdNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.IndexBrokenKernelException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.exceptions.schema.TooManyLabelsException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.api.properties.DefinedProperty;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.api.properties.PropertyKeyIdIterator;
import org.neo4j.kernel.impl.api.KernelStatement;
import org.neo4j.kernel.impl.api.index.IndexingService;
import org.neo4j.kernel.impl.api.operations.StatementLayer;
import org.neo4j.kernel.impl.api.store.CacheLoader;
import org.neo4j.kernel.impl.api.store.DiskLayer;
import org.neo4j.kernel.impl.api.store.PersistenceCache;
import org.neo4j.kernel.impl.api.store.SchemaCache;
import org.neo4j.kernel.impl.api.store.StoreReadLayer;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.core.RelationshipImpl;
import org.neo4j.kernel.impl.core.Token;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;
import org.neo4j.kernel.impl.nioneo.store.SchemaRule;
import org.neo4j.kernel.impl.nioneo.store.SchemaStorage;
import org.neo4j.kernel.impl.util.PrimitiveIntIterator;
import org.neo4j.kernel.impl.util.PrimitiveIntIteratorForArray;
import org.neo4j.kernel.impl.util.PrimitiveLongIterator;

import static org.neo4j.helpers.collection.Iterables.filter;
import static org.neo4j.helpers.collection.Iterables.map;
import static org.neo4j.helpers.collection.IteratorUtil.toPrimitiveIntIterator;
import static org.neo4j.kernel.impl.util.PrimitiveIntIteratorForArray.primitiveIntIteratorToIntArray;

/**
 * This is the object-caching layer. It delegates to the legacy object cache system if possible, or delegates to the
 * disk layer if there is no relevant caching.
 *
 * An important consideration when working on this is that there are plans to remove the object cache, which means that
 * the aim for this layer is to disappear.
 */
public final class GeneratedCacheLayer implements StatementLayer
{
    private static final Function<? super SchemaRule, IndexDescriptor> TO_INDEX_RULE =
            new Function<SchemaRule, IndexDescriptor>()
    {
        @Override
        public IndexDescriptor apply( SchemaRule from )
        {
            IndexRule rule = (IndexRule) from;
            // We know that we only have int range of property key ids.
            return new IndexDescriptor( rule.getLabel(), rule.getPropertyKey() );
        }
    };
    private final CacheLoader<Iterator<DefinedProperty>> nodePropertyLoader = new CacheLoader<Iterator<DefinedProperty>>()
    {
        @Override
        public Iterator<DefinedProperty> load( long id ) throws EntityNotFoundException
        {
            return diskLayer.nodeGetAllProperties( id );
        }
    };
    private final CacheLoader<Iterator<DefinedProperty>> relationshipPropertyLoader = new CacheLoader<Iterator<DefinedProperty>>()
    {
        @Override
        public Iterator<DefinedProperty> load( long id ) throws EntityNotFoundException
        {
            return diskLayer.relationshipGetAllProperties( id );
        }
    };
    private final CacheLoader<Iterator<DefinedProperty>> graphPropertyLoader = new CacheLoader<Iterator<DefinedProperty>>()
    {
        @Override
        public Iterator<DefinedProperty> load( long id ) throws EntityNotFoundException
        {
            return diskLayer.graphGetAllProperties();
        }
    };
    private final CacheLoader<int[]> nodeLabelLoader = new CacheLoader<int[]>()
    {
        @Override
        public int[] load( long id ) throws EntityNotFoundException
        {
            return primitiveIntIteratorToIntArray( diskLayer.nodeGetLabels( id ) );
        }
    };

    private final PersistenceCache persistenceCache;
    private final SchemaCache schemaCache;
    private final DiskLayer diskLayer;
    private final IndexingService indexingService;
    private final NodeManager nodeManager;

    public GeneratedCacheLayer(
            DiskLayer diskLayer,
            PersistenceCache persistenceCache,
            IndexingService indexingService,
            SchemaCache schemaCache, NodeManager nodeManager )
    {
        this.diskLayer = diskLayer;
        this.persistenceCache = persistenceCache;
        this.indexingService = indexingService;
        this.schemaCache = schemaCache;
        this.nodeManager = nodeManager;
    }

    @Override
    public boolean nodeHasLabel( KernelStatement state, long nodeId, int labelId ) throws EntityNotFoundException
    {
        return persistenceCache.nodeHasLabel( state, nodeId, labelId, nodeLabelLoader );
    }

    @Override
    public PrimitiveIntIterator nodeGetLabels( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        return new PrimitiveIntIteratorForArray( persistenceCache.nodeGetLabels( state, nodeId, nodeLabelLoader ) );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetForLabel( KernelStatement state, int labelId )
    {
        return toIndexDescriptors( schemaCache.schemaRulesForLabel( labelId ), SchemaRule.Kind.INDEX_RULE );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetAll( KernelStatement state )
    {
        return toIndexDescriptors( schemaCache.schemaRules(), SchemaRule.Kind.INDEX_RULE );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetForLabel( KernelStatement state, int labelId )
    {
        return toIndexDescriptors( schemaCache.schemaRulesForLabel( labelId ),
                SchemaRule.Kind.CONSTRAINT_INDEX_RULE );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetAll( KernelStatement state )
    {
        return toIndexDescriptors( schemaCache.schemaRules(), SchemaRule.Kind.CONSTRAINT_INDEX_RULE );
    }

    private static Iterator<IndexDescriptor> toIndexDescriptors( Iterable<SchemaRule> rules,
                                                                 final SchemaRule.Kind kind )
    {
        Iterator<SchemaRule> filteredRules = filter( new Predicate<SchemaRule>()
        {
            @Override
            public boolean accept( SchemaRule item )
            {
                return item.getKind() == kind;
            }
        }, rules.iterator() );
        return map( TO_INDEX_RULE, filteredRules );
    }

    @Override
    public Long indexGetOwningUniquenessConstraintId( KernelStatement state, IndexDescriptor index )
            throws SchemaRuleNotFoundException
    {
        IndexRule rule = indexRule( index, SchemaStorage.IndexRuleKind.ALL );
        if ( rule != null )
        {
            return rule.getOwningConstraint();
        }
        return diskLayer.indexGetOwningUniquenessConstraintId( index );
    }

    @Override
    public long indexGetCommittedId( KernelStatement state, IndexDescriptor index, SchemaStorage.IndexRuleKind kind )
            throws SchemaRuleNotFoundException
    {
        IndexRule rule = indexRule( index, kind );
        if ( rule != null )
        {
            return rule.getId();
        }
        return diskLayer.indexGetCommittedId( index );
    }

    public IndexRule indexRule( IndexDescriptor index, SchemaStorage.IndexRuleKind kind )
    {
        for ( SchemaRule rule : schemaCache.schemaRulesForLabel( index.getLabelId() ) )
        {
            if ( rule instanceof IndexRule )
            {
                IndexRule indexRule = (IndexRule) rule;
                if ( kind.isOfKind( indexRule ) && indexRule.getPropertyKey() == index.getPropertyKeyId() )
                {
                    return indexRule;
                }
            }
        }
        return null;
    }

    @Override
    public PrimitiveLongIterator nodeGetPropertyKeys( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        return persistenceCache.nodeGetPropertyKeys( nodeId, nodePropertyLoader );
    }

    @Override
    public Property nodeGetProperty( KernelStatement state, long nodeId, int propertyKeyId ) throws EntityNotFoundException
    {
        return persistenceCache.nodeGetProperty( nodeId, propertyKeyId, nodePropertyLoader );
    }

    @Override
    public Iterator<DefinedProperty> nodeGetAllProperties( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        return persistenceCache.nodeGetProperties( nodeId, nodePropertyLoader );
    }

    @Override
    public PrimitiveLongIterator relationshipGetPropertyKeys( KernelStatement state, long relationshipId )
            throws EntityNotFoundException
    {
        return new PropertyKeyIdIterator( relationshipGetAllProperties( state, relationshipId ) );
    }

    @Override
    public Property relationshipGetProperty( KernelStatement state, long relationshipId, int propertyKeyId )
            throws EntityNotFoundException
    {
        return persistenceCache.relationshipGetProperty( relationshipId, propertyKeyId,
                relationshipPropertyLoader );
    }

    @Override
    public Iterator<DefinedProperty> relationshipGetAllProperties( KernelStatement state, long nodeId )
            throws EntityNotFoundException
    {
        return persistenceCache.relationshipGetProperties( nodeId, relationshipPropertyLoader );
    }

    @Override
    public PrimitiveLongIterator graphGetPropertyKeys( KernelStatement state )
    {
        return persistenceCache.graphGetPropertyKeys( graphPropertyLoader );
    }

    @Override
    public Property graphGetProperty( KernelStatement state, int propertyKeyId )
    {
        return persistenceCache.graphGetProperty( graphPropertyLoader, propertyKeyId );
    }

    @Override
    public Iterator<DefinedProperty> graphGetAllProperties( KernelStatement state )
    {
        return persistenceCache.graphGetProperties( graphPropertyLoader );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabelAndPropertyKey(
            KernelStatement state, int labelId, int propertyKeyId )
    {
        return schemaCache.constraintsForLabelAndProperty( labelId, propertyKeyId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetForLabel( KernelStatement state, int labelId )
    {
        return schemaCache.constraintsForLabel( labelId );
    }

    @Override
    public Iterator<UniquenessConstraint> constraintsGetAll( KernelStatement state )
    {
        return schemaCache.constraints();
    }

    @Override
    public long nodeGetUniqueFromIndexLookup(
            KernelStatement state,
            IndexDescriptor index,
            Object value )
            throws IndexNotFoundKernelException, IndexBrokenKernelException
    {
        return diskLayer.nodeGetUniqueFromIndexLookup( state, schemaCache.indexId( index ), value ).next();
    }

    @Override
    public PrimitiveLongIterator nodesGetForLabel( KernelStatement state, int labelId )
    {
        return diskLayer.nodesGetForLabel( state, labelId );
    }

    @Override
    public PrimitiveLongIterator nodesGetFromIndexLookup( KernelStatement state, IndexDescriptor index, Object value )
            throws IndexNotFoundKernelException
    {
        return diskLayer.nodesGetFromIndexLookup( state, schemaCache.indexId( index ), value );
    }

    @Override
    public IndexDescriptor indexesGetForLabelAndPropertyKey( KernelStatement state, int labelId, int propertyKey )
            throws SchemaRuleNotFoundException
    {
        return schemaCache.indexDescriptor( labelId, propertyKey );
    }

    @Override
    public InternalIndexState indexGetState( KernelStatement state, IndexDescriptor descriptor )
            throws IndexNotFoundKernelException
    {
        return indexingService.getProxyForRule( schemaCache.indexId( descriptor ) ).getState();
    }

    @Override
    public String indexGetFailure( KernelStatement state, IndexDescriptor descriptor ) throws IndexNotFoundKernelException
    {
        return diskLayer.indexGetFailure( descriptor );
    }

    @Override
    public int labelGetForName( Statement state, String labelName )
    {
        return diskLayer.labelGetForName( labelName );
    }

    @Override
    public String labelGetName( Statement state, int labelId ) throws LabelNotFoundKernelException
    {
        return diskLayer.labelGetName( labelId );
    }

    @Override
    public int propertyKeyGetForName( Statement state, String propertyKeyName )
    {
        return diskLayer.propertyKeyGetForName( propertyKeyName );
    }

    @Override
    public int propertyKeyGetOrCreateForName( Statement state, String propertyKeyName )
    {
        return diskLayer.propertyKeyGetOrCreateForName( propertyKeyName );
    }

    @Override
    public String propertyKeyGetName( Statement state, int propertyKeyId ) throws PropertyKeyIdNotFoundKernelException
    {
        return diskLayer.propertyKeyGetName( propertyKeyId );
    }

    @Override
    public Iterator<Token> propertyKeyGetAllTokens(Statement state)
    {
        return diskLayer.propertyKeyGetAllTokens().iterator();
    }

    @Override
    public Iterator<Token> labelsGetAllTokens(Statement state)
    {
        return diskLayer.labelGetAllTokens().iterator();
    }

    @Override
    public int relationshipTypeGetForName( Statement state, String relationshipTypeName )
    {
        return diskLayer.relationshipTypeGetForName( relationshipTypeName );
    }

    @Override
    public String relationshipTypeGetName( Statement state, int relationshipTypeId ) throws RelationshipTypeIdNotFoundKernelException
    {
        return diskLayer.relationshipTypeGetName( relationshipTypeId );
    }

    @Override
    public int labelGetOrCreateForName( Statement state, String labelName ) throws TooManyLabelsException
    {
        return diskLayer.labelGetOrCreateForName( labelName );
    }

    @Override
    public int relationshipTypeGetOrCreateForName( Statement state, String relationshipTypeName )
    {
        return diskLayer.relationshipTypeGetOrCreateForName( relationshipTypeName );
    }

    @Override
    public PrimitiveLongIterator nodeGetRelationships( KernelStatement statement, long nodeId, Direction direction ) throws EntityNotFoundException
    {
        return persistenceCache.nodeGetRelationships( nodeId, direction );
    }

    @Override
    public PrimitiveLongIterator nodeGetRelationships( KernelStatement state, long nodeId, Direction direction,
                                                        int[] relTypes ) throws EntityNotFoundException
    {
        return persistenceCache.nodeGetRelationships( nodeId, direction, relTypes );
    }

    @Override
    public int nodeGetDegree( KernelStatement statement, long nodeId, Direction direction, int relType ) throws EntityNotFoundException
    {
        return persistenceCache.getNode( nodeId ).getDegree( nodeManager, relType, direction );
    }

    @Override
    public int nodeGetDegree( KernelStatement statement, long nodeId, Direction direction ) throws EntityNotFoundException
    {
        return persistenceCache.getNode( nodeId ).getDegree( nodeManager, direction );
    }

    @Override
    public PrimitiveIntIterator nodeGetRelationshipTypes( KernelStatement statement, long nodeId ) throws EntityNotFoundException

    {
        return toPrimitiveIntIterator( persistenceCache.getNode( nodeId ).getRelationshipTypes( nodeManager ) );
    }

    public void visit( long relationshipId, StoreReadLayer.RelationshipVisitor relationshipVisitor ) throws EntityNotFoundException
    {
        RelationshipImpl relationship = persistenceCache.getRelationship( relationshipId );
        relationshipVisitor.visit( relationshipId, relationship.getStartNodeId(), relationship.getEndNodeId(),
                relationship.getTypeId());
    }

    public final void indexDrop(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.constraints.UniquenessConstraint uniquenessConstraintCreate(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property graphSetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.properties.DefinedProperty _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final void relationshipDelete(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final long relationshipCreate(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            long _ignore2, long _ignore3) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final void nodeDelete(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final boolean nodeAddLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final void constraintDrop(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.constraints.UniquenessConstraint _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final boolean nodeRemoveLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property nodeRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.lang.Object schemaStateGetOrCreate(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            java.lang.Object _ignore1, org.neo4j.helpers.Function _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property graphRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property relationshipRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property relationshipSetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.kernel.api.properties.DefinedProperty _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.index.IndexDescriptor indexCreate(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.impl.api.operations.StatementLayer delegate() {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property nodeSetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.kernel.api.properties.DefinedProperty _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final void uniqueIndexDrop(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final boolean schemaStateContains(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            java.lang.Object _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }
}
