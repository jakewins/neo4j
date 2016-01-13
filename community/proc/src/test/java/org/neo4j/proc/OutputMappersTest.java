/*
 * Copyright (c) 2002-2016 "Neo Technology,"
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
package org.neo4j.proc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.proc.OutputMappers.OutputMapper;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class OutputMappersTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    public static class SingleStringFieldRecord
    {
        public String name;

        public SingleStringFieldRecord( String name )
        {
            this.name = name;
        }
    }

    public static class UnmappableRecord
    {
        public UnmappableRecord wat;
    }

    public static class RecordWithPrivateField
    {
        private String wat;
    }

    public static class RecordWithStaticFields
    {
        public static String skipMePublic;
        public String includeMe;
        private static String skipMePrivate;

        public RecordWithStaticFields( String val )
        {
            this.includeMe = val;
        }
    }

    @Test
    public void shouldMapSimpleRecordWithString() throws Throwable
    {
        // When
        OutputMapper mapper = new OutputMappers().mapper( SingleStringFieldRecord.class );

        // Then
        assertThat(
            mapper.signature(),
            contains( new ProcedureSignature.FieldSignature( "name", Neo4jTypes.NTString ) )
        );
        assertThat(
            asList( mapper.apply( new SingleStringFieldRecord( "hello, world!" ) ) ),
            contains( "hello, world!" )
        );
    }

    @Test
    public void shouldSkipStaticFields() throws Throwable
    {
        // When
        OutputMapper mapper = new OutputMappers().mapper( RecordWithStaticFields.class );

        // Then
        assertThat(
                mapper.signature(),
                contains( new ProcedureSignature.FieldSignature( "includeMe", Neo4jTypes.NTString ) )
        );
        assertThat(
                asList( mapper.apply( new RecordWithStaticFields( "hello, world!" ) ) ),
                contains( "hello, world!" )
        );
    }

    @Test
    public void shouldGiveHelpfulErrorOnUnmappable() throws Throwable
    {
        // Expect
        exception.expect( ProcedureException.class );
        exception.expectMessage( "Field `wat` in record `UnmappableRecord` cannot be converted to a Neo4j type: Don't know how to map `class org.neo4j.proc.OutputMappersTest$UnmappableRecord`" );

        // When
        new OutputMappers().mapper( UnmappableRecord.class );
    }

    @Test
    public void shouldGiveHelpfulErrorOnPrivateField() throws Throwable
    {
        // Expect
        exception.expect( ProcedureException.class );
        exception.expectMessage( "Field `wat` in record `RecordWithPrivateField` cannot be accessed. Please ensure the field is marked as `public`." );

        // When
        new OutputMappers().mapper( RecordWithPrivateField.class );
    }
}