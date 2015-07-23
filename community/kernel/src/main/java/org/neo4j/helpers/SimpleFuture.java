package org.neo4j.helpers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A simple single-writer multiple-reader future object based on a latch. Using this future with multiple writers will lead to errors.
 * @param <T>
 */
public class SimpleFuture<T> implements Future<T>
{
    private final CountDownLatch fulfillmentLatch = new CountDownLatch(1);
    private volatile T value;
    private volatile RuntimeException failure;

    public void fulfill( T value )
    {
        assert fulfillmentLatch.getCount() == 1;
        this.value = value;
        this.fulfillmentLatch.countDown();
    }

    public void fail( Throwable cause )
    {
        assert fulfillmentLatch.getCount() == 1;
        this.failure = (cause instanceof RuntimeException) ? (RuntimeException)cause : new RuntimeException( cause );
        this.fulfillmentLatch.countDown();
    }

    @Override
    public boolean cancel( boolean mayInterruptIfRunning )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled()
    {
        return false;
    }

    @Override
    public boolean isDone()
    {
        return fulfillmentLatch.getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException
    {
        if(isDone())
        {
            return value();
        }

        fulfillmentLatch.await();
        return value();
    }

    @Override
    public T get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException
    {
        if(isDone())
        {
            return value();
        }

        fulfillmentLatch.await( timeout, unit );
        return value();
    }

    private T value()
    {
        if(failure != null)
        {
            throw failure;
        }
        return value;
    }
}
