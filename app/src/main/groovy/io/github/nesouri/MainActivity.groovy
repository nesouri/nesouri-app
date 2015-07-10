package io.github.nesouri

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.arasthel.swissknife.SwissKnife
import com.arasthel.swissknife.annotations.InjectView
import groovy.transform.CompileStatic
import rx.Observer
import rx.Subscriber
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.android.view.OnClickEvent
import rx.android.view.ViewObservable
import rx.functions.Action1
import rx.observers.Subscribers
import rx.schedulers.Schedulers

import static rx.android.schedulers.AndroidSchedulers.handlerThread

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

@CompileStatic
class MainActivity extends AppCompatActivity {

    @InjectView(R.id.text)
    TextView text

    @InjectView(R.id.button)
    Button button

    @InjectView(R.id.imageView)
    ImageView imageView

    BackgroundThread backgroundThread
    Handler backgroundHandler

    Subscription downloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        contentView = R.layout.activity_main

        SwissKnife.inject(this)
        SwissKnife.restoreState(this, savedInstanceState)
        SwissKnife.loadExtras(this)

        text.text = "Hello Injected World!"

        backgroundThread = new BackgroundThread()
        backgroundThread.daemon = true
        backgroundThread.start()
        backgroundHandler = new Handler(backgroundThread.getLooper())

        def url = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/NES-Console-Set.png/640px-NES-Console-Set.png"

        ViewObservable.clicks(button, false)
                .observeOn(handlerThread(backgroundHandler))
                .map({ OnClickEvent event ->
                    new URL(url).withInputStream {
                        BitmapFactory.decodeStream(it)
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ Bitmap b -> imageView.imageBitmap = b })
    }

    @Override
    boolean onCreateOptionsMenu(Menu menu) {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @Override
    boolean onOptionsItemSelected(MenuItem item) {
        if (item.itemId == R.id.action_settings)
            return true
        return super.onOptionsItemSelected(item)
    }

    static class BackgroundThread extends HandlerThread {
        BackgroundThread() {
            super("BackgroundThread", THREAD_PRIORITY_BACKGROUND);
        }
    }
}