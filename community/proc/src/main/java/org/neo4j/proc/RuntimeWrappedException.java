package org.neo4j.proc;

import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.Status;

/**
 * Because procedures use {@link java.util.stream.Stream} API extensively and it does not give us a sensible way to throw checked exceptions, we use this
 * as a vessel.
 */
public class RuntimeWrappedException extends RuntimeException implements Status.HasStatus
{
    private final KernelException wrappee;

    public RuntimeWrappedException( KernelException wrappee )
    {
        super( wrappee.getMessage(), wrappee );
        this.wrappee = wrappee;
    }

    @Override
    public Status status()
    {
        return wrappee.status();
    }

    public KernelException unwrap()
    {
        return wrappee;
    }
}
