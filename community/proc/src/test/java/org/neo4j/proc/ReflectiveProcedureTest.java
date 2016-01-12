package org.neo4j.proc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.stream.Stream;

import org.neo4j.kernel.api.exceptions.ProcedureException;

import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo4j.proc.ProcedureSignature.procedureSignature;

public class ReflectiveProcedureTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldCompileProcedure() throws Throwable
    {
        // When
        List<Procedure> procedures = new ReflectiveProcedures().compile( SingleReadOnlyProcedure.class );

        // Then
        assertEquals( 1, procedures.size() );
        assertThat( procedures.get( 0 ).signature(), equalTo(
                procedureSignature( "org", "neo4j", "proc", "listCoolPeople" )
                        .out( "name", Neo4jTypes.NTString )
                        .build() ));
    }

    @Test
    public void shouldRunSimpleReadOnlyProcedure() throws Throwable
    {
        // Given
        Procedure proc = new ReflectiveProcedures().compile( SingleReadOnlyProcedure.class ).get( 0 );

        // When
        Stream<Object[]> out = proc.apply( new Procedure.BasicContext(), new Object[0] );

        // Then
        assertThat( out.collect( toList() ), contains(
            new Object[]{"Bonnie"},
            new Object[]{"Clyde"}
        ));
    }

    @Test
    public void shouldGiveHelpfulErrorOnConstructorThatRequiresArgument() throws Throwable
    {
        // Expect
        exception.expect( ProcedureException.class );
        exception.expectMessage( "Unable to find a usable public no-argument constructor in the class `WierdConstructorProcedure`. " +
                                 "Please add a valid, public constructor, recompile the class and try again." );

        // When
        new ReflectiveProcedures().compile( WierdConstructorProcedure.class );
    }

    @Test
    public void shouldGiveHelpfulErrorOnNoPublicConstructor() throws Throwable
    {
        // Expect
        exception.expect( ProcedureException.class );
        exception.expectMessage( "Unable to find a usable public no-argument constructor in the class `PrivateConstructorProcedure`. " +
                                 "Please add a valid, public constructor, recompile the class and try again." );

        // When
        new ReflectiveProcedures().compile( PrivateConstructorProcedure.class );
    }

    public static class MyOutputRecord
    {
        public String name;

        public MyOutputRecord( String name )
        {
            this.name = name;
        }
    }

    public static class SingleReadOnlyProcedure
    {
        @ReadOnlyProcedure
        public Stream<MyOutputRecord> listCoolPeople() {
            return Stream.of(
                    new MyOutputRecord( "Bonnie" ),
                    new MyOutputRecord( "Clyde" ));
        }
    }

    public static class WierdConstructorProcedure
    {
        public WierdConstructorProcedure( WierdConstructorProcedure wat )
        {

        }

        @ReadOnlyProcedure
        public Stream<MyOutputRecord> listCoolPeople() {
            return Stream.of(
                    new MyOutputRecord( "Bonnie" ),
                    new MyOutputRecord( "Clyde" ));
        }
    }

    public static class PrivateConstructorProcedure
    {
        private PrivateConstructorProcedure()
        {

        }

        @ReadOnlyProcedure
        public Stream<MyOutputRecord> listCoolPeople() {
            return Stream.of(
                    new MyOutputRecord( "Bonnie" ),
                    new MyOutputRecord( "Clyde" ));
        }
    }
}
