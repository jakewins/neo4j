/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.kernel.impl.branch;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.neo4j.helpers.Provider;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.api.properties.DefinedProperty;
import org.neo4j.kernel.impl.core.LabelTokenHolder;
import org.neo4j.kernel.impl.core.PropertyKeyTokenHolder;
import org.neo4j.kernel.impl.core.RelationshipTypeToken;
import org.neo4j.kernel.impl.core.RelationshipTypeTokenHolder;
import org.neo4j.kernel.impl.core.Token;
import org.neo4j.kernel.impl.core.TokenHolder;
import org.neo4j.kernel.impl.core.TokenNotFoundException;
import org.neo4j.kernel.impl.nioneo.store.NodeRecord;
import org.neo4j.kernel.impl.nioneo.store.PropertyBlock;
import org.neo4j.kernel.impl.nioneo.store.PropertyStore;
import org.neo4j.kernel.impl.nioneo.store.labels.NodeLabelsField;
import org.neo4j.kernel.impl.nioneo.xa.command.Command;
import org.neo4j.kernel.impl.transaction.XaDataSourceManager;
import org.neo4j.kernel.impl.transaction.xaframework.XaCommand;

/**
 * Takes deserialized transactions and attempts to describe them in a human-readable way, for instance by printing
 * property values in full and resolving types, labels and property keys to their human names.
 */
public class FriendlyTxDescriber
{
    private final GraphDatabaseAPI db;
    private final TokenHolder<Token> labels;
    private final TokenHolder<Token> propertyKeys;
    private final TokenHolder<RelationshipTypeToken> relTypes;
    private final Provider<PropertyStore> propStore;

    public FriendlyTxDescriber(final GraphDatabaseAPI db)
    {
        this.db = db;
        labels = db.getDependencyResolver().resolveDependency( LabelTokenHolder.class );
        propertyKeys = db.getDependencyResolver().resolveDependency( PropertyKeyTokenHolder.class );
        relTypes = db.getDependencyResolver().resolveDependency( RelationshipTypeTokenHolder.class );
        propStore = new Provider<PropertyStore>()
        {
            @Override
            public PropertyStore instance()
            {
                return db.getDependencyResolver().resolveDependency( XaDataSourceManager.class ).getNeoStoreDataSource().getNeoStore().getPropertyStore();
            }
        };
    }

    public String describe( Tx transaction )
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final FriendlyIdentifiers identifiers = new FriendlyIdentifiers();
        final PrintWriter out = new PrintWriter( baos );

        transaction.visit( new Tx.Visitor(){
            @Override
            public void node( Command.NodeCommand command )
            {
                String entity = identifiers.node( command.getBefore().getId() );
                if(!command.getBefore().inUse())
                {
                    out.println("CREATE (" + entity + labels(command.getAfter()) + ")" );
                }
                else if(command.getAfter().inUse())
                {
                    out.println("MATCH (" + entity + labels(command.getAfter()) + ") WHERE id(" + entity +")=" + command.getBefore().getId());
                }
            }

            @Override
            public void property( Command.PropertyCommand command )
            {
                long nodeId = command.getNodeId();
                long relId = command.getRelId();
                String entity;
                if(nodeId == -1)
                {
                    entity = identifiers.rel( relId );
                }
                else
                {
                    entity = identifiers.node( nodeId );
                }

                for ( PropertyBlock block : command.getAfter().getPropertyBlocks() )
                {
                    DefinedProperty prop = block.getType().readProperty( block.getKeyIndexId(), block,
                            propStore );
                    out.println("SET " + entity + "." + propertyKey( block.getKeyIndexId() ) + " = " + propToCypherString( prop ) );
                }
            }

            @Override
            public void unknown( XaCommand command )
            {
                out.println("Unknown: " + command);
            }
        } );
        out.flush();
        return baos.toString();
    }

    private String propToCypherString( DefinedProperty prop )
    {
        return prop.valueAsString();
    }

    private String propertyKey( int keyId )
    {
        try
        {
            return propertyKeys.getTokenById( keyId ).name();
        }
        catch ( TokenNotFoundException e )
        {
            return "PropertyKey[" + keyId + "]";
        }
    }

    private String labels( NodeRecord nodeRecord )
    {
        StringBuilder sb = new StringBuilder();
        for ( long l : NodeLabelsField.parseLabelsField( nodeRecord ).getIfLoaded() )
        {
            try
            {
                sb.append( ":" ).append( labels.getTokenById( (int) l ).name() );
            }
            catch ( TokenNotFoundException e )
            {
                sb.append( ":Label[" ).append( l ).append( "]" );
            }
        }

        return sb.toString();
    }
}
