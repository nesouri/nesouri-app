package io.github.nesouri;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.annimon.stream.Stream;

import java.util.HashSet;
import java.util.Set;

import io.github.nesouri.Util.UnsafeFunction;

public class PlaybackServiceConnection implements ServiceConnection {
	public interface PlaybackServiceConnectionListener {
		void onPlaybackServiceConnect(final MediaControllerCompat mediaController);
		void onPlaybackServiceDisconnect();
	}

	private final UnsafeFunction<MediaSessionCompat.Token, MediaControllerCompat> mediaControllerFactory;

	private MediaControllerCompat mediaController;
	private Set<PlaybackServiceConnectionListener> listeners = new HashSet<>(2);
	private Set<PlaybackServiceConnectionListener> connected = new HashSet<>(2);

	public PlaybackServiceConnection(final UnsafeFunction<MediaSessionCompat.Token, MediaControllerCompat> mediaControllerFactory) {
		this.mediaControllerFactory = mediaControllerFactory;
	}

	@Override
	public void onServiceConnected(final ComponentName name, final IBinder service) {
		final PlaybackService.PlaybackBinder binder = (PlaybackService.PlaybackBinder) service;
		final PlaybackService playbackService = binder.getService();
		try {
			mediaController = mediaControllerFactory.apply(playbackService.getSessionToken());
			Stream.of(listeners).forEach(l -> l.onPlaybackServiceConnect(mediaController));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onServiceDisconnected(final ComponentName name) {
		Stream.of(connected).forEach(PlaybackServiceConnectionListener::onPlaybackServiceDisconnect);
		connected.clear();
		mediaController = null;
	}

	public void registerListener(final PlaybackServiceConnectionListener listener) {
		listeners.add(listener);
		if (mediaController != null) {
			listener.onPlaybackServiceConnect(mediaController);
			connected.add(listener);
		}
	}

	public void unregisterListener(final PlaybackServiceConnectionListener listener) {
		if (mediaController != null) {
			listener.onPlaybackServiceDisconnect();
			connected.remove(listener);
		}
		listeners.remove(listener);
	}
}