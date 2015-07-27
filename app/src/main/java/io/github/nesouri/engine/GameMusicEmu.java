package io.github.nesouri.engine;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class GameMusicEmu implements Closeable, AutoCloseable
{
    static {
        System.loadLibrary("GameMusicEmu");
    }

	public static final int TRACK_INFO_ONLY = -1;

	private long nativeHandle;

    public native static GameMusicEmu create(final EngineType engine, final int sampleRate);
	public native static GameMusicEmu create(final String path, final int sampleRate) throws IOException;
	public native static GameMusicEmu create(final ByteBuffer buffer, final int sampleRate) throws IOException;
    public native static GameMusicEmu create(final byte[] buffer, final int sampleRate) throws IOException;

    public native void load(final String filename) throws IOException;
    public native void load(final ByteBuffer buffer) throws IOException;
    public native void load(final byte[] buffer) throws IOException;

	public native int trackCount();
	public native void track(final int track) throws IOException;

	public native TrackInfo info(final int track) throws IOException;

	public native int position();
	public native int positionSamples();

	public native int seek(int offset_ms);
	public native int seekSamples(int offset);

	public native int endAt(int offset_ms);

	public native boolean eof();

	public native int read(final ByteBuffer buffer) throws IOException;
	public native int read(final byte[] buffer) throws IOException;

	public native void stereoDepth(final float stereo);
	public native void enableAccuracy(final boolean enabled);

    @Override
	public native void close();
}
