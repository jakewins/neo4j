/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.io.pagecache.PagedFile;
import org.neo4j.kernel.api.impl.index.storage.DirectoryFactory;
import org.neo4j.unsafe.impl.internal.dragons.FeatureToggles;

// This extends FSDirectory, because Lucenes file locks class asserts it's being used
// with a subclass of FSDirectory; we should replace FSLockFactory.getDefault() with
// one that relies on locks via FileSystemAbstraction to reduce coupling with FSDirectory
public class PagedDirectory extends FSDirectory
{
    private final PageCache pageCache;
    private final FileSystemAbstraction fs;

    public static class Factory implements DirectoryFactory
    {
        private final int MAX_MERGE_SIZE_MB = FeatureToggles.getInteger( DirectoryFactory.class, "max_merge_size_mb", 5 );
        private final int MAX_CACHED_MB = FeatureToggles.getInteger( DirectoryFactory.class, "max_cached_mb", 50 );

        private final PageCache pageCache;

        public Factory( PageCache pageCache )
        {
            this.pageCache = pageCache;
        }

        @SuppressWarnings( "ResultOfMethodCallIgnored" )
        @Override
        public Directory open( File dir ) throws IOException
        {
            dir.mkdirs();
            return new NRTCachingDirectory( new PagedDirectory( dir.toPath(), pageCache ), MAX_MERGE_SIZE_MB, MAX_CACHED_MB );
        }

        @Override
        public void close()
        {
            // No resources to release. This method only exists as a hook for test implementations.
        }

        @Override
        public void dumpToZip( ZipOutputStream zip, byte[] scratchPad )
        {
            // do nothing
        }
    }

    private final Path directory; // The underlying filesystem directory

    public PagedDirectory( Path path, PageCache pageCache ) throws IOException
    {
        super( path, FSLockFactory.getDefault() );
        this.pageCache = pageCache;
        this.fs = pageCache.getCachedFileSystem();
        this.directory = path;
    }

    /** Creates an IndexInput for the file with the given name. */
    @Override
    public IndexInput openInput( String name, IOContext context ) throws IOException
    {
        ensureOpen();
        Path path = directory.resolve( name );
        String resourceDescription = "PagedIndexInput(path=\"" + path.toString() + "\")";
        PagedFile file = pageCache.map( path.toFile(), pageCache.pageSize(), StandardOpenOption.READ );
        return new PagedIndexInput( resourceDescription, file, pageCache.getCachedFileSystem().getFileSize( path.toFile() ) );
    }

    /**
     * Lists all files (including subdirectories) in the
     * directory.
     *
     * @throws IOException if there was an I/O error during listing
     */
    private static String[] listAll0( Path dir ) throws IOException
    {
        List<String> entries = new ArrayList<>();

        try ( DirectoryStream<Path> stream = Files.newDirectoryStream( dir ) )
        {
            for ( Path path : stream )
            {
                entries.add( path.getFileName().toString() );
            }
        }

        return entries.toArray( new String[entries.size()] );
    }

    @Override
    public String[] listAll() throws IOException
    {
        ensureOpen();
        return listAll0( directory );
    }

    /** Returns the length in bytes of a file in the directory. */
    @Override
    public long fileLength( String name ) throws IOException
    {
        ensureOpen();
        return Files.size( directory.resolve( name ) );
    }

    /** Removes an existing file in the directory. */
    @Override
    public void deleteFile( String name ) throws IOException
    {
        ensureOpen();
        Files.delete( directory.resolve( name ) );
    }

    /** Creates an IndexOutput for the file with the given name. */
    @Override
    public IndexOutput createOutput( String name, IOContext context ) throws IOException
    {
        ensureOpen();
        return new PagedIndexOutput( directory.resolve( name ), fs );
    }

    @Override
    public void sync( Collection<String> names ) throws IOException
    {
        ensureOpen();

        for ( String name : names )
        {
            fsync0( name );
        }
    }

    @Override
    public void renameFile( String source, String dest ) throws IOException
    {
        ensureOpen();
        Files.move( directory.resolve( source ), directory.resolve( dest ), StandardCopyOption.ATOMIC_MOVE );
        // TODO: move to FS?
        IOUtils.fsync( directory, true );
    }

    /** Closes the store to future operations. */
    @Override
    public synchronized void close()
    {
        isOpen = false;
    }

    /** For debug output. */
    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + "@" + directory + " lockFactory=" + lockFactory;
    }

    private void fsync0( String name ) throws IOException
    {
        // TODO: Add fsync to fs instead?
        IOUtils.fsync( directory.resolve( name ), false );
    }
}
