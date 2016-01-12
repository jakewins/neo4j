package org.neo4j.proc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.util.List;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.proc.ProcedureSignature.FieldSignature;
import org.neo4j.proc.TypeMappers.ToNeoValue;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Takes user-defined record classes, and does two things: Describe the class as a {@link ProcedureSignature}, and provide a mechanism to convert
 * an instance of the class to Neo4j-typed Object[].
 */
public class ClassRecordMappers
{
    /**
     * A compiled mapper, takes an instance of a java class, and converts it to an Object[] matching the specified {@link #signature()}.
     */
    public static class ClassRecordMapper
    {
        private final List<FieldSignature> signature;
        private final FieldMapper[] fieldMappers;

        public ClassRecordMapper( FieldSignature[] signature, FieldMapper[] fieldMappers )
        {
            this.signature = asList( signature );
            this.fieldMappers = fieldMappers;
        }

        public List<FieldSignature> signature()
        {
            return signature;
        }

        public Object[] apply( Object record )
        {
            Object[] output = new Object[fieldMappers.length];
            for ( int i = 0; i < fieldMappers.length; i++ )
            {
                output[i] = fieldMappers[i].apply( record );
            }
            return output;
        }
    }

    /**
     * Extracts field value from an instance and converts it to a Neo4j typed value.
     */
    private static class FieldMapper
    {
        private final MethodHandle getter;
        private final ToNeoValue mapper;

        public FieldMapper( MethodHandle getter, ToNeoValue mapper )
        {
            this.getter = getter;
            this.mapper = mapper;
        }

        Object apply( Object record )
        {
            try
            {
                return mapper.apply( getter.invoke( record ) );
            }
            catch ( Throwable throwable )
            {
                throw new AssertionError( throwable ); // TODO test and better error
            }
        }
    }

    private final Lookup lookup = MethodHandles.lookup();
    private final TypeMappers typeMappers = new TypeMappers();

    public ClassRecordMapper mapper( Class<?> userClass ) throws ProcedureException
    {
        List<Field> fields = instanceFields( userClass );
        FieldSignature[] signature = new FieldSignature[fields.size()];
        FieldMapper[] fieldMappers = new FieldMapper[fields.size()];

        for ( int i = 0; i < fields.size(); i++ )
        {
            Field field = fields.get( i );
            if( !isPublic( field.getModifiers() ))
            {
                throw new ProcedureException( Status.Procedure.TypeError, "Field `%s` in record `%s` cannot be accessed. Please ensure the field is marked as `public`.", field.getName(), userClass.getSimpleName() );
            }

            try
            {
                ToNeoValue mapper = typeMappers.javaToNeo( field.getType() );
                MethodHandle getter = lookup.unreflectGetter( field );
                FieldMapper fieldMapper = new FieldMapper(getter, mapper);

                fieldMappers[i] = fieldMapper;
                signature[i] = new FieldSignature( field.getName(), mapper.type() );
            }
            catch( ProcedureException e )
            {
                throw new ProcedureException( e.status(), e, "Field `%s` in record `%s` cannot be converted to a Neo4j type: %s", field.getName(), userClass.getSimpleName(), e.getMessage() );
            }
            catch ( IllegalAccessException e )
            {
                throw new ProcedureException( Status.Procedure.TypeError, e, "Field `%s` in record `%s` cannot be accessed: %s", field.getName(), userClass.getSimpleName(), e.getMessage() );
            }
        }

        return new ClassRecordMapper( signature, fieldMappers );
    }

    private List<Field> instanceFields( Class<?> userClass )
    {
        return asList( userClass.getDeclaredFields()).stream().filter( (f) -> !isStatic( f.getModifiers() ) ).collect( toList() );
    }
}
