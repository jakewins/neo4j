package org.neo4j.kernel.api.procedure;

import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.procedure.RecordCursor;

/**
 * TODO
 */
public interface Procedure
{
    RecordCursor call(Statement statement, Object[] args);
}
