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

import java.io.Closeable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.LabelNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.PropertyKeyIdNotFoundException;
import org.neo4j.kernel.api.exceptions.PropertyKeyNotFoundException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.DropIndexFailureException;
import org.neo4j.kernel.api.exceptions.schema.SchemaKernelException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.api.operations.EntityReadOperations;
import org.neo4j.kernel.api.operations.EntityWriteOperations;
import org.neo4j.kernel.api.operations.KeyReadOperations;
import org.neo4j.kernel.api.operations.KeyWriteOperations;
import org.neo4j.kernel.api.operations.SchemaReadOperations;
import org.neo4j.kernel.api.operations.SchemaStateOperations;
import org.neo4j.kernel.api.operations.SchemaWriteOperations;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.impl.api.PrimitiveLongIterator;
import org.neo4j.kernel.impl.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.core.Token;

public class KernelStatement implements Closeable
{
    private final KeyReadOperations keyReadOperations;
    private final KeyWriteOperations keyWriteOperations;
    private final EntityReadOperations entityReadOperations;
    private final EntityWriteOperations entityWriteOperations;
    private final SchemaReadOperations schemaReadOperations;
    private final SchemaWriteOperations schemaWriteOperations;
    private final SchemaStateOperations schemaStateOperations;
    
    @SuppressWarnings( "rawtypes" )
    private Map<Class,Object> additionalParts;

    public KernelStatement(
            KeyReadOperations keyReadOperations,
            KeyWriteOperations keyWriteOperations,
            EntityReadOperations entityReadOperations,
            EntityWriteOperations entityWriteOperations,
            SchemaReadOperations schemaReadOperations,
            SchemaWriteOperations schemaWriteOperations,
            SchemaStateOperations schemaStateOperations )
    {
        this.keyReadOperations = keyReadOperations;
        this.keyWriteOperations = keyWriteOperations;
        this.entityReadOperations = entityReadOperations;
        this.entityWriteOperations = entityWriteOperations;
        this.schemaReadOperations = schemaReadOperations;
        this.schemaWriteOperations = schemaWriteOperations;
        this.schemaStateOperations = schemaStateOperations;
    }

    @Override
    public void close()
    {
    }

    public <T> KernelStatement additionalPart( Class<T> cls, T value )
    {
        if ( additionalParts == null )
        {
            additionalParts = new HashMap<>();
        }
        additionalParts.put( cls, value );
        return this;
    }
    
    @SuppressWarnings( "unchecked" )
    public <T> T resolve( Class<T> cls )
    {
        T part = additionalParts != null ? (T) additionalParts.get( cls ) : null;
        if ( part == null )
        {
            throw new IllegalArgumentException( "No part " + cls.getName() );
        }
        return part;
    }

    public KeyReadOperations keyReadOperations()
    {
        return checkNotNull( keyReadOperations, KeyReadOperations.class );
    }

    public KeyWriteOperations keyWriteOperations()
    {
        return checkNotNull( keyWriteOperations, KeyWriteOperations.class );
    }

    public EntityReadOperations entityReadOperations()
    {
        return checkNotNull( entityReadOperations, EntityReadOperations.class );
    }

    public EntityWriteOperations entityWriteOperations()
    {
        return checkNotNull( entityWriteOperations, EntityWriteOperations.class );
    }

    public SchemaReadOperations schemaReadOperations()
    {
        return checkNotNull( schemaReadOperations, SchemaReadOperations.class );
    }

    public SchemaWriteOperations schemaWriteOperations()
    {
        return checkNotNull( schemaWriteOperations, SchemaWriteOperations.class );
    }

