package org.neo4j.kernel.impl.api.operations;

import java.io.InputStream;
import java.util.Iterator;

import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.api.procedure.RecordCursor;
import org.neo4j.kernel.impl.api.KernelStatement;

/**
 * TODO
 */
public interface ProcedureExecutionOperations
{
    void verify( ProcedureSignature signature, String language, InputStream code ) throws ProcedureException;

    RecordCursor call( KernelStatement statement, ProcedureSignature signature, Object[] args )
            throws ProcedureException;
}
