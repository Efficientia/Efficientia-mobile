package com.inter.efficientia_mobile.feedback;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteFeedbackActivity extends AppCompatActivity {

    private TextView selectedRating;
    private String selectedRatingValue = "OTIMA";
    private EditText commentInput;
    private MaterialButton submitButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_route_feedback);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        commentInput = findViewById(R.id.inputFeedbackComment);
        submitButton = findViewById(R.id.btnSubmitFeedback);

        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.feedbackRoot), true);

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

        submitButton.setOnClickListener(view -> submitFeedback());
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

        int ratingId = rating.getId();
        if (ratingId == R.id.ratingGood) {
            selectedRatingValue = "BOA";
        } else if (ratingId == R.id.ratingRegular) {
            selectedRatingValue = "REGULAR";
        } else if (ratingId == R.id.ratingBad) {
            selectedRatingValue = "RUIM";
        } else {
            selectedRatingValue = "OTIMA";
        }
    }

    private void submitFeedback() {
        setLoading(true);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            saveFeedback(currentUser.getUid());
            return;
        }

        firebaseAuth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    FirebaseUser authenticatedUser = result.getUser();
                    if (authenticatedUser == null) {
                        handleSubmissionError();
                        return;
                    }
                    saveFeedback(authenticatedUser.getUid());
                })
                .addOnFailureListener(error -> handleSubmissionError());
    }

    private void saveFeedback(String firebaseUid) {
        int userId = SessionManager.userId(this);
        String userName = SessionManager.userName(this);
        if (userName.isBlank()) {
            userName = "Motorista";
        }

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("firebaseUid", firebaseUid);
        feedback.put("motoristaId", userId > 0 ? String.valueOf(userId) : "nao-identificado");
        feedback.put("motoristaNome", userName);
        feedback.put("localizacaoRota", getString(R.string.feedback_route_location));
        feedback.put("avaliacao", selectedRatingValue);
        feedback.put("motivos", selectedReasons());
        feedback.put("comentario", commentInput.getText().toString().trim());
        feedback.put("origem", "android");
        feedback.put("criadoEm", FieldValue.serverTimestamp());

        firestore.collection("feedbacks_rota")
                .add(feedback)
                .addOnSuccessListener(document -> {
                    Toast.makeText(this, R.string.feedback_send_success, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(error -> handleSubmissionError());
    }

    private List<String> selectedReasons() {
        List<String> reasons = new ArrayList<>();
        appendReasonIfChecked(reasons, R.id.chipEntrance, "ENTRADA");
        appendReasonIfChecked(reasons, R.id.chipWait, "ESPERA_NA_FAZENDA");
        appendReasonIfChecked(reasons, R.id.chipHeat, "CALOR");
        appendReasonIfChecked(reasons, R.id.chipCorral, "CURRAL");
        appendReasonIfChecked(reasons, R.id.chipFilling, "PREENCHIMENTO");
        return reasons;
    }

    private void appendReasonIfChecked(List<String> reasons, int chipId, String value) {
        Chip chip = findViewById(chipId);
        if (chip.isChecked()) {
            reasons.add(value);
        }
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        submitButton.setText(loading ? R.string.feedback_sending : R.string.feedback_submit);
    }

    private void handleSubmissionError() {
        setLoading(false);
        Toast.makeText(this, R.string.feedback_send_error, Toast.LENGTH_LONG).show();
    }

    private void showPendingFeature(View ignored) {
        Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show();
    }
}
