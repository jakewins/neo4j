import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.StringTokenizer;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.LongByReference;
import sun.misc.JavaIOFileDescriptorAccess;
import sun.misc.SharedSecrets;
import sun.nio.ch.FileChannelImpl;

public class DirectIO
{
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();

    public static void main(String ... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, IOException
    {
        String path = "/tmp/test";
        int flags = 0;
        int mode = 0;

        FileChannel channel = open( path, flags, mode );

        channel.close();
    }

    private static FileChannel open( String path, int flags, int mode ) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class<?> UnixNativeDispatcher = DirectIO.class.getClassLoader().loadClass( "sun.nio.fs.UnixNativeDispatcher" );
        Class<?> UnixPath = DirectIO.class.getClassLoader().loadClass( "sun.nio.fs.UnixPath" );
        Class<?> UnixFileSystem = DirectIO.class.getClassLoader().loadClass( "sun.nio.fs.UnixFileSystem" );

        Method open = UnixNativeDispatcher.getDeclaredMethod( "open", UnixPath, int.class, int.class );
        open.setAccessible( true );

        Constructor<?> UnixPath_constructor = UnixPath.getDeclaredConstructor( UnixFileSystem, String.class );
        UnixPath_constructor.setAccessible( true );

        Object unixPath = UnixPath_constructor.newInstance( FileSystems.getDefault(), path );
        int fp = (int) open.invoke( null, unixPath, flags, mode );
        FileDescriptor fd = new FileDescriptor();
        fdAccess.set( fd, fp );
        return FileChannelImpl.open( fd, true, true, null );;
    }



    interface Cache
    {
        enum Lock
        {
            SHARED,
            EXCLUSIVE;
        }

        void pin(PageCursor cursor, Lock lock, long pageId);
        void unpin(long pageId);
    }


    interface PageCursor
    {
        byte get();
        byte get(int offset);
        void put(byte b);

        void reset(ByteBuffer backingPage);
    }



    public static class OSXSyscalls
    {
        public static final int AUE_FCNTL = 92;

    }

    public static class OSXFCNTL
    {
        public static final int F_DUPFD	         = 0;		/* duplicate file descriptor */
        public static final int F_GETFD	         = 1;		/* get file descriptor flags */
        public static final int F_SETFD	         = 2;		/* set file descriptor flags */
        public static final int F_GETFL	         = 3;		/* get file status flags */
        public static final int F_SETFL	         = 4;		/* set file status flags */
        public static final int F_GETOWN         = 5;		/* get SIGIO/SIGURG proc/pgrp */
        public static final int F_SETOWN         = 6;		/* set SIGIO/SIGURG proc/pgrp */
        public static final int F_GETLK	         = 7;		/* get record locking information */
        public static final int F_SETLK	         = 8;		/* set record locking information */
        public static final int F_SETLKW         = 9;		/* F_SETLK; wait if blocked */
        public static final int F_FLUSH_DATA     = 40;
        public static final int F_CHKCLEAN       = 41;      /* Used for regression test */
        public static final int F_PREALLOCATE    = 42;		/* Preallocate storage */
        public static final int F_SETSIZE        = 43;		/* Truncate a file without zeroing space */
        public static final int F_RDADVISE       = 44;      /* Issue an advisory read async with no copy to user */
        public static final int F_RDAHEAD        = 45;      /* turn read ahead off/on for this fd */
        public static final int F_READBOOTSTRAP  = 46;      /* Read bootstrap from disk */
        public static final int F_WRITEBOOTSTRAP = 47;      /* Write bootstrap on disk */
        public static final int F_NOCACHE        = 48;      /* turn data caching off/on for this fd */
        public static final int F_LOG2PHYS	     = 49;		/* file offset to device offset */
        public static final int F_GETPATH        = 50;      /* return the full path of the fd */
        public static final int F_FULLFSYNC      = 51;		/* fsync + ask the drive to flush to the media */
        public static final int F_PATHPKG_CHECK  = 52;      /* find which component (if any) is a package */
        public static final int F_FREEZE_FS      = 53;      /* "freeze" all fs operations */
        public static final int F_THAW_FS        = 54;      /* "thaw" all fs operations */
        public static final int F_GLOBAL_NOCACHE = 55;		/* turn data caching off/on (globally) for this file */
        public static final int F_NODIRECT	     = 62; /* ??? */
    }



    public static class Test {
        public interface CStdLib extends Library
        {
            int syscall(int number, Object... args);
        }

