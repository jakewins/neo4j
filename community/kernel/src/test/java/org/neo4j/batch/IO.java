package org.neo4j.batch;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import com.sun.jna.Library;
import com.sun.jna.Native;
import sun.misc.JavaIOFileDescriptorAccess;
import sun.misc.SharedSecrets;
import sun.nio.ch.FileChannelImpl;

public class IO
{
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();

    public static class OSXSyscalls
    {
        public static final int AUE_FCNTL = 92;

    }

    public static class OSXFCNTL
    {
        public static final int F_DUPFD          = 0;       /* duplicate file descriptor */
        public static final int F_GETFD          = 1;       /* get file descriptor flags */
        public static final int F_SETFD          = 2;       /* set file descriptor flags */
        public static final int F_GETFL          = 3;       /* get file status flags */
        public static final int F_SETFL          = 4;       /* set file status flags */
        public static final int F_GETOWN         = 5;       /* get SIGIO/SIGURG proc/pgrp */
        public static final int F_SETOWN         = 6;       /* set SIGIO/SIGURG proc/pgrp */
        public static final int F_GETLK          = 7;       /* get record locking information */
        public static final int F_SETLK          = 8;       /* set record locking information */
        public static final int F_SETLKW         = 9;       /* F_SETLK; wait if blocked */
        public static final int F_FLUSH_DATA     = 40;
        public static final int F_CHKCLEAN       = 41;      /* Used for regression test */
        public static final int F_PREALLOCATE    = 42;      /* Preallocate storage */
        public static final int F_SETSIZE        = 43;      /* Truncate a file without zeroing space */
        public static final int F_RDADVISE       = 44;      /* Issue an advisory read async with no copy to user */
        public static final int F_RDAHEAD        = 45;      /* turn read ahead off/on for this fd */
        public static final int F_READBOOTSTRAP  = 46;      /* Read bootstrap from disk */
        public static final int F_WRITEBOOTSTRAP = 47;      /* Write bootstrap on disk */
        public static final int F_NOCACHE        = 48;      /* turn data caching off/on for this fd */
        public static final int F_LOG2PHYS       = 49;      /* file offset to device offset */
        public static final int F_GETPATH        = 50;      /* return the full path of the fd */
        public static final int F_FULLFSYNC      = 51;      /* fsync + ask the drive to flush to the media */
        public static final int F_PATHPKG_CHECK  = 52;      /* find which component (if any) is a package */
        public static final int F_FREEZE_FS      = 53;      /* "freeze" all fs operations */
        public static final int F_THAW_FS        = 54;      /* "thaw" all fs operations */
        public static final int F_GLOBAL_NOCACHE = 55;      /* turn data caching off/on (globally) for this file */
        public static final int F_NODIRECT       = 62; /* ??? */
    }



    public interface CStdLib extends Library
    {
        int syscall(int number, Object... args);
    }


    private static int fp( FileChannel chan ) throws NoSuchFieldException, IllegalAccessException
    {
        Field fd_field = FileChannelImpl.class.getDeclaredField( "fd" );
        fd_field.setAccessible( true );
        FileDescriptor fd = (FileDescriptor) fd_field.get( chan );

        return fdAccess.get( fd );
    }

    private static int fp( AsynchronousFileChannel chan ) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException
    {
        Class<?> asyncImpl = IO.class.getClassLoader().loadClass( "sun.nio.ch.AsynchronousFileChannelImpl" );
        Field fd_field = asyncImpl.getDeclaredField( "fdObj" );
        fd_field.setAccessible( true );
        FileDescriptor fd = (FileDescriptor) fd_field.get( chan );

        return fdAccess.get( fd );
    }

    public static AsynchronousFileChannel openAsync( String path ) throws IOException
    {
        try
        {
            CStdLib c = (CStdLib) Native.loadLibrary( "c", CStdLib.class );
            AsynchronousFileChannel chan = AsynchronousFileChannel.open( new File( path ).toPath(), StandardOpenOption.READ );
            c.syscall( OSXSyscalls.AUE_FCNTL, fp( chan ), OSXFCNTL.F_NOCACHE, 1 );
            return chan;
        } catch(Exception e)
        {
            throw new RuntimeException( e );
        }
    }
}
