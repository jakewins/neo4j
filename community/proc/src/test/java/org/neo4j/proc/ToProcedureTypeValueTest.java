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

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.proc.TypeMappers.ToProcedureValue;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.neo4j.helpers.collection.MapUtil.map;

public class ToProcedureTypeValueTest
{
    //NOTE this is only here to get the generic type
    @SuppressWarnings( "unused" )
    private List<List<Integer>> listOfList;

    @Test
    public void shouldNotTouchNativeNeoTypes() throws ProcedureException
    {
        Map<Type,Object> nativeValues = new HashMap<>();
        nativeValues.put( long.class, 42L );
        nativeValues.put( Long.class, 423L );
        nativeValues.put( double.class, 423D );
        nativeValues.put( Double.class, 423D );
        nativeValues.put( String.class, "hello" );
        nativeValues.put( boolean.class, true );
        nativeValues.put( Boolean.class, Boolean.FALSE );
        nativeValues.put( Map.class, map( "key", map( "innerKey", 76 ) ) );
        nativeValues.forEach( ( type, value ) -> {
            try
            {
                // GIVEN
                ToProcedureValue mapper = new TypeMappers().neoToJava( type );

                // WHEN
                Object mapped = mapper.apply( value );

                // THEN
                assertThat( mapped, sameInstance( value ) );
            }
            catch ( ProcedureException e )
            {
                fail( e.getMessage() );
            }
        } );
    }

    @Test
    public void shouldDownCastLongsToInt() throws ProcedureException
    {
        // GIVEN
        ToProcedureValue mapper = new TypeMappers().neoToJava( int.class );

        // WHEN
        Object mapped = mapper.apply( 42L );

        // THEN
        assertThat( mapped, instanceOf( Integer.class ) );
        assertThat(mapped, equalTo(42));
    }

    @Test
    public void shouldDownCastDoubleToFloat() throws ProcedureException
    {
        // GIVEN
        ToProcedureValue mapper = new TypeMappers().neoToJava( float.class );

        // WHEN
        Object mapped = mapper.apply( 3.14159265359D );

        // THEN
        assertThat( mapped, instanceOf( Float.class ) );
        assertThat(mapped, equalTo((3.14159265359F)));
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldConvertGenericTypes() throws ProcedureException, NoSuchFieldException
    {
        // GIVEN
        List<List<Long>> list = asList( asList( 1L, 2L ), asList( 3L, 4L, 5L ) );
        ToProcedureValue mapper =
                new TypeMappers()
                        .neoToJava( ToProcedureTypeValueTest.class.getDeclaredField( "listOfList" ).getGenericType() );

        // WHEN
         List<List<?>> mapped = (List<List<?>>) mapper.apply( list );

        // THEN
        for ( List<?> objects : mapped )
        {
            for ( Object object : objects )
            {
                assertThat( object, instanceOf( Integer.class ) );
            }
        }
    }
}
