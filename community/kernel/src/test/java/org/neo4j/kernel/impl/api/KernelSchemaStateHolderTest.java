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
    public void should_create_missing_state()
    {
        // GIVEN
        String result = holder.getOrCreate( "key", String.class, constant( "created_value" ) );

        // THEN
        assertEquals( "created_value", result );
    }

    @Test
    public void should_return_original_state()
    {
        // GIVEN
        holder.getOrCreate( "key", String.class, constant( "original_value" ) );

        // WHEN
        String result = holder.getOrCreate( "key", String.class, constant( "updated_value" ) );

        // THEN
        assertEquals( "original_value", result );
    }

    @Test
    public void should_clear_original_state_on_flush()
    {
        // GIVEN
        holder.getOrCreate( "key", String.class, constant( "original_value" ) );

        // WHEN
        holder.flush();
        String result = holder.getOrCreate( "key", String.class, constant( "updated_value" ) );

        // THEN
        assertEquals( "updated_value", result );
    }

    @Test
    public void should_apply_updates()
    {
        // GIVEN
        Map<String, String> update = MapUtil.stringMap( "key", "original_value" );

        // WHEN
        holder.apply( update );

        // THEN
        String result = holder.getOrCreate( "key", String.class, constant( "updated_value" ) );
        assertEquals( "original_value", result );
    }

    @Test(expected = /* THEN */ IllegalArgumentException.class)
    public void should_throw_when_updates_overwrite()
    {
        // GIVEN
        holder.getOrCreate( "key", String.class, constant( "original_value" ) );
        Map<String, String> update = MapUtil.stringMap( "key", "updated_value" );

        // WHEN
        holder.apply( update );
    }

    @Before
    public void before()
    {
        this.holder = new KernelSchemaStateHolder();
    }
}
