package org.neo4j.kernel.impl.coreapi;

import org.neo4j.graphdb.DatabaseShutdownException;
import org.neo4j.kernel.AvailabilityGuard;

/**
 * This is a simple wrapper around {@link AvailabilityGuard} that augments its behavior to match how
 * availability errors and timeouts are handled in the Core API.
 */
public class CoreAPIAvailabilityGuard
{
    private final AvailabilityGuard guard;
    private final long timeout;

    public CoreAPIAvailabilityGuard( AvailabilityGuard guard, long timeout )
    {
        this.guard = guard;
        this.timeout = timeout;
    }

    public boolean isAvailable( long timeoutMillis )
    {
        return guard.isAvailable( timeoutMillis );
    }

    public void assertDatabaseAvailable()
    {
        try
        {
            guard.await( timeout );
        }
        catch ( AvailabilityGuard.UnavailableException e )
        {
            if( guard.isShutdown())
            {
                throw new DatabaseShutdownException();
            }
            throw new org.neo4j.graphdb.TransactionFailureException( e.getMessage() );
        }
    }
}
