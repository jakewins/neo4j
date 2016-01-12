package org.neo4j.proc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.proc.ClassRecordMappers.ClassRecordMapper;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class ClassRecordMappersTest
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
        ClassRecordMapper mapper = new ClassRecordMappers().mapper( SingleStringFieldRecord.class );

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
        ClassRecordMapper mapper = new ClassRecordMappers().mapper( RecordWithStaticFields.class );

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
        exception.expectMessage( "Field `wat` in record `UnmappableRecord` cannot be converted to a Neo4j type: Don't know how to map `class org.neo4j.proc.ClassRecordMappersTest$UnmappableRecord`" );

        // When
        new ClassRecordMappers().mapper( UnmappableRecord.class );
    }

    @Test
    public void shouldGiveHelpfulErrorOnPrivateField() throws Throwable
    {
        // Expect
        exception.expect( ProcedureException.class );
        exception.expectMessage( "Field `wat` in record `RecordWithPrivateField` cannot be accessed. Please ensure the field is marked as `public`." );

        // When
        new ClassRecordMappers().mapper( RecordWithPrivateField.class );
    }
}