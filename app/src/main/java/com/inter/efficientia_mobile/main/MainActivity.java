package com.inter.efficientia_mobile.main;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;

public class MainActivity extends AppCompatActivity {

    private PopupWindow reportActionsPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String userName = SessionManager.userName(this);
        if (userName.isBlank()) {
            userName = "Motorista";
        }
        TextView greeting = findViewById(R.id.txtMainHomeGreeting);
        greeting.setText(getString(R.string.main_home_greeting, userName));

        int[] pendingActions = {
                R.id.btnHomeNotifications,
                R.id.btnHomeProfile,
                R.id.btnMainReports,
                R.id.btnMainFeedback,
                R.id.btnMainTruck,
                R.id.btnMainAdd,
                R.id.navMainProfile,
                R.id.navMainDocuments,
                R.id.navMainAssistant,
                R.id.navMainSettings
        };
        for (int actionId : pendingActions) {
            findViewById(actionId).setOnClickListener(this::showPendingFeature);
        }

        configureReportMenu(
                R.id.btnRouteOneMore,
                R.id.cardRouteOne,
                R.string.main_home_route_one_date,
                R.string.main_home_route_one_city
        );
        configureReportMenu(
                R.id.btnRouteTwoMore,
                R.id.cardRouteTwo,
                R.string.main_home_route_two_date,
                R.string.main_home_route_two_city
        );
        configureReportMenu(
                R.id.btnRouteThreeMore,
                R.id.cardRouteThree,
                R.string.main_home_route_three_date,
                R.string.main_home_route_three_city
        );
        configureReportMenu(
                R.id.btnRouteFourMore,
                R.id.cardRouteFour,
                R.string.main_home_route_four_date,
                R.string.main_home_route_four_city
        );
    }

    private void showPendingFeature(View ignored) {
        Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show();
    }

    private void configureReportMenu(
            int actionViewId,
            int cardViewId,
            int dateResource,
            int routeResource
    ) {
        View actionView = findViewById(actionViewId);
        View cardView = findViewById(cardViewId);
        String date = getString(dateResource);
        String route = getString(routeResource);
        actionView.setOnClickListener(anchor -> showReportActions(anchor, cardView, date, route));
    }

    private void showReportActions(View anchor, View cardView, String date, String route) {
        if (reportActionsPopup != null) {
            reportActionsPopup.dismiss();
        }

        View content = LayoutInflater.from(this).inflate(R.layout.popup_report_actions, null);
        ((TextView) content.findViewById(R.id.txtPopupReportDate)).setText(date);
        ((TextView) content.findViewById(R.id.txtPopupReportRoute)).setText(route);

        int popupWidth = dpToPx(300);
        reportActionsPopup = new PopupWindow(
                content,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        reportActionsPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        reportActionsPopup.setOutsideTouchable(true);
        reportActionsPopup.setElevation(dpToPx(12));

        content.findViewById(R.id.actionSaveReportPdf).setOnClickListener(view -> {
            reportActionsPopup.dismiss();
            Toast.makeText(
                    this,
                    getString(R.string.main_home_pdf_pending, route),
                    Toast.LENGTH_SHORT
            ).show();
        });
        content.findViewById(R.id.actionEditReport).setOnClickListener(view -> {
            reportActionsPopup.dismiss();
            Toast.makeText(
                    this,
                    getString(R.string.main_home_edit_pending, route),
                    Toast.LENGTH_SHORT
            ).show();
        });
        content.findViewById(R.id.actionDeleteReport).setOnClickListener(view -> {
            reportActionsPopup.dismiss();
            confirmReportDeletion(cardView, route);
        });

        int horizontalOffset = anchor.getWidth() - popupWidth;
        reportActionsPopup.showAsDropDown(anchor, horizontalOffset, -dpToPx(8));
    }

    private void confirmReportDeletion(View cardView, String route) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.main_home_delete_title)
                .setMessage(getString(R.string.main_home_delete_message, route))
                .setNegativeButton(R.string.main_home_delete_cancel, null)
                .setPositiveButton(R.string.main_home_delete_confirm, (dialog, which) -> {
                    cardView.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            getString(R.string.main_home_delete_success, route),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (reportActionsPopup != null) {
            reportActionsPopup.dismiss();
        }
        super.onDestroy();
    }
}
