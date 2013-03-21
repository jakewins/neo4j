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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;

public class SchemaStateOperationsTest
{
    private SchemaStateOperations inner;
    private TransactionalSchemaState stateHolder;
            
    @Test
    public void addingIndexRuleShouldNotImmediatelyFlushSchemaState() throws ConstraintViolationKernelException
    {
        // GIVEN
        SchemaStateOperations stateOperations = new SchemaStateOperations( inner, stateHolder );

        // WHEN
        stateOperations.addIndexRule( 0L, 1L );

        // THEN
        verifyZeroInteractions( stateHolder );
    }

    @Test
    public void dropIndexRuleShouldFlushStateHolder() throws ConstraintViolationKernelException
    {
        // GIVEN
        SchemaStateOperations stateOperations = new SchemaStateOperations( inner, stateHolder );
        IndexRule rule = stateOperations.addIndexRule( 0L, 1L );

        // WHEN
        stateOperations.dropIndexRule( rule );

        // THEN
        verify( stateHolder).flush();
    }

    @Before
    public void before()
    {
        inner = mock( SchemaStateOperations.class );
        stateHolder = mock( TransactionalSchemaState.class );
    }
}
