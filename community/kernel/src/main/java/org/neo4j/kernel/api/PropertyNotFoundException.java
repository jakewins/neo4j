package org.neo4j.kernel.api;

public class PropertyNotFoundException extends KernelException
{
    public PropertyNotFoundException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