        public static void main(String[] args) throws IOException, NoSuchFieldException, IllegalAccessException,
                InterruptedException
        {
            public interface CStdLib extends Library
            {
                int syscall(int number, Object... args);
            }

            private static int filePointer( FileChannel chan ) throws NoSuchFieldException, IllegalAccessException
            {
                Field fd_field = FileChannelImpl.class.getDeclaredField( "fd" );
                fd_field.setAccessible( true );
                FileDescriptor fd = (FileDescriptor) fd_field.get( chan );

                return fdAccess.get( fd );
            }

            public static void main(String ... args)
            {
                CStdLib c = (CStdLib) Native.loadLibrary( "c", CStdLib.class );

                File file = new File( "/tmp/test" );
                FileChannel chan = FileChannel.open( file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ );
                c.syscall( OSXSyscalls.AUE_FCNTL, filePointer( chan ), OSXFCNTL.F_NOCACHE, 1 );

            }

            if(file.exists())
            {
                file.delete();
            }

            // Allocate some data to write
            int bufferSize = 1024 * 4 * 1024 * 32;
            ByteBuffer buffer = ByteBuffer.allocateDirect( bufferSize );

            {
                System.out.println("[DIRECT]");
                for ( int i = 0; i < 10; i++ )
                {
                    FileChannel chan = createDirect( c, file );
                    System.out.println("Run " + i);
                    bench( chan, bufferSize, buffer );
                    chan.close();
                }
            }

            {
                System.out.println("[CACHED]");
                for ( int i = 0; i < 10; i++ )
                {
                    FileChannel chan = create( file );
                    System.out.println("Run " + i);
                    bench( chan, bufferSize, buffer );
                    chan.close();
                }
            }

        }

        private static FileChannel create( File file ) throws IOException
        {
            if(file.exists())
            {
                file.delete();
            }
            return FileChannel.open( file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ );
        }

        private static FileChannel createDirect( CStdLib c, File file ) throws IOException, NoSuchFieldException, IllegalAccessException
        {
            FileChannel chan = create( file );
            c.syscall( OSXSyscalls.AUE_FCNTL, filePointer( chan ), OSXFCNTL.F_NOCACHE, 1 );
            c.syscall( OSXSyscalls.AUE_FCNTL, filePointer( chan ), OSXFCNTL.F_NODIRECT, 0 );
            return chan;
        }

        private static void bench( FileChannel chan, int bufferSize, ByteBuffer buffer ) throws IOException
        {
            // And then write that data to the file
            int timesToWrite = 10;
            long start = System.currentTimeMillis();
            for ( int i = 0; i < timesToWrite; i++ )
            {
                buffer.clear();
                for ( int i1 = i; i1 < buffer.capacity(); i1++ )
                {
                    buffer.put( (byte) (i1) );
                }
                buffer.position( 0 );
                chan.write( buffer );
                chan.force( false );
            }
            long delta = System.currentTimeMillis() - start;

            System.out.println( "Wrote " + (buffer.capacity() * timesToWrite) / delta + " B/ms" );


            int timesToRead = 10;
            chan.position(0);
            start = System.currentTimeMillis();
            for ( int i = 0; i < timesToRead; i++ )
            {
                buffer.clear();
                chan.read( buffer );
            }
            delta = System.currentTimeMillis() - start;

            System.out.println( "Read " + (bufferSize * timesToRead) / delta + " B/ms" );
        }

        private static int filePointer( FileChannel chan ) throws NoSuchFieldException, IllegalAccessException
        {
            Field fd_field = FileChannelImpl.class.getDeclaredField( "fd" );
            fd_field.setAccessible( true );
            FileDescriptor fd = (FileDescriptor) fd_field.get( chan );

            return fdAccess.get( fd );
        }
    }

    public static class ThreadAffinity {

        private static final Core[] cores;

        static {
            final int coresCount = Runtime.getRuntime().availableProcessors();
            cores = new Core[coresCount];

            for ( int i = 0; i < cores.length; i++ ) {
                cores[i] = new Core( i );
            }
        }

        public static final class Core {
            private final int sequence;

            public Core( final int sequence ) {
                this.sequence = sequence;
                if ( sequence > Integer.SIZE ) {
                    throw new IllegalStateException( "Too many cores (" + sequence + ") for integer mask" );
                }
            }

            public int sequence() {
                return sequence;
            }

            public void attachTo() throws Exception {

                final long mask = mask();

                setCurrentThreadAffinityMask( mask );
            }

