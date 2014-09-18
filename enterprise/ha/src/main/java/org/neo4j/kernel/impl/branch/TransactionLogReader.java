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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.transaction.xa.Xid;

import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.PrefetchingIterator;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;
import org.neo4j.kernel.impl.nioneo.store.StoreChannel;
import org.neo4j.kernel.impl.nioneo.xa.LogDeserializer;
import org.neo4j.kernel.impl.nioneo.xa.XaCommandReaderFactory;
import org.neo4j.kernel.impl.transaction.xaframework.LogEntry;
import org.neo4j.kernel.impl.transaction.xaframework.VersionAwareLogEntryReader;
import org.neo4j.kernel.impl.util.Consumer;
import org.neo4j.kernel.impl.util.Cursor;

/**
 * Programmatic introspection of transaction logs.
 */
public class TransactionLogReader
{
    private final FileSystemAbstraction fs;

    public enum Direction
    {
        /**
         * Read logs starting with the oldest and moving forward in time.
         */
        FORWARD,

        /**
         * Read logs starting with the newest and moving backward in time.
         */
        BACKWARD;
    }

    public TransactionLogReader(FileSystemAbstraction fs)
    {
        this.fs = fs;
    }

    public ResourceIterator<Tx> readLogs(final File db, final Direction dir) throws IOException
    {
        return readLogs( db, -1, dir );
    }

    public ResourceIterator<Tx> readLogs( File db, long startAtTx, Direction dir )
    {
        switch(dir)
        {
            case FORWARD:
                return new ForwardLogEntryIterator( db, dir, startAtTx );
            case BACKWARD:
                return new BackwardLogEntryIterator( db, dir, startAtTx );
            default: throw new RuntimeException("Unknown direction: " + dir);
        }
    }

    protected Collection<String> filenamesOf( File file, final String prefix, Direction dir )
    {
        if ( fs.isDirectory( file ) )
        {
            File[] files = fs.listFiles( file, new FilenameFilter()
            {
                @Override
                public boolean accept( File dir, String name )
                {
                    return name.contains( prefix ) && !name.contains( "active" );
                }
            } );
            Collection<String> result = new TreeSet<>( sequentialComparator(dir) );
            for ( int i = 0; i < files.length; i++ )
            {
                result.add( files[i].getPath() );
            }
            return result;
        }
        else
        {
            return Collections.emptyList();
        }
    }

    private static Comparator<? super String> sequentialComparator( final Direction dir )
    {
        return new Comparator<String>()
        {
            @Override
            public int compare( String o1, String o2 )
            {
                switch(dir)
                {
                    case FORWARD:
                        return versionOf( o1 ).compareTo( versionOf( o2 ) );
                    case BACKWARD:
                        return versionOf( o2 ).compareTo( versionOf( o1 ) );
                    default: return 0;
                }
            }

            private Integer versionOf( String string )
            {
                String toFind = ".v";
                int index = string.indexOf( toFind );
                if ( index == -1 )
                {
                    return Integer.MAX_VALUE;
                }
                return Integer.valueOf( string.substring( index + toFind.length() ) );
            }
        };
    }

    private abstract class LogEntryIterator extends PrefetchingIterator<Tx> implements ResourceIterator<Tx>
    {
        protected final ByteBuffer buffer = ByteBuffer.allocateDirect( 9 + Xid.MAXGTRIDSIZE + Xid.MAXBQUALSIZE * 10 );
        private final File db;
        private final Direction dir;

        private Iterator<String> logFiles;
        private StoreChannel currentLog;
        private Tx txGettingBuilt = new Tx();

        protected Cursor<LogEntry, IOException> currentCursor;
        protected Tx currentTx;


        protected Consumer<LogEntry, IOException> consumer = new Consumer<LogEntry, IOException>()
        {
            @Override
            public boolean accept( LogEntry logEntry ) throws IOException
            {
                txGettingBuilt.append(logEntry);
                if( logEntry instanceof LogEntry.Done)
                {
                    if(shouldIncludeTx( txGettingBuilt.id() ))
                    {
                        currentTx = txGettingBuilt;
                    }
                    txGettingBuilt = new Tx();
                }
                return true;
            }
        };

        public LogEntryIterator( File db, Direction dir )
        {
            this.db = db;
            this.dir = dir;
        }

