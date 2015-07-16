package org.neo4j.kernel.api.procedure;

import org.neo4j.cursor.Cursor;

/**
 * TODO
 */
public interface RecordCursor
    extends Cursor
{
    Object[] getRecord();
}
