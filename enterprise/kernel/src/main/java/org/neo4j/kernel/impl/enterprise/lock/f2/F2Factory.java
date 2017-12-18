/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.enterprise.lock.f2;

import java.time.Clock;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.neo4j.graphdb.config.Setting;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.storageengine.api.lock.ResourceType;

import static org.neo4j.kernel.configuration.Settings.setting;

/**
 * This makes F2 available to Neo4j, assuming neo can find it on the classpath.
 */
public class F2Factory extends Locks.Factory
{

    public static final Setting<Integer> numPartitions = setting( "unsupported.dbms.f2.partitions", Settings.INTEGER, "128" );

    public F2Factory()
    {
        super( "f2" );
    }

    @Override
    public Locks newInstance( Config config, Clock clocks, ResourceType[] resourceTypes )
    {
        OptionalInt maxResourceIndex = Stream.of( resourceTypes ).mapToInt( ResourceType::typeId ).max();
        if ( !maxResourceIndex.isPresent() )
        {
            throw new IllegalStateException( "There needs to be at least one lock resource type to user the F2 lock manager." );
        }
        return new F2Locks( maxResourceIndex.getAsInt(), config.get( numPartitions ) );
    }
}
