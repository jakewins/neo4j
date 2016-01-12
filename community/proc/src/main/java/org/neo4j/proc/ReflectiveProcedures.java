package org.neo4j.proc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;

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
            MethodHandle constructor = lookup.unreflectConstructor( procDefinition.getConstructor() );

            return asList(procDefinition.getDeclaredMethods()).stream()
                    .filter( m -> m.isAnnotationPresent( ReadOnlyProcedure.class ) )
                    .map( (method) -> {
                        try
                        {
                            ProcedureSignature.ProcedureName procName = extractName( procDefinition, method );

                            MethodHandle handle = lookup.unreflect( method );

                            Class<?> cls = method.getReturnType();
                            if( cls != Stream.class )
                            {
                                throw new RuntimeWrappedException( new ProcedureException( Status.Procedure.FailedRegistration,
                                        "A procedure must return a `java.util.stream.Stream`, `%s.%s` returns `%s`.",
                                        procDefinition.getSimpleName(), method.getName(), cls.getSimpleName() ) );
                            }

                            try
                            {
                                ParameterizedType genType = (ParameterizedType) method.getGenericReturnType();
                                Type recordType = genType.getActualTypeArguments()[0];

                                ClassRecordMappers.ClassRecordMapper outputMapper = recordMappers.mapper( (Class<?>) recordType );

                                ProcedureSignature signature = new ProcedureSignature( procName, emptyList(), outputMapper.signature() );

                                return new Procedure()
                                {
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
                                            return null;
                                        }
                                        catch ( Throwable throwable )
                                        {
                                            throw new ProcedureException( Status.Procedure.CallFailed, throwable, "Failed to invoke procedure." ); // TODO
                                        }
                                    }
                                };
                            }
                            catch ( ProcedureException e )
                            {
                                throw new RuntimeWrappedException( e );
                            }
                        }
                        catch ( IllegalAccessException e )
                        {
                            throw new AssertionError( e );
                        }
                    }).collect( Collectors.toList() );
        }
        catch( RuntimeWrappedException e )
        {
            throw e.unwrap();
        }
        catch ( Exception e )
        {
            throw new ProcedureException( Status.Procedure.FailedRegistration, e, "Failed to compile procedure defined in `%s`: %s", procDefinition.getSimpleName(), e.getMessage() );
        }
    }

    private ProcedureSignature.ProcedureName extractName( Class<?> procDefinition, Method m )
    {
        String[] namespace = procDefinition.getPackage().getName().split( "\\." );
        String name = m.getName();
        return new ProcedureSignature.ProcedureName( namespace, name );
    }
}