    public SchemaStateOperations schemaStateOperations()
    {
        return checkNotNull( schemaStateOperations, SchemaStateOperations.class );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public KernelStatement override(
            KeyReadOperations keyReadOperations,
            KeyWriteOperations keyWriteOperations,
            EntityReadOperations entityReadOperations,
            EntityWriteOperations entityWriteOperations,
            SchemaReadOperations schemaReadOperations,
            SchemaWriteOperations schemaWriteOperations,
            SchemaStateOperations schemaStateOperations,
            Object... alternatingAdditionalClassAndObject )
    {
        KernelStatement parts = new KernelStatement(
                eitherOr( keyReadOperations, this.keyReadOperations, KeyReadOperations.class ),
                eitherOr( keyWriteOperations, this.keyWriteOperations, KeyWriteOperations.class ),
                eitherOr( entityReadOperations, this.entityReadOperations, EntityReadOperations.class ),
                eitherOr( entityWriteOperations, this.entityWriteOperations, EntityWriteOperations.class ),
                eitherOr( schemaReadOperations, this.schemaReadOperations, SchemaReadOperations.class ),
                eitherOr( schemaWriteOperations, this.schemaWriteOperations, SchemaWriteOperations.class ),
                eitherOr( schemaStateOperations, this.schemaStateOperations, SchemaStateOperations.class ));

        if ( additionalParts != null )
        {
            parts.additionalParts = new HashMap<>( additionalParts );
        }
        for ( int i = 0; i < alternatingAdditionalClassAndObject.length; i++ )
        {
            parts.additionalPart( (Class) alternatingAdditionalClassAndObject[i++],
                    alternatingAdditionalClassAndObject[i] );
        }

        return parts;
    }
    
    private <T> T checkNotNull( T object, Class<T> cls )
    {
        if ( object == null )
        {
            throw new IllegalStateException( "No part of type " + cls.getSimpleName() + " assigned" );
        }
        return object;
    }

    private <T> T eitherOr( T first, T other, Class<T> cls )
    {
        return first != null ? first : other;
    }
    
    /**
     * @deprecated Transitional method as long as consumers of the {@link KernelAPI} still always deal with the full
     * {@link StatementOperations} interface. As they will access individual parts instead this should go away.
     * This is just for convenience, but should be removed since it adds one level of delegation.
     *  
     * @return all these parts as a unified {@link StatementOperations} instance.
     */
    @Deprecated
    public StatementOperations asStatementOperations()
    {
        return new StatementOperations()
        {
            @Override
            public long labelGetForName( String labelName ) throws LabelNotFoundKernelException
            {
                return keyReadOperations.labelGetForName( labelName );
            }
            @Override
            public String labelGetName( long labelId ) throws LabelNotFoundKernelException
            {
                return keyReadOperations.labelGetName( labelId );
            }
            @Override
            public long propertyKeyGetForName( String propertyKeyName ) throws PropertyKeyNotFoundException
            {
                return keyReadOperations.propertyKeyGetForName( propertyKeyName );
            }
            @Override
            public String propertyKeyGetName( long propertyKeyId ) throws PropertyKeyIdNotFoundException
            {
                return keyReadOperations.propertyKeyGetName( propertyKeyId );
            }
            @Override
            public Iterator<Token> labelsGetAllTokens()
            {
                return keyReadOperations.labelsGetAllTokens();
            }
            @Override
            public long labelGetOrCreateForName( String labelName ) throws SchemaKernelException
            {
                return keyWriteOperations.labelGetOrCreateForName( labelName );
            }
            @Override
            public long propertyKeyGetOrCreateForName( String propertyKeyName ) throws SchemaKernelException
            {
                return keyWriteOperations.propertyKeyGetOrCreateForName( propertyKeyName );
            }
            @Override
            public PrimitiveLongIterator nodesGetForLabel( long labelId )
            {
                return entityReadOperations.nodesGetForLabel( labelId );
            }
            @Override
            public PrimitiveLongIterator nodesGetFromIndexLookup( IndexDescriptor index, Object value )
                    throws IndexNotFoundKernelException
            {
                return entityReadOperations.nodesGetFromIndexLookup( index, value );
            }
            @Override
            public boolean nodeHasLabel( long nodeId, long labelId ) throws EntityNotFoundException
            {
                return entityReadOperations.nodeHasLabel( nodeId, labelId );
            }
            @Override
            public PrimitiveLongIterator nodeGetLabels( long nodeId ) throws EntityNotFoundException
            {
                return entityReadOperations.nodeGetLabels( nodeId );
            }
            @Override
            public Property nodeGetProperty( long nodeId, long propertyKeyId ) throws PropertyKeyIdNotFoundException,
                    EntityNotFoundException
            {
                return entityReadOperations.nodeGetProperty( nodeId, propertyKeyId );
            }
            @Override
            public Property relationshipGetProperty( long relationshipId, long propertyKeyId )
                    throws PropertyKeyIdNotFoundException, EntityNotFoundException
            {
                return entityReadOperations.relationshipGetProperty( relationshipId, propertyKeyId );
            }
            @Override
            public Property graphGetProperty( long propertyKeyId ) throws PropertyKeyIdNotFoundException
            {
                return entityReadOperations.graphGetProperty( propertyKeyId );
            }
            @Override
            public boolean nodeHasProperty( long nodeId, long propertyKeyId ) throws PropertyKeyIdNotFoundException,
                    EntityNotFoundException
            {
                return entityReadOperations.nodeHasProperty( nodeId, propertyKeyId );
            }
            @Override
            public boolean relationshipHasProperty( long relationshipId, long propertyKeyId )
                    throws PropertyKeyIdNotFoundException, EntityNotFoundException
            {
                return entityReadOperations.relationshipHasProperty( relationshipId, propertyKeyId );
            }
            @Override
            public boolean graphHasProperty( long propertyKeyId ) throws PropertyKeyIdNotFoundException
            {
                return entityReadOperations.graphHasProperty( propertyKeyId );
            }
            @Override
            public PrimitiveLongIterator nodeGetPropertyKeys( long nodeId ) throws EntityNotFoundException
            {
                return entityReadOperations.nodeGetPropertyKeys( nodeId );
            }
            @Override
            public Iterator<Property> nodeGetAllProperties( long nodeId ) throws EntityNotFoundException
            {
                return entityReadOperations.nodeGetAllProperties( nodeId );
            }
            @Override
            public PrimitiveLongIterator relationshipGetPropertyKeys( long relationshipId ) throws EntityNotFoundException
            {
                return entityReadOperations.relationshipGetPropertyKeys( relationshipId );
            }
            @Override
            public Iterator<Property> relationshipGetAllProperties( long relationshipId )
                    throws EntityNotFoundException
            {
                return entityReadOperations.relationshipGetAllProperties( relationshipId );
            }
            @Override
            public PrimitiveLongIterator graphGetPropertyKeys()
            {
                return entityReadOperations.graphGetPropertyKeys();
            }
            @Override
            public Iterator<Property> graphGetAllProperties()
            {
                return entityReadOperations.graphGetAllProperties();
            }
            @Override
            public void nodeDelete( long nodeId )
            {
                entityWriteOperations.nodeDelete( nodeId );
            }
            @Override
            public void relationshipDelete( long relationshipId )
            {
                entityWriteOperations.relationshipDelete( relationshipId );
            }
            @Override
            public boolean nodeAddLabel( long nodeId, long labelId ) throws EntityNotFoundException
            {
                return entityWriteOperations.nodeAddLabel( nodeId, labelId );
            }
            @Override
            public boolean nodeRemoveLabel( long nodeId, long labelId ) throws EntityNotFoundException
            {
                return entityWriteOperations.nodeRemoveLabel( nodeId, labelId );
            }
            @Override
            public Property nodeSetProperty( long nodeId, Property property ) throws PropertyKeyIdNotFoundException,
                    EntityNotFoundException
            {
                return entityWriteOperations.nodeSetProperty( nodeId, property );
            }
            @Override
            public Property relationshipSetProperty( long relationshipId, Property property )
                    throws PropertyKeyIdNotFoundException, EntityNotFoundException
            {
                return entityWriteOperations.relationshipSetProperty( relationshipId, property );
            }
            @Override
            public Property graphSetProperty( Property property ) throws PropertyKeyIdNotFoundException
            {
                return entityWriteOperations.graphSetProperty( property );
            }
            @Override
            public Property nodeRemoveProperty( long nodeId, long propertyKeyId )
                    throws PropertyKeyIdNotFoundException, EntityNotFoundException
            {
                return entityWriteOperations.nodeRemoveProperty( nodeId, propertyKeyId );
            }
            @Override
            public Property relationshipRemoveProperty( long relationshipId, long propertyKeyId )
                    throws PropertyKeyIdNotFoundException, EntityNotFoundException
            {
                return entityWriteOperations.relationshipRemoveProperty( relationshipId, propertyKeyId );
            }
            @Override
            public Property graphRemoveProperty( long propertyKeyId ) throws PropertyKeyIdNotFoundException
            {
                return entityWriteOperations.graphRemoveProperty( propertyKeyId );
            }
            @Override
            public IndexDescriptor indexesGetForLabelAndPropertyKey( long labelId, long propertyKey )
                    throws SchemaRuleNotFoundException
            {
                return schemaReadOperations.indexesGetForLabelAndPropertyKey( labelId, propertyKey );
            }
            @Override
            public Iterator<IndexDescriptor> indexesGetForLabel( long labelId )
            {
                return schemaReadOperations.indexesGetForLabel( labelId );
            }
            @Override
            public Iterator<IndexDescriptor> indexesGetAll()
            {
                return schemaReadOperations.indexesGetAll();
            }
            @Override
            public Iterator<IndexDescriptor> uniqueIndexesGetForLabel( long labelId )
            {
                return schemaReadOperations.uniqueIndexesGetForLabel( labelId );
            }
            @Override
            public Iterator<IndexDescriptor> uniqueIndexesGetAll()
            {
                return schemaReadOperations.uniqueIndexesGetAll();
            }
            @Override
            public InternalIndexState indexGetState( IndexDescriptor descriptor ) throws IndexNotFoundKernelException
            {
                return schemaReadOperations.indexGetState( descriptor );
            }
            @Override
            public String indexGetFailure( IndexDescriptor descriptor ) throws IndexNotFoundKernelException
            {
                return schemaReadOperations.indexGetFailure( descriptor );
            }
            @Override
            public Iterator<UniquenessConstraint> constraintsGetForLabelAndPropertyKey( long labelId, long propertyKeyId )
            {
                return schemaReadOperations.constraintsGetForLabelAndPropertyKey( labelId, propertyKeyId );
            }
            @Override
            public Iterator<UniquenessConstraint> constraintsGetForLabel( long labelId )
            {
                return schemaReadOperations.constraintsGetForLabel( labelId );
            }
            @Override
            public Iterator<UniquenessConstraint> constraintsGetAll()
            {
                return schemaReadOperations.constraintsGetAll();
            }
            @Override
            public Long indexGetOwningUniquenessConstraintId( IndexDescriptor index )
                    throws SchemaRuleNotFoundException
            {
                return schemaReadOperations.indexGetOwningUniquenessConstraintId( index );
            }
            @Override
            public long indexGetCommittedId( IndexDescriptor index ) throws SchemaRuleNotFoundException
            {
                return schemaReadOperations.indexGetCommittedId( index );
            }
            @Override
            public IndexDescriptor indexCreate( long labelId, long propertyKeyId ) throws SchemaKernelException
            {
                return schemaWriteOperations.indexCreate( labelId, propertyKeyId );
            }
            @Override
            public IndexDescriptor uniqueIndexCreate( long labelId, long propertyKey ) throws SchemaKernelException
            {
                return schemaWriteOperations.uniqueIndexCreate( labelId, propertyKey );
            }
            @Override
            public void indexDrop( IndexDescriptor descriptor ) throws DropIndexFailureException
            {
                schemaWriteOperations.indexDrop( descriptor );
            }
            @Override
            public void uniqueIndexDrop( IndexDescriptor descriptor ) throws DropIndexFailureException
            {
                schemaWriteOperations.uniqueIndexDrop( descriptor );
            }
            @Override
            public UniquenessConstraint uniquenessConstraintCreate( long labelId, long propertyKeyId )
                    throws SchemaKernelException
            {
                return schemaWriteOperations.uniquenessConstraintCreate( labelId, propertyKeyId );
            }
            @Override
            public void constraintDrop( UniquenessConstraint constraint )
            {
                schemaWriteOperations.constraintDrop( constraint );
            }
            @Override
            public <K, V> V schemaStateGetOrCreate( K key, Function<K, V> creator )
            {
                return schemaStateOperations.schemaStateGetOrCreate( key, creator );
            }
            @Override
            public <K> boolean schemaStateContains( K key )
            {
                return schemaStateOperations.schemaStateContains( key );
            }
        };
    }
}