        @Override
        protected Tx fetchNextOrNull()
        {
            try
            {
                if(logFiles == null)
                {
                    logFiles = createLogIterator( db, dir );
                }

                if ( currentLog == null )
                {
                    if( logFiles.hasNext() )
                    {
                        currentLog = fs.open( new File( logFiles.next() ), "r" );
                        buffer.clear();
                        VersionAwareLogEntryReader.readLogHeader( buffer, currentLog, false );
                    }

                    if( currentLog == null )
                    {
                        return null;
                    }
                }

                currentCursor = new LogDeserializer( buffer, XaCommandReaderFactory.DEFAULT ).cursor( currentLog );

                if ( !nextEntry() )
                {
                    closeCurrentLog();
                    return fetchNextOrNull();
                }

                return currentTx;
            }
            catch(IOException e)
            {
                try
                {
                    closeCurrentLog();
                }
                catch ( IOException e1 )
                {
                    e1.printStackTrace();
                }
                return null;
            }
        }

        protected abstract boolean shouldIncludeTx( long txId );
        protected abstract String chooseStartLog( Iterator<String> iterator ) throws IOException;

        protected boolean nextEntry() throws IOException
        {
            currentTx = null;
            while(currentCursor.next( consumer ))
            {
                if( currentTx != null)
                {
                    return true;
                }
            }
            return false; // End of file
        }

        /**
         * Delegate to child-class to pick start log, and then build an iterator of log entries past that start point
         * to inspect.
         */
        private Iterator<String> createLogIterator( File db, Direction dir )
        {
            try
            {
                String start = chooseStartLog( filenamesOf( db, "nioneo_logical.log", dir ).iterator() );
                Iterable<String> logPaths = filenamesOf( db, "nioneo_logical.log", dir );

                Iterator<String> logIter = logPaths.iterator();
                while(logIter.hasNext())
                {
                    String next = logIter.next();
                    if( next.equals( start ))
                    {
                        break;
                    }
                    else
                    {
                        logIter.remove();
                    }
                }
                return logPaths.iterator();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }

        private void closeCurrentLog() throws IOException
        {
            if(currentCursor != null)
            {
                currentCursor.close();
                currentCursor = null;
            }
            if(currentLog != null)
            {
                currentLog.close();
                currentLog = null;
            }
        }

        @Override
        public void close()
        {
            try
            {
                closeCurrentLog();
            }catch(IOException e)
            {
                throw new RuntimeException( e );
            }
        }
    }


    private class ForwardLogEntryIterator extends LogEntryIterator
    {
        private final long startAtTx;

        public ForwardLogEntryIterator( File db, Direction dir, long startAtTx )
        {
            super( db, dir );
            this.startAtTx = startAtTx;
        }

        @Override
        protected boolean shouldIncludeTx( long txId )
        {
            return startAtTx == -1 || txId >= startAtTx;
        }

        @Override
        protected String chooseStartLog( Iterator<String> iterator ) throws IOException
        {
            String prev = null;
            while(iterator.hasNext())
            {
                String current = iterator.next();
                buffer.clear();
                try(StoreChannel currentChannel = fs.open( new File( current ), "r" ))
                {
                    long[] header = VersionAwareLogEntryReader.readLogHeader( buffer, currentChannel, false );
                    if ( shouldIncludeTx( header[1] ) )
                    {
                        return prev == null ? current : prev;
                    }
                }
                prev = current;
            }
            return prev;
        }
    }

    private class BackwardLogEntryIterator extends LogEntryIterator
    {
        private final long startAtTx;

        private LinkedList<Tx> currentEntries;

        public BackwardLogEntryIterator( File db, Direction dir )
        {
            this(db, dir, -1);
        }

        public BackwardLogEntryIterator( File db, Direction dir, long startAtTx )
        {
            super( db, dir );
            this.startAtTx = startAtTx;
        }

        @Override
        protected boolean shouldIncludeTx( long tx )
        {
            return startAtTx == -1 || tx <= startAtTx;
        }

        @Override
        protected String chooseStartLog( Iterator<String> iterator ) throws IOException
        {
            while(iterator.hasNext())
            {
                String current = iterator.next();
                buffer.clear();
                try(StoreChannel currentChannel = fs.open( new File( current ), "r" ))
                {
                    long[] header = VersionAwareLogEntryReader.readLogHeader( buffer, currentChannel, false );
                    if ( shouldIncludeTx( header[1] ) )
                    {
                        return current;
                    }
                }
            }
            return null;
        }

        @Override
        protected boolean nextEntry() throws IOException
        {
            if(currentEntries == null)
            {
                currentEntries = new LinkedList<>();
                while ( currentCursor.next( consumer ) )
                {
                    if ( currentTx != null )
                    {
                        currentEntries.addFirst( currentTx );
                        currentTx = null;
                    }
                }
            }

            if(currentEntries.peek() == null)
            {
                currentEntries = null;
                return false;
            }
            currentTx = currentEntries.pop();
            return true;
        }
    }
}
