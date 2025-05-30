package net.minecraft.util;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.FileUtils;

public class SessionLock implements AutoCloseable {

    public static final String LOCK_FILE = "session.lock";
    private final FileChannel lockFile;
    private final FileLock lock;
    private static final ByteBuffer DUMMY;

    public static SessionLock create(Path path) throws IOException {
        Path path1 = path.resolve("session.lock");

        FileUtils.createDirectoriesSafe(path);
        FileChannel filechannel = FileChannel.open(path1, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        try {
            filechannel.write(SessionLock.DUMMY.duplicate());
            filechannel.force(true);
            FileLock filelock = filechannel.tryLock();

            if (filelock == null) {
                throw SessionLock.ExceptionWorldConflict.alreadyLocked(path1);
            } else {
                return new SessionLock(filechannel, filelock);
            }
        } catch (IOException ioexception) {
            try {
                filechannel.close();
            } catch (IOException ioexception1) {
                ioexception.addSuppressed(ioexception1);
            }

            throw ioexception;
        }
    }

    private SessionLock(FileChannel filechannel, FileLock filelock) {
        this.lockFile = filechannel;
        this.lock = filelock;
    }

    public void close() throws IOException {
        try {
            if (this.lock.isValid()) {
                this.lock.release();
            }
        } finally {
            if (this.lockFile.isOpen()) {
                this.lockFile.close();
            }

        }

    }

    public boolean isValid() {
        return this.lock.isValid();
    }

    public static boolean isLocked(Path path) throws IOException {
        Path path1 = path.resolve("session.lock");

        try {
            boolean flag;

            try (FileChannel filechannel = FileChannel.open(path1, StandardOpenOption.WRITE); FileLock filelock = filechannel.tryLock();) {
                flag = filelock == null;
            }

            return flag;
        } catch (AccessDeniedException accessdeniedexception) {
            return true;
        } catch (NoSuchFileException nosuchfileexception) {
            return false;
        }
    }

    static {
        byte[] abyte = "\u2603".getBytes(Charsets.UTF_8);

        DUMMY = ByteBuffer.allocateDirect(abyte.length);
        SessionLock.DUMMY.put(abyte);
        SessionLock.DUMMY.flip();
    }

    public static class ExceptionWorldConflict extends IOException {

        private ExceptionWorldConflict(Path path, String s) {
            String s1 = String.valueOf(path.toAbsolutePath());

            super(s1 + ": " + s);
        }

        public static SessionLock.ExceptionWorldConflict alreadyLocked(Path path) {
            return new SessionLock.ExceptionWorldConflict(path, "already locked (possibly by other Minecraft instance?)");
        }
    }
}
