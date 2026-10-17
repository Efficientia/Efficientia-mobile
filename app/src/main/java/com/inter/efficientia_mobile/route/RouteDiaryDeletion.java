package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Deletes a server draft only after the API confirms it was removed. */
public final class RouteDiaryDeletion {
    private RouteDiaryDeletion() { }

    public static void confirmServerDraft(Activity activity, int reportId, String label,
                                          Runnable onDeleted) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.main_home_delete_title)
                .setMessage(activity.getString(R.string.main_home_delete_message, label))
                .setNegativeButton(R.string.main_home_delete_cancel, null)
                .setPositiveButton(R.string.main_home_delete_confirm,
                        (dialog, ignored) -> deleteServerDraft(activity, reportId, label, onDeleted))
                .show();
    }

    private static void deleteServerDraft(Activity activity, int reportId, String label,
                                          Runnable onDeleted) {
        String auth = SessionManager.authorizationHeader(activity);
        if (auth == null) {
            Toast.makeText(activity, R.string.reports_login_diary, Toast.LENGTH_LONG).show();
            return;
        }
        RetrofitClient.getInstance().create(RouteDiaryService.class)
                .delete(auth, reportId).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (response.isSuccessful()) {
                            RouteDiaryDraft.clearForReport(activity, reportId);
                            RouteDiaryReportIndex.forget(activity, reportId);
                            Toast.makeText(activity,
                                    activity.getString(R.string.main_home_delete_success, label),
                                    Toast.LENGTH_SHORT).show();
                            onDeleted.run();
                        } else {
                            new AlertDialog.Builder(activity)
                                    .setMessage(activity.getString(
                                            R.string.reports_delete_error, response.code()))
                                    .setPositiveButton(R.string.dialog_understood, null).show();
                        }
                    }

                    @Override public void onFailure(Call<Void> call, Throwable error) {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            new AlertDialog.Builder(activity)
                                    .setMessage(R.string.reports_delete_offline)
                                    .setPositiveButton(R.string.dialog_understood, null).show();
                        }
                    }
                });
    }
}
