package io.github.nesouri

import android.os.StrictMode
import groovy.transform.CompileStatic
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile

@CompileStatic
public class Util {
    public static byte[] extract7zip(final File file) throws IOException {
        final SevenZFile sz = new SevenZFile(file)
        while (true) {
            SevenZArchiveEntry entry = sz.getNextEntry()
            if (entry == null)
                break
            final String name = entry.getName()
            if (name.endsWith(".nsf")) {
                final byte[] content = new byte[(int) entry.getSize()]
                sz.read(content)
                return content
            }
        }
        throw new FileNotFoundException("No NSF file found")
    }

    public static void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build())
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build())
    }
}