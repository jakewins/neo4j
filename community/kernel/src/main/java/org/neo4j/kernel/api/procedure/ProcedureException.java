package org.neo4j.kernel.api.procedure;

import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.Status;

/**
 * TODO
 */
public class ProcedureException
    extends KernelException
{
    public ProcedureException( Status statusCode, Throwable cause,
            String message, Object... parameters )
    {
        super( statusCode, cause, message, parameters );
    }

    public ProcedureException( Status statusCode, String message,
            Object... parameters )
    {
        super( statusCode, message, parameters );
    }
}
