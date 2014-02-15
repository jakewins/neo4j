package org.neo4j.kernel.impl.api.operations;

public interface StatementLayer extends ReadOperations, WriteOperations
{
    StatementLayer delegate();
}
