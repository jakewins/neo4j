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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.proc.InputMappers.InputMapper;
import org.neo4j.proc.OutputMappers.OutputMapper;
import org.neo4j.proc.ProcedureSignature.ProcedureName;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Handles converting a class into one or more callable {@link Procedure}.
 */
public class ReflectiveProcedureCompiler
{
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final OutputMappers outputMappers = new OutputMappers();
    private final InputMappers inputMappers = new InputMappers();

    public List<Procedure> compile( Class<?> procDefinition ) throws KernelException
    {
        try
        {
            List<Method> procedureMethods = asList( procDefinition.getDeclaredMethods() ).stream()
                    .filter( m -> m.isAnnotationPresent( ReadOnlyProcedure.class ) )
                    .collect( Collectors.toList() );

            if( procedureMethods.isEmpty() )
            {
                return emptyList();
            }

            MethodHandle constructor = constructor( procDefinition );

            ArrayList<Procedure> out = new ArrayList<>( procedureMethods.size() );
            for ( Method method : procedureMethods )
            {
                out.add( compileProcedure( procDefinition, constructor, method ) );
            }
            out.sort( (a,b) -> a.signature().name().toString().compareTo( b.signature().name().toString() ) );
            return out;
        }
        catch( KernelException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new ProcedureException( Status.Procedure.FailedRegistration, e,
                    "Failed to compile procedure defined in `%s`: %s", procDefinition.getSimpleName(), e.getMessage() );
        }
    }

    private ReflectiveProcedure compileProcedure( Class<?> procDefinition, MethodHandle constructor, Method method )
            throws ProcedureException, IllegalAccessException
    {
        ProcedureName procName = extractName( procDefinition, method );

        InputMapper inputMapper = inputMappers.mapper( method );
        OutputMapper outputMapper = outputMappers.mapper( method );
        MethodHandle procedureMethod = lookup.unreflect( method );

        ProcedureSignature signature = new ProcedureSignature( procName, inputMapper.signature(), outputMapper.signature() );

        return new ReflectiveProcedure( signature, constructor, procedureMethod, inputMapper, outputMapper );
    }

    private MethodHandle constructor( Class<?> procDefinition ) throws ProcedureException
    {
        try
        {
            return lookup.unreflectConstructor( procDefinition.getConstructor() );
        }
        catch ( IllegalAccessException | NoSuchMethodException e )
        {
            throw new ProcedureException( Status.Procedure.FailedRegistration, e, "Unable to find a usable public no-argument constructor in the class `%s`. " +
                                                                                  "Please add a valid, public constructor, recompile the class and try again.",
                    procDefinition.getSimpleName() );
        }
    }

    private ProcedureName extractName( Class<?> procDefinition, Method m )
    {
        String[] namespace = procDefinition.getPackage().getName().split( "\\." );
        String name = m.getName();
        return new ProcedureName( namespace, name );
    }

    private static class ReflectiveProcedure implements Procedure
    {
        private final ProcedureSignature signature;
        private final MethodHandle constructor;
        private final MethodHandle procedureMethod;
        private final InputMapper inputMapper;
        private final OutputMapper outputMapper;


        public ReflectiveProcedure( ProcedureSignature signature, MethodHandle constructor,
                MethodHandle procedureMethod, InputMapper inputMapper, OutputMapper outputMapper )
        {
            this.signature = signature;
            this.constructor = constructor;
            this.procedureMethod = procedureMethod;
            this.inputMapper = inputMapper;
            this.outputMapper = outputMapper;
        }

        @Override
        public ProcedureSignature signature()
        {
            return signature;
        }

        @Override
        public Stream<Object[]> apply( Context ctx, Object[] input ) throws ProcedureException
        {
            // For now, create a new instance of the class for each invocation. In the future, we'd like to keep instances local to
            // at least the executing session, but we don't yet have good interfaces to the kernel to model that with.
            try
            {
                Object cls = constructor.invoke();
                Object[] args = new Object[signature.inputSignature().size() + 1];
                args[0] = cls;
                //in place converts to correct type
                inputMapper.apply( input );
                System.arraycopy( input, 0, args, 1, input.length );
                Stream<?> out = (Stream<?>) procedureMethod.invokeWithArguments( args );
                return out.map( outputMapper::apply );
            }
            catch ( Throwable throwable )
            {
                throw new ProcedureException( Status.Procedure.CallFailed, throwable, "Failed to invoke procedure." ); // TODO
            }
        }
    }
}
