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
package org.neo4j.proc.jar;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.neo4j.collection.PrefetchingRawIterator;
import org.neo4j.collection.RawIterator;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.proc.Procedure;
import org.neo4j.proc.ReflectiveProcedureCompiler;

/**
 * Loads
 */
public class ProcedureJarLoader
{
    private final ReflectiveProcedureCompiler compiler;

    public ProcedureJarLoader( ReflectiveProcedureCompiler compiler )
    {
        this.compiler = compiler;
    }

    public List<Procedure> loadProcedures( URL jar ) throws IOException, KernelException
    {
        LinkedList<Procedure> procedures = new LinkedList<>();
        RawIterator<Class<?>,IOException> classes = listClassesIn( jar );
        while(classes.hasNext())
        {
            Class<?> next = classes.next();
            procedures.addAll( compiler.compile( next ) );
        }
        return procedures;
    }

    private RawIterator<Class<?>, IOException> listClassesIn( URL jar ) throws IOException
    {
        ZipInputStream zip = new ZipInputStream(jar.openStream());

        return new PrefetchingRawIterator<Class<?>, IOException>()
        {
            @Override
            protected Class<?> fetchNextOrNull() throws IOException
            {
                while(true)
                {
                    ZipEntry nextEntry = zip.getNextEntry();
                    if ( nextEntry == null )
                    {
                        zip.close();
                        return null;
                    }

                    String name = nextEntry.getName();
                    if ( name.endsWith( ".class" ) )
                    {
                        String className = name.substring( 0, name.length() - ".class".length() ).replace( "/", "." );

                        try
                        {
                            return Class.forName( className );
                        }
                        catch ( ClassNotFoundException e1 )
                        {
                            throw new IOException( "Unable to load class `" + className + "` from jarfile `" + jar + "`", e1 );
                        }
                    }
                }
            }
        };
    }
}
