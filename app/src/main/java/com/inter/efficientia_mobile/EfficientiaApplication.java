package com.inter.efficientia_mobile;

import android.app.Activity;
import android.app.Application;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/** Plays the brand sound only when the user taps a bull logo. */
public class EfficientiaApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private MediaPlayer mooPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        attachCowLogoClickListeners(activity.getWindow().getDecorView());
    }

    private void attachCowLogoClickListeners(View view) {
        if (view instanceof ImageView && getString(R.string.cow_logo_tag).equals(view.getTag())) {
            view.setContentDescription(getString(R.string.cow_logo_sound_description));
            view.setOnClickListener(logo -> playMoo());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachCowLogoClickListeners(group.getChildAt(i));
            }
        }
    }

    private void playMoo() {
        stopMoo();
        mooPlayer = MediaPlayer.create(this, R.raw.cow_moo);
        if (mooPlayer != null) {
            mooPlayer.setOnCompletionListener(player -> {
                player.release();
                if (mooPlayer == player) {
                    mooPlayer = null;
                }
            });
            mooPlayer.start();
        }
    }

    private void stopMoo() {
        if (mooPlayer != null) {
            mooPlayer.setOnCompletionListener(null);
            mooPlayer.release();
            mooPlayer = null;
        }
    }

    @Override public void onActivityPaused(Activity activity) { stopMoo(); }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivityDestroyed(Activity activity) { }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
}
