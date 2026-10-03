package com.inter.efficientia_mobile.feedback;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;

public class RouteFeedbackActivity extends AppCompatActivity {

    private TextView selectedRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_route_feedback);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.feedbackRoot), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnFeedbackBack).setOnClickListener(view -> finish());
        findViewById(R.id.navFeedbackHome).setOnClickListener(view -> finish());

        configureRating(R.id.ratingGreat);
        configureRating(R.id.ratingGood);
        configureRating(R.id.ratingRegular);
        configureRating(R.id.ratingBad);
        selectRating(findViewById(R.id.ratingGreat));

        int[] pendingNavigation = {
                R.id.navFeedbackProfile,
                R.id.navFeedbackDocuments,
                R.id.navFeedbackAssistant,
                R.id.navFeedbackSettings
        };
        for (int viewId : pendingNavigation) {
            findViewById(viewId).setOnClickListener(this::showPendingFeature);
        }

        findViewById(R.id.btnSubmitFeedback).setOnClickListener(view ->
                Toast.makeText(this, R.string.feedback_front_ready, Toast.LENGTH_LONG).show()
        );
    }

    private void configureRating(int viewId) {
        TextView rating = findViewById(viewId);
        rating.setOnClickListener(view -> selectRating((TextView) view));
    }

    private void selectRating(TextView rating) {
        if (selectedRating != null) {
            selectedRating.setSelected(false);
        }
        selectedRating = rating;
        selectedRating.setSelected(true);
    }

    private void showPendingFeature(View ignored) {
        Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show();
    }
}
