package org.neo4j.kernel.impl.api;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.api.operations.SchemaOperations;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;

public class SchemaStateOperations extends DelegatingSchemaOperations
{
    private final TransactionalSchemaState stateHolder;

    public SchemaStateOperations( SchemaOperations inner, TransactionalSchemaState stateHolder )
    {
        super( inner );
        this.stateHolder = stateHolder;
    }

    @Override
    public IndexRule addIndexRule( long labelId, long propertyKey ) throws ConstraintViolationKernelException
    {
        // stateHolder.flush() is called only when the index actually goes online
        return delegate.addIndexRule( labelId, propertyKey );
    }

    @Override
    public void dropIndexRule( IndexRule indexRule ) throws ConstraintViolationKernelException
    {
        stateHolder.flush();
        delegate.dropIndexRule( indexRule );
    }

    @Override
    public <K, V> V getOrCreateFromSchemaState( K key, Function<K, V> creator )
    {
        return stateHolder.getOrCreate( key, creator );
    }
}
