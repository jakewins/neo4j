package org.neo4j.proc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.proc.TypeMappers.ToNeoValue;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@RunWith( Parameterized.class )
public class TypeMappersTest
{
    @Parameterized.Parameter(0) public Class<?> javaClass;
    @Parameterized.Parameter(1) public Neo4jTypes.AnyType neoType;
    @Parameterized.Parameter(2) public Object javaValue;
    @Parameterized.Parameter(3) public Object expectecNeoValue;

    @Parameterized.Parameters(name = "{0} to {1}")
    public static List<Object[]> conversions()
    {
        return asList(
            new Object[]{ Object.class, Neo4jTypes.NTAny, "", "" },
            new Object[]{ Object.class, Neo4jTypes.NTAny, null, null },
            new Object[]{ Object.class, Neo4jTypes.NTAny, 1, 1 },
            new Object[]{ Object.class, Neo4jTypes.NTAny, true, true },
            new Object[]{ Object.class, Neo4jTypes.NTAny, asList(1,2,3), asList(1,2,3) },
            new Object[]{ Object.class, Neo4jTypes.NTAny, new HashMap<>(), new HashMap<>() },

            new Object[]{ String.class, Neo4jTypes.NTString, "", "" },
            new Object[]{ String.class, Neo4jTypes.NTString, "not empty", "not empty" },
            new Object[]{ String.class, Neo4jTypes.NTString, null, null },

            new Object[]{ Map.class, Neo4jTypes.NTMap, new HashMap<>(), new HashMap<>() },
            new Object[]{ Map.class, Neo4jTypes.NTMap, new HashMap<String, Object>() {{ put( "k", 1 ); }} , new HashMap<String, Object>() {{ put( "k", 1 ); }} },
            new Object[]{ Map.class, Neo4jTypes.NTMap, null, null },

            // TODO: Expand on this to pull out the generic type of the lists (which you can do from the field ParameterizedType info) to get the inner type
            new Object[]{ List.class, Neo4jTypes.NTList( Neo4jTypes.NTAny ), asList(), asList() },
            new Object[]{ List.class, Neo4jTypes.NTList( Neo4jTypes.NTAny ), asList(1,2,3,4) , asList(1,2,3,4) },
            new Object[]{ List.class, Neo4jTypes.NTList( Neo4jTypes.NTAny ), asList(asList(1,2), asList("three", "four")) , asList(asList(1,2), asList("three", "four")) },
            new Object[]{ List.class, Neo4jTypes.NTList( Neo4jTypes.NTAny ), null, null },

            new Object[]{ boolean.class, Neo4jTypes.NTBoolean, false, false },
            new Object[]{ boolean.class, Neo4jTypes.NTBoolean, true, true },
            new Object[]{ boolean.class, Neo4jTypes.NTBoolean, null, null },
            new Object[]{ Boolean.class, Neo4jTypes.NTBoolean, false, false },
            new Object[]{ Boolean.class, Neo4jTypes.NTBoolean, true, true },
            new Object[]{ Boolean.class, Neo4jTypes.NTBoolean, null, null },

            new Object[]{ Number.class, Neo4jTypes.NTNumber, 1L, 1L },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, 0L, 0L },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, null, null },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, Long.MIN_VALUE, Long.MIN_VALUE },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, Long.MAX_VALUE, Long.MAX_VALUE },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, 1D, 1D },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, 0D, 0D },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, 1.234D, 1.234D },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, null, null },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, Double.MIN_VALUE, Double.MIN_VALUE },
            new Object[]{ Number.class, Neo4jTypes.NTNumber, Double.MAX_VALUE, Double.MAX_VALUE },

            new Object[]{ long.class, Neo4jTypes.NTInteger, 1L, 1L },
            new Object[]{ long.class, Neo4jTypes.NTInteger, 0L, 0L },
            new Object[]{ long.class, Neo4jTypes.NTInteger, null, null },
            new Object[]{ long.class, Neo4jTypes.NTInteger, Long.MIN_VALUE, Long.MIN_VALUE },
            new Object[]{ long.class, Neo4jTypes.NTInteger, Long.MAX_VALUE, Long.MAX_VALUE },
            new Object[]{ Long.class, Neo4jTypes.NTInteger, 1L, 1L },
            new Object[]{ Long.class, Neo4jTypes.NTInteger, 0L, 0L },
            new Object[]{ Long.class, Neo4jTypes.NTInteger, null, null },
            new Object[]{ Long.class, Neo4jTypes.NTInteger, Long.MIN_VALUE, Long.MIN_VALUE },
            new Object[]{ Long.class, Neo4jTypes.NTInteger, Long.MAX_VALUE, Long.MAX_VALUE },

            new Object[]{ int.class, Neo4jTypes.NTInteger, 1, 1L },
            new Object[]{ int.class, Neo4jTypes.NTInteger, 0, 0L },
            new Object[]{ int.class, Neo4jTypes.NTInteger, null, null },
            new Object[]{ int.class, Neo4jTypes.NTInteger, Integer.MIN_VALUE, (long)Integer.MIN_VALUE },
            new Object[]{ int.class, Neo4jTypes.NTInteger, Integer.MAX_VALUE, (long)Integer.MAX_VALUE },
            new Object[]{ Integer.class, Neo4jTypes.NTInteger, 1, 1L },
            new Object[]{ Integer.class, Neo4jTypes.NTInteger, 0, 0L },
            new Object[]{ Integer.class, Neo4jTypes.NTInteger, null, null },
            new Object[]{ Integer.class, Neo4jTypes.NTInteger, Integer.MIN_VALUE, (long)Integer.MIN_VALUE },
            new Object[]{ Integer.class, Neo4jTypes.NTInteger, Integer.MAX_VALUE, (long)Integer.MAX_VALUE },

            new Object[]{ float.class, Neo4jTypes.NTFloat, 1F, 1D },
            new Object[]{ float.class, Neo4jTypes.NTFloat, 0F, 0D },
            new Object[]{ float.class, Neo4jTypes.NTFloat, 1.234F, (double)1.234F },
            new Object[]{ float.class, Neo4jTypes.NTFloat, null, null },
            new Object[]{ float.class, Neo4jTypes.NTFloat, Float.MIN_VALUE, (double)Float.MIN_VALUE },
            new Object[]{ float.class, Neo4jTypes.NTFloat, Float.MAX_VALUE, (double)Float.MAX_VALUE },
            new Object[]{ Float.class, Neo4jTypes.NTFloat, 1F, 1D },
            new Object[]{ Float.class, Neo4jTypes.NTFloat, 0F, 0D },
            new Object[]{ Float.class, Neo4jTypes.NTFloat, 1.234F, (double)1.234F },
            new Object[]{ Float.class, Neo4jTypes.NTFloat, null, null },
            new Object[]{ Float.class, Neo4jTypes.NTFloat, Float.MIN_VALUE, (double)Float.MIN_VALUE },
            new Object[]{ Float.class, Neo4jTypes.NTFloat, Float.MAX_VALUE, (double)Float.MAX_VALUE },

            new Object[]{ double.class, Neo4jTypes.NTFloat, 1D, 1D },
            new Object[]{ double.class, Neo4jTypes.NTFloat, 0D, 0D },
            new Object[]{ double.class, Neo4jTypes.NTFloat, 1.234D, 1.234D },
            new Object[]{ double.class, Neo4jTypes.NTFloat, null, null },
            new Object[]{ double.class, Neo4jTypes.NTFloat, Double.MIN_VALUE, Double.MIN_VALUE },
            new Object[]{ double.class, Neo4jTypes.NTFloat, Double.MAX_VALUE, Double.MAX_VALUE }
        );
    }

    @Test
    public void shouldDetectCorrectType() throws Throwable
    {
        // When
        ToNeoValue mapper = new TypeMappers().javaToNeo( javaClass );

        // Then
        assertEquals( neoType, mapper.type() );
    }

    @Test
    public void shouldMapCorrectly() throws Throwable
    {
        // Given
        ToNeoValue mapper = new TypeMappers().javaToNeo( javaClass );

        // When
        Object converted = mapper.apply( javaValue );

        // Then
        assertEquals( expectecNeoValue, converted );
    }
}