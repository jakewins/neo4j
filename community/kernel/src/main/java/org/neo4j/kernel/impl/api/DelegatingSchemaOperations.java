package org.neo4j.kernel.impl.api;

import org.neo4j.helpers.Function;
import org.neo4j.kernel.api.ConstraintViolationKernelException;
import org.neo4j.kernel.api.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.api.operations.SchemaOperations;
import org.neo4j.kernel.impl.api.index.IndexDescriptor;
import org.neo4j.kernel.impl.nioneo.store.IndexRule;

public class DelegatingSchemaOperations implements SchemaOperations
{
    protected final SchemaOperations delegate;

    public DelegatingSchemaOperations( SchemaOperations delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public IndexRule addIndexRule( long labelId, long propertyKey ) throws ConstraintViolationKernelException
    {
        return delegate.addIndexRule( labelId, propertyKey );
    }

    @Override
    public void dropIndexRule( IndexRule indexRule ) throws ConstraintViolationKernelException
    {
        delegate.dropIndexRule( indexRule );
    }

    @Override
    public IndexDescriptor getIndexDescriptor( long indexId ) throws IndexNotFoundKernelException
    {
        return delegate.getIndexDescriptor( indexId );
    }

    @Override
    public IndexRule getIndexRule( long labelId, long propertyKey ) throws SchemaRuleNotFoundException
    {
        return delegate.getIndexRule( labelId, propertyKey );
    }

    @Override
    public Iterable<IndexRule> getIndexRules()
    {
        return delegate.getIndexRules();
    }

    @Override
    public Iterable<IndexRule> getIndexRules( long labelId )
    {
        return delegate.getIndexRules( labelId );
    }

    @Override
    public InternalIndexState getIndexState( IndexRule indexRule ) throws IndexNotFoundKernelException
    {
        return delegate.getIndexState( indexRule );
    }

    @Override
    public <T> T getOrCreateFromSchemaState( Object key, Function<Void, T> creator )
    {
        return delegate.getOrCreateFromSchemaState( key, creator );
    }
}
