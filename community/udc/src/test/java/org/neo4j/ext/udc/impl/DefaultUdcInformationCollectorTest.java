/*
 * Copyright (c) 2002-2015 "Neo Technology,"
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
package org.neo4j.ext.udc.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.neo4j.ext.udc.UdcConstants;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.IdGeneratorFactory;
import org.neo4j.kernel.IdType;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.core.StartupStatistics;
import org.neo4j.kernel.impl.store.id.IdGenerator;
import org.neo4j.kernel.impl.store.id.IdRange;
import org.neo4j.udc.UsageDataKeys;
import org.neo4j.udc.UsageData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class DefaultUdcInformationCollectorTest
{
    private final UsageData sysinfo = new UsageData();
    private final DefaultUdcInformationCollector collector = new DefaultUdcInformationCollector(
            new Config(), null,
            new StubIdGeneratorFactory(), mock( StartupStatistics.class ), sysinfo );

    @Test
    public void shouldIncludeTheMacAddress()
    {
        assertNotNull( collector.getUdcParams().get( UdcConstants.MAC ) );
    }

    @Test
    public void shouldIncludeTheNumberOfProcessors()
    {
        assertNotNull( collector.getUdcParams().get( UdcConstants.NUM_PROCESSORS ) );
    }

    @Test
    public void shouldIncludeTotalMemorySize()
    {
        assertNotNull( collector.getUdcParams().get( UdcConstants.TOTAL_MEMORY ) );
    }

    @Test
    public void shouldIncludeHeapSize()
    {
        assertNotNull( collector.getUdcParams().get( UdcConstants.HEAP_SIZE ) );
    }

    @Test
    public void shouldIncludeNodeIdsInUse()
    {
        assertEquals( "100", collector.getUdcParams().get( UdcConstants.NODE_IDS_IN_USE ) );
    }

    @Test
    public void shouldIncludeRelationshipIdsInUse()
    {
        assertEquals( "200", collector.getUdcParams().get( UdcConstants.RELATIONSHIP_IDS_IN_USE ) );
    }

    @Test
    public void shouldIncludePropertyIdsInUse()
    {
        assertEquals( "400", collector.getUdcParams().get( UdcConstants.PROPERTY_IDS_IN_USE ) );
    }

    @Test
    public void shouldIncludeLabelIdsInUse()
    {
        assertEquals( "300", collector.getUdcParams().get( UdcConstants.LABEL_IDS_IN_USE ) );
    }

    @Test
    public void shouldIncludeVersionEditionAndMode() throws Throwable
    {
        // Given
        sysinfo.set( UsageDataKeys.version, "1.2.3");
        sysinfo.set( UsageDataKeys.edition, UsageDataKeys.Edition.advanced );
        sysinfo.set( UsageDataKeys.operationalMode, UsageDataKeys.OperationalMode.ha );

        // When & Then
        assertEquals( "1.2.3", collector.getUdcParams().get( UdcConstants.VERSION ) );
        assertEquals( "advanced", collector.getUdcParams().get( UdcConstants.EDITION ) );
        assertEquals( "ha", collector.getUdcParams().get( UdcConstants.DATABASE_MODE ) );
    }

    @Test
    public void shouldIncludeRecentClientNames() throws Throwable
    {
        // Given
        sysinfo.get( UsageDataKeys.clientNames ).add( "SteveBrookClient/1.0" );
        sysinfo.get( UsageDataKeys.clientNames ).add( "MayorClient/1.0" );

        // When & Then
        assertEquals( "SteveBrookClient/1.0,MayorClient/1.0", collector.getUdcParams().get( UdcConstants.USER_AGENTS ) );
    }

    private static class StubIdGeneratorFactory implements IdGeneratorFactory
    {
        private final Map<IdType, Long> idsInUse = new HashMap<>();

        {
            idsInUse.put( IdType.NODE, 100l );
            idsInUse.put( IdType.RELATIONSHIP, 200l );
            idsInUse.put( IdType.LABEL_TOKEN, 300l );
            idsInUse.put( IdType.PROPERTY, 400l );
        }

        @Override
        public IdGenerator open( FileSystemAbstraction fs, File fileName, int grabSize, IdType idType, long highId )
        {
            return get( idType );
        }

        @Override
        public void create( FileSystemAbstraction fs, File fileName, long highId )
        {   // Ignore
        }

        @Override
        public IdGenerator get( IdType idType )
        {
            return new StubIdGenerator( idsInUse.get( idType ) );
        }
    }

    private static class StubIdGenerator implements IdGenerator
    {
        private final long numberOfIdsInUse;

        public StubIdGenerator( long numberOfIdsInUse )
        {
            this.numberOfIdsInUse = numberOfIdsInUse;
        }

        @Override
        public long nextId()
        {
            throw new UnsupportedOperationException( "Please implement" );
        }

        @Override
        public IdRange nextIdBatch( int size )
        {
            throw new UnsupportedOperationException( "Please implement" );
        }

        @Override
        public void setHighId( long id )
        {
            throw new UnsupportedOperationException( "Please implement" );
        }

        @Override
        public long getHighId()
        {
            return 0;
        }

        @Override
        public long getHighestPossibleIdInUse()
        {
            return 0;
        }

        @Override
        public void freeId( long id )
        {   // Ignore
        }

        @Override
        public void close()
        {   // Ignore
        }

        @Override
        public long getNumberOfIdsInUse()
        {
            return numberOfIdsInUse;
        }

        @Override
        public long getDefragCount()
        {
            return 0;
        }

        @Override
        public void delete()
        {   // Ignore
        }
    }
}
