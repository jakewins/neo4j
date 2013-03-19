package org.neo4j.kernel.impl.api;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.Functions;

public class TransactionAwareSchemaStateHolderTest
{
    private KernelSchemaStateHolder kernelHolder;
    private TransactionAwareSchemaStateHolder txHolder;

    @Test
    public void should_not_create_in_underlying_holder()
    {
        // GIVEN
        txHolder.getOrCreate( "key", String.class, Functions.constant("update") );

        // WHEN
        String txResult = this.txHolder.getOrCreate( "key", String.class, Functions.constant( "tx" ) );
        String kernelResult = this.kernelHolder.getOrCreate( "key", String.class, Functions.constant( "kernel" ) );

        // THEN
        assertEquals("kernel", kernelResult);
        assertEquals("update", txResult);
    }

    @Test
    public void should_get_from_underlying_holder()
    {
        // GIVEN
        kernelHolder.getOrCreate( "key", String.class, Functions.constant("value") );

        // WHEN
        String txResult = this.txHolder.getOrCreate( "key", String.class, Functions.constant( "tx" ) );
        String kernelResult = this.kernelHolder.getOrCreate( "key", String.class, Functions.constant( "kernel" ) );

        // THEN
        assertEquals("value", kernelResult);
        assertEquals("value", txResult);
    }

    @Test
    public void flush_does_not_apply_to_underlying_before_commit()
    {
        // GIVEN
        txHolder.getOrCreate( "key", String.class, Functions.constant("value") );

        // WHEN
        txHolder.flush();
        String txResult = this.txHolder.getOrCreate( "key", String.class, Functions.constant( "tx" ) );
        String kernelResult = this.kernelHolder.getOrCreate( "key", String.class, Functions.constant( "kernel" ) );

        // THEN
        assertEquals("kernel", kernelResult);
        assertEquals("tx", txResult);
    }

    @Test
    public void flush_does_apply_to_underlying_after_commit()
    {
        // GIVEN
        kernelHolder.getOrCreate( "key", String.class, Functions.constant("kernelValue") );
        txHolder.getOrCreate( "key", String.class, Functions.constant("txValue") );

        // WHEN
        txHolder.flush();
        txHolder.commit();

        String txResult = this.txHolder.getOrCreate( "key", String.class, Functions.constant( "tx" ) );
        String kernelResult = this.kernelHolder.getOrCreate( "key", String.class, Functions.constant( "kernel" ) );

        // THEN
        assertEquals("kernel", kernelResult);
        assertEquals("tx", txResult);
    }

    @Before
    public void before()
    {
        this.kernelHolder = new KernelSchemaStateHolder();
        this.txHolder = new TransactionAwareSchemaStateHolder( this.kernelHolder );
    }
}
