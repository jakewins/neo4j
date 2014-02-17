package org.neo4j.kernel.impl.api.operations;

public interface StatementLayer extends ReadOperations, WriteOperations, SchemaStateOperations
{
    StatementLayer delegate();

    public interface RelationshipVisitor
    {
        void visit( long relId, long startNode, long endNode, int type );
    }
}
