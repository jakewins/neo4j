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
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.proc.jar.ProcedureJarLoader;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.neo4j.proc.Neo4jTypes.NTInteger;
import static org.neo4j.proc.ProcedureSignature.procedureSignature;

public class ProcedureJarLoaderTest
{
    @Rule public TemporaryFolder tmpdir = new TemporaryFolder();
    @Rule public ExpectedException exception = ExpectedException.none();

    private final ProcedureJarLoader jarloader = new ProcedureJarLoader( new ReflectiveProcedureCompiler() );

    @Test
    public void shouldLoadProcedureFromJar() throws Throwable
    {
        // Given
        URL jar = createJarFor( ClassWithOneProcedure.class );

        // When
        List<Procedure> procedures = jarloader.loadProcedures( jar );

        // Then
        List<ProcedureSignature> signatures = procedures.stream().map( Procedure::signature ).collect( toList() );
        assertThat( signatures, contains(
                procedureSignature( "org","neo4j", "proc", "myProcedure" ).out( "someNumber", NTInteger ).build() ));

        assertThat( procedures.get( 0 ).apply( new Procedure.BasicContext(), new Object[0] ).collect( toList() ),
                contains( equalTo( new Object[]{1337L} )) );
    }

    @Test
    public void shouldLoadProcedureFromJarWithMultipleProcedureClasses() throws Throwable
    {
        // Given
        URL jar = createJarFor( ClassWithOneProcedure.class, AnotherProcedure.class, ClassWithNoProcedureAtAll.class );

        // When
        List<Procedure> procedures = jarloader.loadProcedures( jar );

        // Then
        List<ProcedureSignature> signatures = procedures.stream().map( Procedure::signature ).collect( toList() );
        assertThat( signatures, containsInAnyOrder(
                procedureSignature( "org","neo4j", "proc", "myOtherProcedure" ).out( "someNumber", NTInteger ).build(),
                procedureSignature( "org","neo4j", "proc", "myProcedure" ).out( "someNumber", NTInteger ).build() ));
    }

    @Test
    public void shouldGiveHelpfulErrorOnInvalidProcedure() throws Throwable
    {
        // Given
        URL jar = createJarFor( ClassWithOneProcedure.class, ClassWithInvalidProcedure.class );

        // Expect
        exception.expect( ProcedureException.class );
        exception.expectMessage( "A procedure must return a `java.util.stream.Stream`, `ClassWithInvalidProcedure.booleansAreNotAcceptableReturnTypes` returns `boolean`." );

        // When
        jarloader.loadProcedures( jar );
    }

    private URL createJarFor( Class<?> ... targets ) throws IOException
    {
        File f =  tmpdir.newFile( "myJar.jar" );
        try( FileOutputStream fout = new FileOutputStream( f );
             JarOutputStream jarOut = new JarOutputStream(fout) )
        {
            for ( Class<?> target : targets )
            {
                String fileName = target.getName().replace( ".", "/" ) + ".class";
                jarOut.putNextEntry( new ZipEntry( fileName ) );
                jarOut.write( classCompiledBytes( fileName ) );
                jarOut.closeEntry();
            }
        }
        return f.toURI().toURL();
    }

    private byte[] classCompiledBytes( String fileName ) throws IOException
    {
        InputStream in = getClass().getClassLoader().getResourceAsStream( fileName );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }

        return out.toByteArray();
    }

    public static class Output
    {
        public int someNumber = 1337;
    }

    public static class ClassWithInvalidProcedure
    {
        @ReadOnlyProcedure
        public boolean booleansAreNotAcceptableReturnTypes()
        {
            return false;
        }
    }

    public static class ClassWithOneProcedure
    {
        @ReadOnlyProcedure
        public Stream<Output> myProcedure()
        {
            return Stream.of( new Output() );
        }
    }

    public static class ClassWithNoProcedureAtAll
    {
        void thisMethodIsEntirelyUnrelatedToAllThisExcitement()
        {

        }
    }

    public static class AnotherProcedure
    {
        @ReadOnlyProcedure
        public Stream<Output> myOtherProcedure()
        {
            return Stream.of( new Output() );
        }
    }
}
