package org.neo4j.kernel.api.impl.index.storage.paged;

import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.Constants;
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

public class PagedDirectory extends BaseDirectory
{
    private final PageCache pageCache;
    private final FileSystemAbstraction fs;

    public static class Factory implements DirectoryFactory
    {
        private final int MAX_MERGE_SIZE_MB =
                FeatureToggles.getInteger( DirectoryFactory.class, "max_merge_size_mb", 5 );
        private final int MAX_CACHED_MB =
                FeatureToggles.getInteger( DirectoryFactory.class, "max_cached_mb", 50 );

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

    /**
     * Default max chunk size.
     */
    private static final int DEFAULT_MAX_CHUNK_SIZE = Constants.JRE_IS_64BIT ? (1 << 30) : (1 << 28);
    private final int chunkSizePower;

    public PagedDirectory(Path path, PageCache pageCache) throws IOException {
        super(FSLockFactory.getDefault());
        this.pageCache = pageCache;
        this.fs = pageCache.getCachedFileSystem();
        this.directory = path.toRealPath();
        this.chunkSizePower = 31 - Integer.numberOfLeadingZeros(DEFAULT_MAX_CHUNK_SIZE);
        assert this.chunkSizePower >= 0 && this.chunkSizePower <= 30;
    }

    /** Creates an IndexInput for the file with the given name. */
    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException
    {
        ensureOpen();
        Path path = directory.resolve( name );
        String resourceDescription = "MMapIndexInput(path=\"" + path.toString() + "\")";
        PagedFile file = pageCache.map( path.toFile(), pageCache.pageSize(), StandardOpenOption.READ );
        return PagedIndexInput.newInstance( resourceDescription, file, chunkSizePower, true );
    }


    /** Lists all files (including subdirectories) in the
     *  directory.
     *
     *  @throws IOException if there was an I/O error during listing */
    private static String[] listAll( Path dir ) throws IOException {
        List<String> entries = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                entries.add(path.getFileName().toString());
            }
        }

        return entries.toArray(new String[entries.size()]);
    }

    @Override
    public String[] listAll() throws IOException {
        ensureOpen();
        return listAll(directory);
    }

    /** Returns the length in bytes of a file in the directory. */
    @Override
    public long fileLength(String name) throws IOException {
        ensureOpen();
        return Files.size(directory.resolve(name));
    }

    /** Removes an existing file in the directory. */
    @Override
    public void deleteFile(String name) throws IOException {
        ensureOpen();
        Files.delete(directory.resolve(name));
    }

    /** Creates an IndexOutput for the file with the given name. */
    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        ensureOpen();
        return new PagedIndexOutput( directory.resolve( name ), fs );
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        ensureOpen();

        for (String name : names) {
            fsync(name);
        }
    }

    @Override
    public void renameFile(String source, String dest) throws IOException {
        ensureOpen();
        Files.move(directory.resolve(source), directory.resolve(dest), StandardCopyOption.ATOMIC_MOVE);
        // TODO: should we move directory fsync to a separate 'syncMetadata' method?
        // for example, to improve listCommits(), IndexFileDeleter could also call that after deleting segments_Ns
        IOUtils.fsync(directory, true);
    }

    /** Closes the store to future operations. */
    @Override
    public synchronized void close() {
        isOpen = false;
    }

    /** For debug output. */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + directory + " lockFactory=" + lockFactory;
    }

    private void fsync(String name) throws IOException {
        IOUtils.fsync(directory.resolve(name), false);
    }
}
