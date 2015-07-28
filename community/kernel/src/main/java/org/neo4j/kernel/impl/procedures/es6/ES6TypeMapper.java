package org.neo4j.kernel.impl.procedures.es6;

import jdk.nashorn.internal.runtime.ScriptObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.impl.store.Neo4jTypes;
import org.neo4j.kernel.impl.store.Neo4jTypes.ListType;

/** Converts from Nashorn types to Neo4j types. Each mapper is built to suit one specific signature, so it's a fixed JS record -> Neo4j Record conversion */
public class ES6TypeMapper
{
    /** Array of javascript->java converters, one converter per field in the output records */
    private final FromJS[] fromJS;

    /** Signature of the procedure this mapper is built for */
    private ProcedureSignature signature;

    public ES6TypeMapper( ProcedureSignature signature ) throws ProcedureException
    {
        this.signature = signature;
        this.fromJS = buildFromJSConverters( signature.outputSignature() );
    }

    /** Translate a yielded record array into a regular java object array */
    void translateRecord( Object jsRecord, Object[] targetRecord ) throws ProcedureException
    {
        ScriptObject rec = (ScriptObject) jsRecord;

        if(((Number)rec.getLength()).intValue() != targetRecord.length)
        {
            throw new ProcedureException( Status.Schema.ProcedureSemanticError,
                    "Procedure output does not match declared signature. `%s` should yield %d fields per record, but %d fields were returned.",
                    signature.toString(), signature.outputSignature().size(), rec.getLength() );
        }

        for ( int i = 0; i < targetRecord.length; i++ )
        {
            targetRecord[i] = fromJS[i].apply( rec.get( i ) );
        }
    }

    private interface FromJS<OUT>
    {
        /** Given a JS-provided record object, convert and return the value at specified record index */
        OUT apply( Object jsValue ) throws ProcedureException;
    }

    private FromJS[] buildFromJSConverters( List<Pair<String,Neo4jTypes.AnyType>> signature ) throws ProcedureException
    {
        FromJS[] converters = new FromJS[signature.size()];
        // For each field in the output records
        for ( int i = 0; i < signature.size(); i++ )
        {
            Neo4jTypes.AnyType type = signature.get( i ).other();
            converters[i] = converterFor( type );
        }
        return converters;
    }

    private FromJS<?> converterFor( Neo4jTypes.AnyType type ) throws ProcedureException
    {
        if( type == Neo4jTypes.NTInteger )
        {
            return new IntegerFromJS();
        }
        else if( type == Neo4jTypes.NTFloat )
        {
            return new FloatFromJS();
        }
        else if( type == Neo4jTypes.NTNumber )
        {
            return new NumberFromJS();
        }
        else if( type == Neo4jTypes.NTBoolean )
        {
            return new BooleanFromJS();
        }
        else if( type == Neo4jTypes.NTText )
        {
            return new TextFromJS();
        }
        else if( type == Neo4jTypes.NTNode )
        {
            return new NodeFromJS();
        }
        else if( type == Neo4jTypes.NTRelationship )
        {
            return new RelationshipFromJS();
        }
        else if( type == Neo4jTypes.NTPath )
        {
            return new PathFromJS();
        }
        else if( type == Neo4jTypes.NTMap )
        {
            return new MapFromJS();
        }
        else if( type instanceof ListType )
        {
            return new ListFromJS( converterFor( ((ListType) type).innerType() ) );
        }
        else if( type == Neo4jTypes.NTAny )
        {
            return new AnyFromJS();
        }
        else
        {
            throw new ProcedureException( Status.Schema.ProcedureInitializationError, "Unknown type: `%s`", type );
        }
    }

    private static class IntegerFromJS implements FromJS<Long>
    {
        @Override
        public Long apply( Object val )
        {
            return ((Number)val).longValue();
        }
    }

    private static class TextFromJS implements FromJS<String>
    {
        @Override
        public String apply( Object val )
        {
            return (String) val;
        }
    }

    private static class FloatFromJS implements FromJS<Double>
    {
        @Override
        public Double apply( Object val )
        {
            return ((Number)val).doubleValue();
        }
    }

    private static class NumberFromJS implements FromJS<Number>
    {
        @Override
        public Number apply( Object val )
        {
            return (Number) val;
        }
    }

