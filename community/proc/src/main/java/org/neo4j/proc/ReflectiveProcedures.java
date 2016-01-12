package org.neo4j.proc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.proc.ClassRecordMappers.ClassRecordMapper;
import org.neo4j.proc.ProcedureSignature.ProcedureName;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Handles converting a class into one or more callable {@link Procedure}.
 */
public class ReflectiveProcedures
{
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final ClassRecordMappers recordMappers = new ClassRecordMappers();

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
                ProcedureName procName = extractName( procDefinition, method );
                ClassRecordMapper outputMapper = outputMapper( procDefinition, method );
                MethodHandle procedureMethod = lookup.unreflect( method );

                ProcedureSignature signature = new ProcedureSignature( procName, emptyList(), outputMapper.signature() );

                out.add( new ReflectiveProcedure( signature, constructor, procedureMethod, outputMapper ) );
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
            throw new ProcedureException( Status.Procedure.FailedRegistration, e, "Failed to compile procedure defined in `%s`: %s", procDefinition.getSimpleName(), e.getMessage() );
        }
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

    private ClassRecordMapper outputMapper( Class<?> procDefinition, Method method ) throws ProcedureException
    {
        Class<?> cls = method.getReturnType();
        if( cls != Stream.class )
        {
            throw new RuntimeWrappedKernelException( new ProcedureException( Status.Procedure.FailedRegistration,
                    "A procedure must return a `java.util.stream.Stream`, `%s.%s` returns `%s`.",
                    procDefinition.getSimpleName(), method.getName(), cls.getSimpleName() ) );
        }

        ParameterizedType genType = (ParameterizedType) method.getGenericReturnType();
        Type recordType = genType.getActualTypeArguments()[0];

        return recordMappers.mapper( (Class<?>) recordType );
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
        private final ClassRecordMapper outputMapper;

        public ReflectiveProcedure( ProcedureSignature signature, MethodHandle constructor, MethodHandle procedureMethod, ClassRecordMapper outputMapper )
        {
            this.signature = signature;
            this.constructor = constructor;
            this.procedureMethod = procedureMethod;
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
                Stream<?> out = (Stream<?>) procedureMethod.invoke( cls );
                return out.map( outputMapper::apply );
            }
            catch ( Throwable throwable )
            {
                throw new ProcedureException( Status.Procedure.CallFailed, throwable, "Failed to invoke procedure." ); // TODO
            }
        }
    }
}
