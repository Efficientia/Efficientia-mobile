package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import com.inter.efficientia_mobile.R;

import java.util.List;

/** Lets the driver start a distinct trip or resume one of their local diaries. */
public final class RouteDiaryFlow {
    private RouteDiaryFlow() { }

    public static void open(Activity activity) {
        List<RouteDiaryDraft> saved = RouteDiaryDraft.saved(activity);
        if (saved.isEmpty()) {
            startNew(activity);
            return;
        }
        String[] choices = new String[saved.size() + 1];
        choices[0] = activity.getString(R.string.route_new);
        for (int i = 0; i < saved.size(); i++)
            choices[i + 1] = activity.getString(R.string.route_resume, saved.get(i).label(activity));
        new AlertDialog.Builder(activity)
                .setTitle(R.string.route_diary_title)
                .setMessage(R.string.route_choose_message)
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.route_start_another_title)
                                .setMessage(R.string.route_start_another_message)
                                .setNegativeButton(R.string.main_home_delete_cancel, null)
                                .setPositiveButton(R.string.route_new, (confirm, ignored) -> startNew(activity))
                                .show();
                    } else {
                        activity.startActivity(saved.get(which - 1).resume(activity));
                    }
                }).show();
    }

    public static void startNew(Activity activity) {
        RouteDiaryDraft draft = RouteDiaryDraft.startNew(activity);
        activity.startActivity(draft.next(activity, RouteDiaryStepOneActivity.class));
    }

    public static String currentRouteSummary(Context context) {
        String id = RouteDiaryDraft.activeId(context);
        if (id == null) return context.getString(R.string.route_none_in_progress);
        RouteDiaryDraft draft = RouteDiaryDraft.find(context, id);
        String origin = draft.get(R.id.inputRouteOrigin).trim();
        String destination = draft.get(R.id.inputRouteDestination).trim();
        if (origin.isEmpty() && destination.isEmpty()) return context.getString(R.string.route_new_summary);
        return context.getString(R.string.route_in_progress,
                origin.isEmpty() ? context.getString(R.string.route_origin_pending) : origin,
                destination.isEmpty() ? context.getString(R.string.route_destination_pending) : destination);
    }
}
