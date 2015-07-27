package io.github.nesouri

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import groovy.transform.CompileStatic
import io.github.nesouri.fragments.GamesList
import io.github.nesouri.fragments.PlaybackControl

@CompileStatic
class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.name

    final PlaybackServiceConnection serviceConnection = new PlaybackServiceConnection(this)

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        // Util.enableStrictMode()

        bindService(new Intent(this, PlaybackService.class), serviceConnection, BIND_AUTO_CREATE)

        contentView = R.layout.activity_main

        if (!savedInstanceState) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.content_frame, GamesList.newInstance())
            .commit()
        }
    }

    @Override
    void onStart() {
        super.onStart()
        serviceConnection.playbackControl = supportFragmentManager.findFragmentById(R.id.fragment_playback_controls) as PlaybackControl
        // hidePlaybackControls(serviceConnection.playbackControl)
    }

    @Override
    void onStop() {
        super.onStop()
        serviceConnection.playbackControl = null

    }

    @Override
    void onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    @Override
    boolean onCreateOptionsMenu(final Menu menu) {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @Override
    boolean onOptionsItemSelected(final MenuItem item) {
        if (item.itemId == R.id.action_settings)
            return true
        return super.onOptionsItemSelected(item)
    }

    def showPlaybackControls(final Fragment fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom,
                R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom)
            .show(fragment)
        .commit()
    }

    def hidePlaybackControls(final Fragment fragment) {
        supportFragmentManager.beginTransaction()
                .hide(fragment)
        .commit()
    }
}