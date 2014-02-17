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

import org.neo4j.helpers.Function;
import org.neo4j.kernel.impl.api.KernelStatement;
import org.neo4j.kernel.impl.api.UpdateableSchemaState;
import org.neo4j.kernel.impl.api.operations.StatementLayer;

public final class GeneratedSchemaStateConcern implements StatementLayer
{
    private final GeneratedCacheLayer __CacheLayer;
    private final UpdateableSchemaState schemaState;

    public GeneratedSchemaStateConcern( GeneratedCacheLayer __CacheLayer, UpdateableSchemaState schemaState )
    {
        this.__CacheLayer = __CacheLayer;
        this.schemaState = schemaState;
    }

    @Override
    public <K, V> V schemaStateGetOrCreate( KernelStatement state, K key, Function<K, V> creator )
    {
        return schemaState.getOrCreate( key, creator );
    }

    @Override
    public <K> boolean schemaStateContains( KernelStatement state, K key )
    {
        return schemaState.get( key ) != null;
    }

    public final void indexDrop(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
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

    public final java.util.Iterator constraintsGetAll(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator constraintsGetForLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
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

    public final long relationshipCreate(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            long _ignore2, long _ignore3) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final long nodeGetUniqueFromIndexLookup(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1,
            java.lang.Object _ignore2) {
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

    public final void constraintDrop(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.constraints.UniquenessConstraint _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property relationshipGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
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

    public final org.neo4j.kernel.api.properties.Property graphGetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator indexesGetForLabel(
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

    public final java.util.Iterator uniqueIndexesGetAll(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
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

    public final org.neo4j.kernel.api.properties.Property graphRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.index.InternalIndexState indexGetState(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property relationshipRemoveProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            int _ignore2) {
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

    public final java.lang.Long indexGetOwningUniquenessConstraintId(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
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

    public final java.util.Iterator indexesGetAll(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final int propertyKeyGetOrCreateForName(
            org.neo4j.kernel.api.Statement _ignore0, java.lang.String _ignore1) {
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

    public final java.lang.String labelGetName(
            org.neo4j.kernel.api.Statement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final long indexGetCommittedId(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1,
            org.neo4j.kernel.impl.nioneo.store.SchemaStorage.IndexRuleKind _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.properties.Property nodeSetProperty(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1,
            org.neo4j.kernel.api.properties.DefinedProperty _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator relationshipGetAllProperties(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, long _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator uniqueIndexesGetForLabel(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final java.util.Iterator constraintsGetForLabelAndPropertyKey(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final void uniqueIndexDrop(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0,
            org.neo4j.kernel.api.index.IndexDescriptor _ignore1) {
        throw new java.lang.UnsupportedOperationException();
    }

    public final org.neo4j.kernel.api.index.IndexDescriptor indexesGetForLabelAndPropertyKey(
            org.neo4j.kernel.impl.api.KernelStatement _ignore0, int _ignore1,
            int _ignore2) {
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
