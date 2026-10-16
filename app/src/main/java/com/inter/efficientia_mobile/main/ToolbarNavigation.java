package com.inter.efficientia_mobile.main;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;

import com.inter.efficientia_mobile.R;

/** Keeps section changes instantaneous without changing transitions in the diary flow. */
public final class ToolbarNavigation {
    private ToolbarNavigation() { }

    public static void open(Activity activity, Class<? extends Activity> destination) {
        if (destination.isInstance(activity)) return;
        Intent intent = new Intent(activity, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent, ActivityOptions.makeCustomAnimation(activity,
                R.anim.route_no_transition, R.anim.route_no_transition).toBundle());
        suppress(activity);
    }

    public static void home(Activity activity) {
        if (activity instanceof MainActivity) return;
        open(activity, MainActivity.class);
        activity.finish();
        suppress(activity);
    }

    public static void back(Activity activity) {
        activity.finish();
        suppress(activity);
    }

    private static void suppress(Activity activity) {
        activity.overridePendingTransition(R.anim.route_no_transition,
                R.anim.route_no_transition);
    }
}