    private static class BooleanFromJS implements FromJS<Boolean>
    {
        @Override
        public Boolean apply( Object val )
        {
            return (Boolean) val;
        }
    }

    private static class NodeFromJS implements FromJS<Node>
    {
        @Override
        public Node apply( Object val )
        {
            return (Node) val;
        }
    }

    private static class RelationshipFromJS implements FromJS<Relationship>
    {
        @Override
        public Relationship apply( Object val )
        {
            return (Relationship) val;
        }
    }

    private static class PathFromJS implements FromJS<Path>
    {
        @Override
        public Path apply( Object val )
        {
            return (Path) val;
        }
    }

    private static class ListFromJS implements FromJS<Object>
    {
        private final FromJS<?> innerType;

        private ListFromJS( FromJS<?> innerType )
        {
            this.innerType = innerType;
        }

        @Override
        public Object apply( Object val ) throws ProcedureException
        {
            if(val instanceof Collection )
            {
                return val;
            }

            if(val instanceof ScriptObject)
            {
                return toList( (ScriptObject) val, innerType );
            }

            throw new ProcedureException( Status.Statement.InvalidType, "Cannot coerce `%s` to list.", val.getClass().getSimpleName() );
        }
    }

    /** Converts any JS value to an appropriate Neo4j type based on JS type information available at runtime. */
    private static class AnyFromJS implements FromJS<Object>
    {
        @Override
        public Object apply( Object js ) throws ProcedureException
        {
            // Types that require no conversion
            if( js instanceof String || js instanceof Boolean || js instanceof Node || js instanceof Relationship || js instanceof Path
                || js instanceof Map || js instanceof Collection || js instanceof Double || js instanceof Long )
            {
                // TODO: Collection types need deep-copy-and-map - or will it always be the case that regular java data structures will have come from java?
                return js;
            }

            if( js instanceof Integer || js instanceof Short || js instanceof Byte )
            {
                return ((Number)js).longValue();
            }
            if( js instanceof Float )
            {
                return ((Number)js).doubleValue();
            }
            if( js instanceof ScriptObject )
            {
                ScriptObject sobj = (ScriptObject) js;
                if( sobj.isArray())
                {
                    return toList( sobj, this );
                }
                else
                {
                    return toMap( sobj );
                }
            }

            throw new ProcedureException( Status.Statement.InvalidType, "Unknown type: `%s`.", js.getClass().getSimpleName() );
        }
    }

    /**
     * This is a bit tricky. In the Cypher type system, PropertyContainer and Map are the same type, meaning Relationship and Node both inherit from
     * Map. Because of that, we allow both Map objects and Node/Relationship objects to go through this converter without wrapping, trusting cypher to
     * recognize the Node/Rel objects and act appropriately. This should change as we shift away from internal use of the embedded API and consolidate the
     * type system.
     */
    private static class MapFromJS implements FromJS<Object>
    {
        @Override
        public Object apply( Object val ) throws ProcedureException
        {
            if(val instanceof PropertyContainer || val instanceof Map )
            {
                return val;
            }

            if(val instanceof ScriptObject)
            {
                return toMap( (ScriptObject) val );
            }

            throw new ProcedureException( Status.Statement.InvalidType, "Cannot coerce `%s` to map.", val.getClass().getSimpleName() );
        }
    }

    /** Deep copy of maps, since script context is thread-bound and may have changed by the time this gets read. */
    private static Map<String, Object> toMap( ScriptObject js ) throws ProcedureException
    {
        Map<String, Object> mapped = new HashMap<>( js.size(), 1.0f );
        for ( Map.Entry<Object,Object> entry : js.entrySet() )
        {
            mapped.put( entry.getKey().toString(), any.apply( entry.getValue() ) );
        }
        return mapped;
    }

    /** Deep copy of lists, since script context is thread-bound and may have changed by the time this gets read. */
    private static List<Object> toList( ScriptObject js, FromJS<?> innerType ) throws ProcedureException
    {
        // TODO: Nashorn has a *ton* of optimized data structures that are backed by primitive arrays, which we could pull out directly here (eg. int[] and so on)
        List<Object> mapped = new LinkedList<>();
        Iterator<Object> iter = js.valueIterator();
        while(iter.hasNext())
        {
            mapped.add( innerType.apply(iter.next()) );
        }
        return mapped;
    }

    private static final AnyFromJS any = new AnyFromJS();
}
