/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.branch;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;

import org.neo4j.kernel.impl.nioneo.xa.command.Command;
import org.neo4j.kernel.impl.nioneo.xa.command.LogHandler;
import org.neo4j.kernel.impl.transaction.xaframework.LogEntry;
import org.neo4j.kernel.impl.transaction.xaframework.XaCommand;

/**
 * Lacking a good name - this datastructure should exist elsewhere, but I can't find an easily usable one. It is a
 * simple collection of log entries for a transaction.
 */
public class Tx
{
    public interface Visitor
    {
        void node( Command.NodeCommand command );

        void property( Command.PropertyCommand command );

        void unknown( XaCommand command );
    }

    private final LinkedList<LogEntry> entries = new LinkedList<>();
    private long txId;
    private long checksum;

    public void append( LogEntry entry )
    {
        entries.add( entry );
        if(entry instanceof LogEntry.Commit )
        {
            this.txId = ((LogEntry.Commit)entry).getTxId();
        }
        else if(entry instanceof LogEntry.Start)
        {
            checksum = ((LogEntry.Start) entry).getChecksum();
        }
    }

    public long id()
    {
        return txId;
    }

    public long checksum()
    {
        return checksum;
    }

    @Override
    public String toString()
    {
        return "Tx{" +
                "txId=" + txId +
                ", checksum=" + checksum +
                '}';
    }

    public void dump( PrintStream out )
    {
        for ( LogEntry entry : entries )
        {
            out.println( entry.toString() );
        }
    }

    public void visit( final Visitor visitor )
    {
        LogHandler handler = new VisitorTranslator( visitor );
        for ( LogEntry entry : entries )
        {
            try
            {
                entry.accept( handler );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }

    }

    private static class VisitorTranslator implements LogHandler
    {
        private final Visitor visitor;

        public VisitorTranslator( Visitor visitor )
        {
            this.visitor = visitor;
        }

        @Override
        public void commandEntry( LogEntry.Command commandEntry ) throws IOException
        {
            XaCommand command = commandEntry.getXaCommand();
            if(command instanceof Command.NodeCommand)
            {
                visitor.node( (Command.NodeCommand) command );
            }
            else if(command instanceof Command.PropertyCommand)
            {
                visitor.property( (Command.PropertyCommand) command );
            }
            else
            {
                visitor.unknown(command);
            }
        }

        @Override
        public void startLog() {}
        @Override
        public void startEntry( LogEntry.Start startEntry ) throws IOException {}
        @Override
        public void prepareEntry( LogEntry.Prepare prepareEntry ) throws IOException { }
        @Override
        public void onePhaseCommitEntry( LogEntry.OnePhaseCommit onePhaseCommitEntry ) throws IOException { }
        @Override
        public void twoPhaseCommitEntry( LogEntry.TwoPhaseCommit twoPhaseCommitEntry ) throws IOException { }
        @Override
        public void doneEntry( LogEntry.Done doneEntry ) throws IOException {}
        @Override
        public void endLog( boolean success ) throws IOException{}
    }
}