            public void attach( final Thread thread ) throws Exception {
                final long mask = mask();
                //fixme: it does not work for now!
                setThreadAffinityMask( thread.getId(), mask );
            }

            private int mask() {
                return 1 << sequence;
            }


            @Override
            public String toString() {
                return String.format( "Core[#%d]", sequence() );
            }

        }

        public static void setCurrentThreadAffinityMask( final long mask ) throws Exception {
            final CLibrary lib = CLibrary.INSTANCE;
            final int cpuMaskSize = Long.SIZE / 8;
            try {
                final int ret = lib.sched_setaffinity( 0, cpuMaskSize, new LongByReference( mask ) );
                if ( ret < 0 ) {
                    throw new Exception( "sched_setaffinity( 0, (" + cpuMaskSize + ") , &(" + mask + ") ) return " + ret );
                }
            } catch ( Throwable e ) {
                throw new Exception( e );
            }
        }

        public static void setThreadAffinityMask( final long threadID,
                                                  final long mask ) throws Exception {
            final CLibrary lib = CLibrary.INSTANCE;
            final int cpuMaskSize = Long.SIZE / 8;
            try {
                final int ret = lib.sched_setaffinity(
                        ( int ) threadID,
                        cpuMaskSize,
                        new LongByReference( mask )
                );
                if ( ret < 0 ) {
                    throw new Exception( "sched_setaffinity( " + threadID + ", (" + cpuMaskSize + ") , &(" + mask + ") ) return " + ret );
                }
            } catch ( Throwable e ) {
                throw new Exception( e );
            }
        }

        public static Core[] cores() {
            return cores.clone();
        }

        public static Core currentCore() {
            final int cpuSequence = CLibrary.INSTANCE.sched_getcpu();
            return cores[cpuSequence];
        }

        public static void nice( final int increment ) throws Exception {
            final CLibrary lib = CLibrary.INSTANCE;
            try {
                final int ret = lib.nice( increment );
                if ( ret < 0 ) {
                    throw new Exception( "nice( " + increment + " ) return " + ret );
                }
            } catch ( Throwable e ) {
                throw new Exception( e );
            }
        }

        private interface CLibrary extends Library {
            public static final CLibrary INSTANCE = ( CLibrary )
                    Native.loadLibrary( "c", CLibrary.class );

            public int nice( final int increment ) throws LastErrorException;

            public int sched_setaffinity( final int pid,
                                          final int cpusetsize,
                                          final PointerType cpuset ) throws LastErrorException;

            public int sched_getcpu() throws LastErrorException;
        }

        public static void main( final String[] args ) throws Exception {

            final Core currentCore = currentCore();
//        System.out.printf( "currentCore() -> %s\n", currentCore );

//        final int niceRet = lib.nice( -20 );
//        System.out.printf( "nice -> %d\n", niceRet );


            for ( final Core core : cores() ) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            core.attachTo();
                            System.out.printf( "currentCore() -> %s\n", currentCore() );
                            for ( int i = 0; i < Integer.MAX_VALUE; i++ ) {
                                i--;
                            }
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        }
                    }
                }.start();


            }

        }

        public static int[] parseCoresIndexes( final String str,
                                               final int[] defaults ) throws ParseException {
            final StringTokenizer stok = new StringTokenizer( str, "," );
            final int size = stok.countTokens();
            if ( size == 0 ) {
                return defaults;
            }

            final int maxIndex = Runtime.getRuntime().availableProcessors() - 1;
            final int[] indexes = new int[size];
            for ( int i = 0; stok.hasMoreTokens(); i++ ) {
                final String token = stok.nextToken();
                final int index;
                try {
                    index = Integer.parseInt( token );
                } catch ( NumberFormatException e ) {
                    throw new ParseException( "Can't parse [" + i + "]='" + token + "' as Integer", i );
                }
                if ( index > maxIndex || index < 0 ) {
                    throw new ParseException( "Core index[" + i + "]=" + index + " is out of bounds [0," + maxIndex + "]", i );
                }
                indexes[i] = index;
            }
            return indexes;
        }

        public static Core[] parseCores( final String str,
                                         final int[] defaults ) throws ParseException {
            final int[] indexes = parseCoresIndexes( str, defaults );
            final Core[] cores = new Core[indexes.length];
            for ( int i = 0; i < cores.length; i++ ) {
                cores[i] = cores()[indexes[i]];
            }
            return cores;
        }
    }

}
