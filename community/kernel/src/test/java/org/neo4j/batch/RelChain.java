package org.neo4j.batch;

import java.util.HashMap;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.nioneo.store.Record;
import org.neo4j.kernel.impl.nioneo.store.RelationshipRecord;

public class RelChain
{
    public static final byte INCOMING = (byte)0;
    public static final byte OUTGOING = (byte)1;

    private HashMap<Long, RelationshipRecord> known = new HashMap<>();

    private int missing = 0;

    public RelChain( long nodeId )
    {
    }

    public void append( long id, long prevRel, long nextRel, RelationshipRecord record )
    {
        if( !(prevRel == Record.NO_PREV_RELATIONSHIP.intValue()) )
        {
            missing++;
        } else if( known.containsKey( prevRel ))
        {
            missing--;
        }

        if( !(nextRel == Record.NO_NEXT_RELATIONSHIP.intValue()) )
        {
            missing++;
        } else if( known.containsKey( nextRel ))
        {
            missing--;
        }

        known.put( id, record );
    }

    public RelChain reset()
    {
        known.clear();
        missing = 0;
        return this;
    }

    public boolean isComplete()
    {
        return missing == 0;
    }

    public int size()
    {
        return known.size();
    }

    public String toString()
    {
        return "RelChainBuilder{" +
                ", known=" + known +
                ", missing=" + missing +
                '}';
    }

    public void accept( final Visitor<RelationshipRecord, RuntimeException> visitor )
    {
        // TODO
    }
}
