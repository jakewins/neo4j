package org.neo4j.proc;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.proc.Neo4jTypes.AnyType;

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

    public ToNeoValue javaToNeo( Class<?> javaType ) throws ProcedureException
    {
        if( javaType == String.class )
        {
            return new StringToNTString();
        }
        throw javaToNeoMappingError( javaType, Neo4jTypes.NTAny );
    }

    private class StringToNTString implements ToNeoValue
    {
        @Override
        public AnyType type()
        {
            return Neo4jTypes.NTString;
        }

        @Override
        public Object apply( Object javaValue ) throws ProcedureException
        {
            if( javaValue instanceof String )
            {
                return javaValue;
            }
            throw javaToNeoMappingError( javaValue.getClass(), type() );
        }
    }

    private ProcedureException javaToNeoMappingError( Class<?> cls, AnyType neoType )
    {
        return new ProcedureException( Status.Statement.InvalidType, "Don't know how to map `%s` to `%s`", cls, neoType );
    }
}
