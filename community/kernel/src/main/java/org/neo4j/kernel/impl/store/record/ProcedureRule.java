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
package org.neo4j.kernel.impl.store.record;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.procedure.ProcedureDescriptor;
import org.neo4j.kernel.api.procedure.ProcedureSignature;

import static org.neo4j.helpers.UTF8.getDecodedStringFrom;
import static org.neo4j.kernel.api.procedure.ProcedureDescriptor.Mode.READ_ONLY;
import static org.neo4j.kernel.api.procedure.ProcedureDescriptor.Mode.UPDATE;

public class ProcedureRule extends AbstractSchemaRule
{
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

        List<Pair<String,ProcedureSignature.Neo4jType>> inArgs = readFieldList(buffer);
        List<Pair<String,ProcedureSignature.Neo4jType>> outArgs = readFieldList(buffer);

        ByteArrayInputStream body = readProcedureBody( buffer );

        return new ProcedureRule( id, new ProcedureDescriptor(
                new ProcedureSignature( nameSpaces, name, inArgs, outArgs ), lang, mode,
                body ));
    }

    private static ByteArrayInputStream readProcedureBody( ByteBuffer buffer )
    {
        // We currently store the whole body directly in the schema record. The flags in the record
        // are meant to be used to alter this if we want to swap to a blob storage approach in the future
        int size = buffer.getInt();
        byte[] body = new byte[size];
        buffer.get( body );
        return new ByteArrayInputStream( body );
    }

    private static List<Pair<String,ProcedureSignature.Neo4jType>> readFieldList( ByteBuffer buffer )
    {
        int fieldCount = buffer.getShort() & 0xffff;

        for ( int i = 0; i < fieldCount; i++ )
        {
            String fieldName = getDecodedStringFrom( buffer );

        }

        return null;
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

    public ProcedureDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public int length()
    {
        return   1;
    }

    @Override
    public void serialize( ByteBuffer target )
    {
//        super.serialize( target );
//        UTF8.putEncodedStringInto( providerDescriptor.getKey(), target );
//        UTF8.putEncodedStringInto( providerDescriptor.getVersion(), target );
//        target.putShort( (short) 1 /*propertyKeys.length*/ );
//        target.putLong( propertyKey );
//        if ( isConstraintIndex() )
//        {
//            target.putLong( owningConstraint );
//        }
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
}
