package org.neo4j.batch;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.nioneo.store.Record;
import org.neo4j.kernel.impl.nioneo.store.RelationshipRecord;
import org.neo4j.kernel.impl.util.collection.LongObjectOpenHashMap;

public class RelChain
{
    private final long nodeId;

    private LongObjectOpenHashMap<RelationshipRecord> records = new LongObjectOpenHashMap<>();

    private int missing = 0;

    public RelChain( long nodeId )
    {
        this.nodeId = nodeId;
    }

    public void append( RelationshipRecord record, long prevRel, long nextRel )
    {
        if( records.containsKey( prevRel ))
        {
            missing--;
        }
        else if( !(prevRel == Record.NO_PREV_RELATIONSHIP.intValue()) )
        {
            missing++;
        }

        if( records.containsKey( nextRel ))
        {
            missing--;
        }
        else if( !(nextRel == Record.NO_NEXT_RELATIONSHIP.intValue()) )
        {
            missing++;
        }

        records.put( record.getId(), record );
    }

    public boolean isComplete()
    {
        return missing == 0;
    }

    public int size()
    {
        return records.size();
    }

    public long nodeId()
    {
        return nodeId;
    }

    public String toString()
    {
        return "RelChainBuilder{" +
                "nodeId=" + nodeId +
                ", records=" + records +
                ", missing=" + missing +
                '}';
    }

    public void accept( final Visitor<RelationshipRecord, RuntimeException> visitor )
    {
        records.visitValues( visitor );
    }
}
