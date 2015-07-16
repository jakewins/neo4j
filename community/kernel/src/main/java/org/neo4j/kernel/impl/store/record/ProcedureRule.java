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
package org.neo4j.kernel.impl.store.record;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.procedure.ProcedureDescriptor;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.impl.store.Neo4jTypes;
import org.neo4j.kernel.impl.store.UnderlyingStorageException;

import static org.neo4j.helpers.Pair.pair;
import static org.neo4j.helpers.UTF8.getDecodedStringFrom;
import static org.neo4j.helpers.UTF8.putEncodedStringInto;
import static org.neo4j.kernel.api.procedure.ProcedureDescriptor.Mode.READ_ONLY;
import static org.neo4j.kernel.api.procedure.ProcedureDescriptor.Mode.UPDATE;
import static org.neo4j.kernel.impl.store.Neo4jTypes.AnyType;

public class ProcedureRule extends AbstractSchemaRule
{
    // TODO: Tests that test lots of permutations of descriptors, focused on the extremes

    private static final int MODE = 0b00000001;

    private final ProcedureDescriptor descriptor;

    public ProcedureRule( long id, ProcedureDescriptor descriptor )
    {
        super( id, 0, Kind.PROCEDURE );
        this.descriptor = descriptor;
    }

    public static ProcedureRule readProcedureRule( long id, ByteBuffer buffer )
    {
        int flags = buffer.get();

        ProcedureDescriptor.Mode mode = (flags & MODE) == 0 ? READ_ONLY : UPDATE;

        String[] nameSpaces = readStringArray( buffer );
        String name = getDecodedStringFrom( buffer );
        String lang = getDecodedStringFrom( buffer );

        List<Pair<String,AnyType>> inArgs = readFieldList(buffer);
        List<Pair<String,AnyType>> outArgs = readFieldList(buffer);

        String body = getDecodedStringFrom( buffer );

        return new ProcedureRule( id, new ProcedureDescriptor(
                new ProcedureSignature( nameSpaces, name, inArgs, outArgs ), lang, mode, body ));
    }

    public ProcedureDescriptor descriptor()
    {
        return descriptor;
    }

    @Override
    public int length()
    {
        return serialize().length + super.length();
    }

    @Override
    public void serialize( ByteBuffer target )
    {
        super.serialize( target );
        byte[] serialized = serialize();
        target.put( serialized, 0, serialized.length );
    }

    // This is clearly not very efficient - but this is a slow path dealing with schema modifications, it seems
    // silly to try and shave off microseconds and gc here.
    private byte[] serialize()
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutput out = new DataOutputStream( baos );

            // Flags
            out.writeByte( descriptor.mode() == UPDATE ? MODE : 0 );

            writeStringArray( out, descriptor.signature().getNamespace() );
            putEncodedStringInto( descriptor.signature().getName(), out );
            putEncodedStringInto( descriptor.language(), out );

            writeFieldList( descriptor.signature().getInputSignature(), out );
            writeFieldList( descriptor.signature().getOutputSignature(), out );

            putEncodedStringInto( descriptor.procedureBody(), out );

            return baos.toByteArray();
        }
        catch( IOException e )
        {
            throw new UnderlyingStorageException( "Failed to serialize procedure rule: " + descriptor.toString(), e );
        }
    }

    private void writeFieldList( List<Pair<String,AnyType>> fields, DataOutput out ) throws IOException
    {
        out.writeShort( fields.size() );
        for ( Pair<String,AnyType> field : fields )
        {
            putEncodedStringInto( field.first(), out );
            writeType( field.other(), out );
        }
    }

    private void writeType( AnyType type, DataOutput out ) throws IOException
    {
        out.writeByte( type.ordinal() );
        if( type.ordinal() ==  Neo4jTypes.ORD_LIST)
        {
            writeType( ((Neo4jTypes.ListType)type).valueType(), out );
        }
    }

    private void writeStringArray( DataOutput out, String[] namespace ) throws IOException
    {
        out.writeShort( namespace.length );
        for ( int i = 0; i < namespace.length; i++ )
        {
            putEncodedStringInto( namespace[i], out );
        }
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + descriptor.hashCode();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        if ( !super.equals( o ) )
        {
            return false;
        }

        ProcedureRule that = (ProcedureRule) o;

        if ( descriptor != null ? !descriptor.equals( that.descriptor ) : that.descriptor != null )
        {
            return false;
        }

        return true;
    }

    @Override
    protected String innerToString()
    {
        return ", descriptor=" + descriptor;
    }

    private static List<Pair<String,AnyType>> readFieldList( ByteBuffer buffer )
    {
        int fieldCount = buffer.getShort() & 0xffff;

        List<Pair<String, AnyType>> fields = new ArrayList<>( fieldCount );
        for ( int i = 0; i < fieldCount; i++ )
        {
            String fieldName = getDecodedStringFrom( buffer );
            AnyType type = readType( buffer );
            fields.add( pair( fieldName, type ) );
        }
        return fields;
    }

    private static AnyType readType( ByteBuffer buffer )
    {
        int typeOrdinal = buffer.get() & 0xff;
        switch( typeOrdinal )
        {
        case Neo4jTypes.ORD_ANY: return Neo4jTypes.NTAny;
        case Neo4jTypes.ORD_TEXT: return Neo4jTypes.NTText;
        case Neo4jTypes.ORD_NUMBER: return Neo4jTypes.NTNumber;
        case Neo4jTypes.ORD_INTEGER: return Neo4jTypes.NTInteger;
        case Neo4jTypes.ORD_FLOAT: return Neo4jTypes.NTFloat;
        case Neo4jTypes.ORD_BOOLEAN: return Neo4jTypes.NTBoolean;
        case Neo4jTypes.ORD_LIST:
            AnyType subType = readType( buffer );
            return Neo4jTypes.NTList( subType );
        case Neo4jTypes.ORD_MAP: return Neo4jTypes.NTMap;
        case Neo4jTypes.ORD_NODE: return Neo4jTypes.NTNode;
        case Neo4jTypes.ORD_RELATIONSHIP: return Neo4jTypes.NTRelationship;
        case Neo4jTypes.ORD_PATH : return Neo4jTypes.NTPath;
        }
        throw new UnderlyingStorageException( "Unknown type ordinal: " + typeOrdinal );
    }

    private static String[] readStringArray( ByteBuffer buffer )
    {
        int numStrings = buffer.getShort() & 0xffff;
        String[] strings = new String[numStrings];
        for ( int i = 0; i < numStrings; i++ )
        {
            strings[i] = getDecodedStringFrom( buffer );
        }
        return strings;
    }
}
