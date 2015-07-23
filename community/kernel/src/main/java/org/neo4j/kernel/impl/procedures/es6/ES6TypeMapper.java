package org.neo4j.kernel.impl.procedures.es6;

import jdk.nashorn.internal.runtime.ScriptObject;

import java.util.List;

import org.neo4j.helpers.Pair;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.procedure.ProcedureException;
import org.neo4j.kernel.api.procedure.ProcedureSignature;
import org.neo4j.kernel.impl.store.Neo4jTypes;

/** Converts from Nashorn types to Neo4j types. */
public class ES6TypeMapper
{
    private final FromJS[] fromJS;
    private ProcedureSignature signature;

    public ES6TypeMapper( ProcedureSignature signature ) throws ProcedureException
    {
        this.signature = signature;
        this.fromJS = buildFromJSConverters( signature.outputSignature() );
    }

    /** Translate a yielded record array into a regular java object array */
    void translateRecord( Object jsRecord, Object[] targetRecord ) throws ProcedureException
    {
        ScriptObject rec = (ScriptObject) jsRecord;

        if(((Number)rec.getLength()).intValue() != targetRecord.length)
        {
            throw new ProcedureException( Status.Schema.ProcedureSemanticError,
                    "Procedure output does not match declared signature. `%s` should yield %d fields per record, but %d fields were returned.",
                    signature.toString(), signature.outputSignature().size(), rec.getLength() );
        }

        for ( int i = 0; i < targetRecord.length; i++ )
        {
            targetRecord[i] = fromJS[i].apply( rec );
        }
    }

    private interface FromJS
    {
        /** Given a JS-provided record object, convert and return the value at specified record index */
        Object apply( ScriptObject record );
    }

    private FromJS[] buildFromJSConverters( List<Pair<String,Neo4jTypes.AnyType>> signature ) throws ProcedureException
    {
        FromJS[] converters = new FromJS[signature.size()];
        for ( int i = 0; i < signature.size(); i++ )
        {
            Neo4jTypes.AnyType type = signature.get( i ).other();
            if( type == Neo4jTypes.NTInteger )
            {
                converters[i] = new IntegerFromJS(i);
            }
            else
            {
                throw new ProcedureException( Status.Schema.ProcedureInitializationError, "Unknown type: `%s`", type );
            }
        }
        return converters;
    }

    private class IntegerFromJS implements FromJS
    {
        private final int fieldIndex;

        public IntegerFromJS( int fieldIndex )
        {
            this.fieldIndex = fieldIndex;
        }

        @Override
        public Object apply( ScriptObject record )
        {
            return record.getLong( fieldIndex );
        }
    }
}
