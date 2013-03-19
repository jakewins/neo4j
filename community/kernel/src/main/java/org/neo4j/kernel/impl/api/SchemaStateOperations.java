package org.neo4j.kernel.impl.api;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.api.operations.SchemaOperations;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;

public class SchemaStateOperations extends DelegatingSchemaOperations
{
    private final SchemaStateHolder stateHolder;

    public SchemaStateOperations( SchemaOperations inner, SchemaStateHolder stateHolder )
    {
        super( inner );
        this.stateHolder = stateHolder;
    }

    @Override
    public IndexRule addIndexRule( long labelId, long propertyKey ) throws ConstraintViolationKernelException
    {
        stateHolder.flush();
        return delegate.addIndexRule( labelId, propertyKey );
    }

    @Override
    public void dropIndexRule( IndexRule indexRule ) throws ConstraintViolationKernelException
    {
        stateHolder.flush();
        delegate.dropIndexRule( indexRule );
    }

    @Override
    public <T> T getOrCreateFromSchemaState( Object key, Function<Void, T> creator )
    {
        return stateHolder.getOrCreateFromSchemaState( key, creator );
    }
}
