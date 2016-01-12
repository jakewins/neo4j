package org.neo4j.proc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.proc.Neo4jTypes.AnyType;

import static org.neo4j.proc.Neo4jTypes.NTAny;
import static org.neo4j.proc.Neo4jTypes.NTBoolean;
import static org.neo4j.proc.Neo4jTypes.NTFloat;
import static org.neo4j.proc.Neo4jTypes.NTInteger;
import static org.neo4j.proc.Neo4jTypes.NTList;
import static org.neo4j.proc.Neo4jTypes.NTMap;
import static org.neo4j.proc.Neo4jTypes.NTNumber;
import static org.neo4j.proc.Neo4jTypes.NTString;

public class TypeMappers
{
    /**
     * Converts a java object to the specified {@link #type() neo4j type}. In practice, this is often the same java object - but this gives a guarantee
     * that only java objects Neo4j can digest are outputted.
     */
    interface ToNeoValue
    {
        AnyType type();

        Object apply( Object javaValue ) throws ProcedureException;
    }

    private final Map<Class<?>, ToNeoValue> javaToNeo = new HashMap<>();

    public TypeMappers()
    {
        registerScalarsAndCollections();
    }

    /**
     * We don't have Node, Relationship, Property available down here - and don't strictly want to, we want the procedures to be independent of which
     * Graph API is being used (and we don't want them to get tangled up with kernel code). So, we only register the "core" type system here, scalars and
     * collection types. Node, Relationship, Path and any other future graph types should be registered from the outside in the same place APIs to work
     * with those types is registered.
     */
    private void registerScalarsAndCollections()
    {
        registerType( String.class, TO_STRING );
        registerType( int.class, TO_INTEGER );
        registerType( Integer.class, TO_INTEGER );
        registerType( long.class, TO_INTEGER );
        registerType( Long.class, TO_INTEGER );
        registerType( float.class, TO_FLOAT );
        registerType( Float.class, TO_FLOAT );
        registerType( double.class, TO_FLOAT );
        registerType( Double.class, TO_FLOAT );
        registerType( Number.class, TO_NUMBER );
        registerType( boolean.class, TO_BOOLEAN );
        registerType( Boolean.class, TO_BOOLEAN );
        registerType( Map.class, TO_MAP );
        registerType( List.class, TO_LIST );
        registerType( Object.class, TO_ANY );
    }

    public ToNeoValue javaToNeo( Class<?> javaType ) throws ProcedureException
    {
        ToNeoValue converter = javaToNeo.get( javaType );
        if( converter != null )
        {
            return converter;
        }
        throw javaToNeoMappingError( javaType, Neo4jTypes.NTAny );
    }

    public void registerType( Class<?> javaClass, ToNeoValue toNeo )
    {
        javaToNeo.put( javaClass, toNeo );
    }

    public static class TypeMapper implements ToNeoValue
    {
        private final AnyType type;
        private final Function<Object,Object> mapper;

        public TypeMapper( AnyType type, Function<Object, Object> mapper )
        {
            this.type = type;
            this.mapper = mapper;
        }

        @Override
        public AnyType type()
        {
            return type;
        }

        @Override
        public Object apply( Object javaValue ) throws ProcedureException
        {
            if( javaValue == null )
            {
                return null;
            }

            Object out = mapper.apply( javaValue );
            if( out != null )
            {
                return out;
            }
            throw javaToNeoMappingError( javaValue.getClass(), type() );
        }
    }

    private final ToNeoValue TO_STRING = new TypeMapper( NTString, (v) -> v instanceof String ? v : null );
    private final ToNeoValue TO_INTEGER = new TypeMapper( NTInteger, (v) -> v instanceof Number ? ((Number)v).longValue() : null );
    private final ToNeoValue TO_FLOAT = new TypeMapper( NTFloat, (v) -> v instanceof Number ? ((Number)v).doubleValue() : null );
    private final ToNeoValue TO_NUMBER = new TypeMapper( NTNumber, (v) -> v instanceof Number ? v : null );
    private final ToNeoValue TO_BOOLEAN = new TypeMapper( NTBoolean, (v) -> v instanceof Boolean ? v : null );
    private final ToNeoValue TO_MAP = new TypeMapper( NTMap, (v) -> v instanceof Map ? v : null );
    private final ToNeoValue TO_LIST = new TypeMapper( NTList( NTAny ), (v) -> v instanceof List ? v : null );
    private final ToNeoValue TO_ANY = new TypeMapper( NTAny, (v) -> v );

    private static ProcedureException javaToNeoMappingError( Class<?> cls, AnyType neoType )
    {
        return new ProcedureException( Status.Statement.InvalidType, "Don't know how to map `%s` to `%s`", cls, neoType );
    }
}
