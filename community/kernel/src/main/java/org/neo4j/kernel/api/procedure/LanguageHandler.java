package org.neo4j.kernel.api.procedure;

import java.io.InputStream;

/**
 * TODO
 */
public interface LanguageHandler
{
    Procedure compile(ProcedureSignature signature, InputStream code) throws ProcedureException;
}
