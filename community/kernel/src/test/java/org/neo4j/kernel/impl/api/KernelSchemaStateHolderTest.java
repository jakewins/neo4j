package org.neo4j.kernel.impl.api;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.Functions.constant;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.MapUtil;

public class KernelSchemaStateHolderTest
{
    private KernelSchemaStateHolder holder;

    @Test
    public void should_create_missing_state() {
        // GIVEN
        String result = holder.getOrCreate( "key", String.class, constant( "value" ) );

        // THEN
        assertEquals("value", result);
    }


    @Test
    public void should_return_existing_state() {
        // GIVEN
        holder.getOrCreate( "key", String.class, constant( "value" ) );

        // WHEN
        String result = holder.getOrCreate( "key", String.class, constant( "update" ) );


        // THEN
        assertEquals("value", result);
    }


    @Test
    public void should_clear_existing_state_on_flush()
    {
        // GIVEN
        holder.getOrCreate( "key", String.class, constant( "value" ) );

        // WHEN
        holder.flush();
        String result = holder.getOrCreate( "key", String.class, constant( "update" ) );

        // THEN
        assertEquals("update", result);
    }

    @Test
    public void should_apply_updates()
    {
        // GIVEN
        Map<String, String> update = MapUtil.stringMap( "key", "value" );

        // WHEN
        holder.apply( update );
        String result = holder.getOrCreate( "key", String.class, constant( "update" ) );

        // THEN
        assertEquals( "value", result );
    }

    @Test(expected = /* THEN */ IllegalArgumentException.class)
    public void should_throw_when_updates_overwrite()
    {
        // GIVEN
        holder.getOrCreate( "key", String.class, constant( "value" ) );
        Map<String, String> update = MapUtil.stringMap( "key", "update" );

        // WHEN
        holder.apply( update );
    }

    @Before
    public void before()
    {
        this.holder = new KernelSchemaStateHolder();
    }
}
