package org.neo4j.kernel.impl.api;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.Functions.constant;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.Function;

public class SchemaStateHolderTransactionAwarenessIT
{
    private KernelSchemaStateHolder kernelHolder;
    private TransactionAwareSchemaStateHolder txHolder;

    private Function<String, String> txShouldNotCreate = functionThrows( "tx" );
    private Function<String, String> kernelShouldNotCreate = functionThrows( "kernel" );

    @Test
    public void should_not_create_in_underlying_holder()
    {
        // GIVEN
        txHolder.getOrCreate( "key", String.class, constant( "created_value" ) );

        // WHEN
        String txResult = txHolder.getOrCreate( "key", String.class, txShouldNotCreate );

        // THEN
        assertEquals( "created_value", txResult );
    }

    @Test
    public void should_get_from_underlying_holder()
    {
        // GIVEN
        kernelHolder.getOrCreate( "key", String.class, constant( "original_value" ) );

        // WHEN
        String txResult = txHolder.getOrCreate( "key", String.class, txShouldNotCreate );
        String kernelResult = kernelHolder.getOrCreate( "key", String.class, kernelShouldNotCreate );

        // THEN
        assertEquals( "original_value", kernelResult );
        assertEquals( "original_value", txResult );
    }

    @Test
    public void flush_does_not_apply_to_underlying_before_commit()
    {
        // GIVEN
        kernelHolder.getOrCreate( "key", String.class, constant( "original_value" ) );
        txHolder.getOrCreate( "key", String.class, constant( "updated_value" ) );

        // WHEN
        txHolder.flush();

        // THEN
        String kernelResult = kernelHolder.getOrCreate( "key", String.class, kernelShouldNotCreate );
        assertEquals( "original_value", kernelResult );

        // WHEN
        String txResult = txHolder.getOrCreate( "key", String.class, constant( "recreated_value" ) );

        // THEN
        assertEquals( "recreated_value", txResult );
    }

    @Test
    public void flush_does_apply_to_underlying_on_commit()
    {
        // GIVEN
        kernelHolder.getOrCreate( "key", String.class, constant( "original_value" ) );
        txHolder.getOrCreate( "key", String.class, txShouldNotCreate );

        // WHEN
        txHolder.flush();
        txHolder.commit();

        // THEN
        String kernelResult = kernelHolder.getOrCreate( "key", String.class, constant( "new_value" ) );
        assertEquals( "new_value", kernelResult );

        // WHEN
        String txResult = txHolder.getOrCreate( "key", String.class, txShouldNotCreate );

        // THEN
        assertEquals( "new_value", txResult );
    }

    public Function<String, String> functionThrows( final String msg )
    {
        return new Function<String, String>()
        {
            @Override
            public String apply( String s )
            {
                throw new IllegalStateException( msg );
            }
        };
    }

    @Before
    public void before()
    {
        kernelHolder = new KernelSchemaStateHolder();
        txHolder = new TransactionAwareSchemaStateHolder( kernelHolder );
    }
}
