package org.neo4j.harness.internal;

import java.util.LinkedList;
import java.util.List;

import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.proc.Procedures;

public class Procs
{
    private final List<Class<?>> procs = new LinkedList<>();

    public void add( Class<?> procedureClass )
    {
        this.procs.add( procedureClass );
    }

    public void applyTo( GraphDatabaseAPI graph ) throws KernelException
    {
        Procedures procedures = graph.getDependencyResolver().resolveDependency( Procedures.class );
        for ( Class<?> cls : procs )
        {
            procedures.register( cls );
        }
    }
}
