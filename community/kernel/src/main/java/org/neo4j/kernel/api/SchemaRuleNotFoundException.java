package org.neo4j.kernel.api;

public class SchemaRuleNotFoundException extends KernelException
{
    public SchemaRuleNotFoundException( String message )
    {
        super( message );
    }
}
