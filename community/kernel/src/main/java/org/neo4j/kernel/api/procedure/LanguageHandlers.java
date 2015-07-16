package org.neo4j.kernel.api.procedure;

/**
 * TODO
 */
public interface LanguageHandlers
{
    void addLanguageHandler(String language, LanguageHandler handler);

    void removeLanguageHandler(String language);
}
