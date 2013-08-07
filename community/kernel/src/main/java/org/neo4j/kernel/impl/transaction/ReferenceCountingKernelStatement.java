package org.neo4j.kernel.impl.transaction;

import org.neo4j.kernel.api.KernelStatement;

public class ReferenceCountingKernelStatement extends KernelStatement
{
    private final KernelStatement inner;
    private int references = 0;
    private boolean hasBeenClosed = false;

    public ReferenceCountingKernelStatement( KernelStatement actualStatement )
    {
        super( actualStatement.keyReadOperations(), actualStatement.keyWriteOperations(),
                actualStatement.entityReadOperations(), actualStatement.entityWriteOperations(),
                actualStatement.schemaReadOperations(), actualStatement.schemaWriteOperations(),
                actualStatement.schemaStateOperations());
        this.inner = actualStatement;

    }

    public void claimReference()
    {
        references++;
    }

    public boolean isClosed()
    {
        return hasBeenClosed;
    }

    @Override
    public void close()
    {
        if(references > 1)
        {
            references--;
        } else
        {
            inner.close();
        }
    }
}
