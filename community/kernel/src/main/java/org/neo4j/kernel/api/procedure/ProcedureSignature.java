package org.neo4j.kernel.api.procedure;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.neo4j.helpers.Pair;

import static org.neo4j.helpers.Pair.pair;

public class ProcedureSignature
{
    private final String[] namespace;
    private final String name;
    private final List<Pair<String,Neo4jType>> inputSignature;
    private final List<Pair<String,Neo4jType>> outputSignature;

    public ProcedureSignature( String[] namespace, String name,
            List<Pair<String,Neo4jType>> inputSignature,
            List<Pair<String,Neo4jType>> outputSignature )
    {
        this.namespace = namespace;
        this.name = name;
        this.inputSignature = inputSignature;
        this.outputSignature = outputSignature;
    }

    public String[] getNamespace()
    {
        return namespace;
    }

    public String getName()
    {
        return name;
    }

    public List<Pair<String,Neo4jType>> getInputSignature()
    {
        return inputSignature;
    }

    public List<Pair<String,Neo4jType>> getOutputSignature()
    {
        return outputSignature;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        { return true; }
        if ( o == null || getClass() != o.getClass() )
        { return false; }

        ProcedureSignature that = (ProcedureSignature) o;

        if ( !inputSignature.equals( that.inputSignature ) )
        { return false; }
        if ( !name.equals( that.name ) )
        { return false; }
        if ( !Arrays.equals( namespace, that.namespace ) )
        { return false; }
        if ( !outputSignature.equals( that.outputSignature ) )
        { return false; }

        return true;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    // TODO this should move to a unified Neo4j type system place
    // See also PropertyType etc, and this likely needs to be classes rather than
    // enums, to handle generics (if we support that)
    public enum Neo4jType
    {
        TEXT,
        INTEGER,
        FLOAT,
        BOOLEAN,
        LIST,
        MAP,
        NODE,
        RELATIONSHIP,
        PATH
    }

    public static class Builder
    {
        private final String[] namespace;
        private final String name;
        private final List<Pair<String, Neo4jType>> inputSignature = new LinkedList<>();
        private final List<Pair<String, Neo4jType>> outputSignature = new LinkedList<>();

        public Builder( String[] namespace, String name )
        {
            this.namespace = namespace;
            this.name = name;
        }

        /** Define an input field */
        public Builder in( String name, Neo4jType type )
        {
            inputSignature.add( pair( name, type ) );
            return this;
        }

        /** Define an output field */
        public Builder out( String name, Neo4jType type )
        {
            outputSignature.add( pair( name, type ) );
            return this;
        }

        public ProcedureSignature build()
        {
            return new ProcedureSignature(namespace, name, inputSignature, outputSignature );
        }
    }

    public static Builder procedureSignature(String[] namespace, String name)
    {
        return new Builder(namespace, name);
    }
}
