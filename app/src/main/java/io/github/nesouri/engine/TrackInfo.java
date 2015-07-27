package io.github.nesouri.engine;

import java.io.Closeable;

public class TrackInfo implements Closeable, AutoCloseable
{
	static {
		System.loadLibrary("GameMusicEmu");
	}

	private long nativeHandle;

	public native int duration(final DurationType type);

	public native String system();

	public native String game();

	public native String song();

	public native String author();

	public native String copyright();

	public native String comment();

	public native String dumper();

    @Override
	public native void close();
}
