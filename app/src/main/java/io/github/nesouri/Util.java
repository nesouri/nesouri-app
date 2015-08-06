package io.github.nesouri;

import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java8.util.function.Function;

public class Util {
	public static byte[] extract7zip(final File file) throws IOException {
		final SevenZFile sz = new SevenZFile(file);
		while (true) {
			SevenZArchiveEntry entry = sz.getNextEntry();
			if (entry == null)
				break;
			final String name = entry.getName();
			if (name.endsWith(".nsf")) {
				final byte[] content = new byte[(int) entry.getSize()];
				sz.read(content);
				return content;
			}
		}
		throw new FileNotFoundException("No NSF file found");
	}

	public static void enableStrictMode() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				                           .detectDiskReads()
				                           .detectDiskWrites()
				                           .detectNetwork()
				                           .penaltyLog()
				                           .build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				                       .detectLeakedSqlLiteObjects()
				                       .detectLeakedClosableObjects()
				                       .penaltyLog()
				                       .penaltyDeath()
				                       .build());
	}

	public static <T,R> R unlessNull(final T value, final Function<T,R> fun) {
		if (value == null)
			return null;
		return fun.apply(value);
	}

	public static <T> T findFragmentById(final FragmentActivity activity, final int id) {
		return (T) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_playback_controls);
	}

	public static boolean isNullOrEmpty(final CharSequence value) {
		if (value == null)
			return true;
		return value.length() == 0;
	}
}
