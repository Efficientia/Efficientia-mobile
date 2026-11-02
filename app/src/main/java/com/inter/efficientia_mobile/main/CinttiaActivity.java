package com.inter.efficientia_mobile.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.FriendlyHttpError;
import com.inter.efficientia_mobile.TechnicalDetails;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.CinttiaClient;
import com.inter.efficientia_mobile.network.CinttiaService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Driver support chat. The server retains context via user_id and session_id. */
public class CinttiaActivity extends AppCompatActivity {
    private static final String PREFS = "cinttia_chat";
    private static final int MAX_MESSAGES = 40;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private View send;
    private TextView status;
    private SharedPreferences preferences;
    private String historyKey;
    private String sessionKey;
    private String sessionId;
    private String retryMessage;
    private JSONArray history = new JSONArray();
    private Call<CinttiaService.ChatEnvelope> activeCall;
    private View typingIndicator;
    private final List<ObjectAnimator> typingAnimations = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_cinttia);
        View root = findViewById(R.id.cinttiaRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets occupied = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(occupied.left, occupied.top, occupied.right, occupied.bottom);
            return insets;
        });

        messages = findViewById(R.id.cinttiaMessages);
        scroll = findViewById(R.id.cinttiaScroll);
        input = findViewById(R.id.cinttiaInput);
        send = findViewById(R.id.cinttiaSend);
        status = findViewById(R.id.cinttiaStatus);

        int userId = SessionManager.userId(this);
        if (userId <= 0 || SessionManager.authorizationHeader(this) == null) {
            finish();
            return;
        }
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        historyKey = "history_" + userId;
        sessionKey = "session_" + userId;
        sessionId = preferences.getString(sessionKey, null);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            preferences.edit().putString(sessionKey, sessionId).apply();
        }

        findViewById(R.id.cinttiaTopHome).setOnClickListener(v -> ToolbarNavigation.home(this));
        findViewById(R.id.cinttiaNotifications).setOnClickListener(v ->
                ToolbarNavigation.open(this, DriverNotificationsActivity.class));
        findViewById(R.id.cinttiaProfile).setOnClickListener(v ->
                ToolbarNavigation.open(this, DriverProfileActivity.class));
        findViewById(R.id.navCinttiaHome).setOnClickListener(v -> ToolbarNavigation.home(this));
        findViewById(R.id.navCinttiaProfile).setOnClickListener(v ->
                ToolbarNavigation.open(this, DriverProfileActivity.class));
        findViewById(R.id.navCinttiaDocuments).setOnClickListener(v ->
                ToolbarNavigation.open(this, DocumentsActivity.class));
        findViewById(R.id.navCinttiaSettings).setOnClickListener(v ->
                ToolbarNavigation.open(this, DriverSettingsActivity.class));
        send.setOnClickListener(v -> sendMessage());
        findViewById(R.id.cinttiaClear).setOnClickListener(v -> confirmClearConversation());

        try {
            history = new JSONArray(preferences.getString(historyKey, "[]"));
        } catch (JSONException ignored) {
            history = new JSONArray();
        }
        if (history.length() == 0) {
            addBubble(getString(R.string.cinttia_welcome), false);
        } else {
            for (int i = 0; i < history.length(); i++) {
                JSONObject entry = history.optJSONObject(i);
                if (entry != null) addBubble(entry.optString("text"), entry.optBoolean("mine"));
            }
        }
        if (!CinttiaClient.isConfigured()) status.setText(R.string.cinttia_unconfigured);
    }

    @Override protected void onResume() {
        super.onResume();
        if (findViewById(R.id.cinttiaProfile) != null)
            ProfilePhotoStore.show(this, findViewById(R.id.cinttiaProfile));
    }

    private void confirmClearConversation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cinttia_clear_title)
                .setMessage(R.string.cinttia_clear_confirmation)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.cinttia_clear_action, (dialog, which) -> clearConversation())
                .show();
    }

    private void clearConversation() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
        send.setEnabled(true);
        hideTypingIndicator();
        input.setText("");
        status.setText("");
        retryMessage = null;
        history = new JSONArray();
        sessionId = UUID.randomUUID().toString();
        preferences.edit().remove(historyKey).putString(sessionKey, sessionId).apply();
        messages.removeAllViews();
        addBubble(getString(R.string.cinttia_welcome), false);
    }

    private void sendMessage() {
        if (activeCall != null) return;
        String message = input.getText().toString().trim();
        if (message.isEmpty()) return;
        if (!CinttiaClient.isConfigured()) {
            status.setText(R.string.cinttia_unconfigured);
            return;
        }
        input.setText("");
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(), 0);
        if (!message.equals(retryMessage)) {
            addBubble(message, true);
            remember(message, true);
        }
        retryMessage = null;
        status.setText("");
        send.setEnabled(false);
        showTypingIndicator();
        activeCall = CinttiaClient.service().chat(new CinttiaService.ChatPayload(
                String.valueOf(SessionManager.userId(this)), sessionId, message));
        activeCall.enqueue(new Callback<CinttiaService.ChatEnvelope>() {
            @Override public void onResponse(Call<CinttiaService.ChatEnvelope> call,
                                             Response<CinttiaService.ChatEnvelope> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                activeCall = null;
                send.setEnabled(true);
                hideTypingIndicator();
                CinttiaService.ChatEnvelope envelope = response.body();
                if (!response.isSuccessful()) {
                    status.setText(FriendlyHttpError.message(CinttiaActivity.this, response.code()));
                    TechnicalDetails.showError(CinttiaActivity.this,
                            FriendlyHttpError.message(CinttiaActivity.this, response.code()),
                            "CinttIA: HTTP " + response.code());
                    prepareRetry(message);
                } else if (envelope == null || envelope.response == null
                        || envelope.response.resposta == null || envelope.response.resposta.trim().isEmpty()) {
                    status.setText(R.string.cinttia_invalid_response);
                    prepareRetry(message);
                } else {
                    String answer = envelope.response.resposta;
                    addBubble(answer, false);
                    remember(answer, false);
                    status.setText("");
                }
            }

            @Override public void onFailure(Call<CinttiaService.ChatEnvelope> call, Throwable error) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                activeCall = null;
                send.setEnabled(true);
                hideTypingIndicator();
                status.setText(R.string.cinttia_network_error);
                TechnicalDetails.showError(CinttiaActivity.this,
                        getString(R.string.cinttia_network_error), error.toString());
                prepareRetry(message);
            }
        });
    }

    private void prepareRetry(String message) {
        retryMessage = message;
        if (input.getText().toString().isEmpty()) input.setText(message);
    }

    private void remember(String text, boolean mine) {
        JSONObject entry = new JSONObject();
        try {
            entry.put("text", text);
            entry.put("mine", mine);
            history.put(entry);
            JSONArray recent = new JSONArray();
            for (int i = Math.max(0, history.length() - MAX_MESSAGES); i < history.length(); i++)
                recent.put(history.optJSONObject(i));
            history = recent;
            preferences.edit().putString(historyKey, history.toString()).apply();
        } catch (JSONException ignored) { }
    }

    private void addBubble(String text, boolean mine) {
        TextView bubble = new TextView(this);
        bubble.setText(mine ? text : CinttiaMarkdown.render(text));
        bubble.setTextSize(15);
        bubble.setTextColor(mine ? Color.WHITE : Color.rgb(5, 39, 95));
        int pad = dp(14);
        bubble.setPadding(pad, dp(11), pad, dp(11));
        GradientDrawable background = new GradientDrawable();
        background.setColor(mine ? Color.rgb(13, 58, 133) : Color.rgb(213, 238, 251));
        background.setCornerRadius(dp(18));
        bubble.setBackground(background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = mine ? Gravity.END : Gravity.START;
        params.setMargins(mine ? dp(54) : 0, dp(8), mine ? 0 : dp(54), 0);
        messages.addView(bubble, params);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void showTypingIndicator() {
        hideTypingIndicator();
        LinearLayout bubble = new LinearLayout(this);
        bubble.setGravity(Gravity.CENTER_VERTICAL);
        bubble.setOrientation(LinearLayout.HORIZONTAL);
        bubble.setPadding(dp(10), dp(7), dp(13), dp(7));
        bubble.setContentDescription(getString(R.string.cinttia_thinking));
        bubble.setFocusable(true);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(213, 238, 251));
        background.setCornerRadius(dp(18));
        bubble.setBackground(background);

        FrameLayout robotBadge = new FrameLayout(this);
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setColor(Color.rgb(21, 82, 161));
        badgeBackground.setShape(GradientDrawable.OVAL);
        robotBadge.setBackground(badgeBackground);
        ImageView robot = new ImageView(this);
        robot.setImageResource(R.drawable.ic_nav_assistant);
        FrameLayout.LayoutParams robotParams = new FrameLayout.LayoutParams(dp(19), dp(19), Gravity.CENTER);
        robotBadge.addView(robot, robotParams);
        bubble.addView(robotBadge, new LinearLayout.LayoutParams(dp(28), dp(28)));

        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams dotsParams = new LinearLayout.LayoutParams(-2, dp(28));
        dotsParams.setMarginStart(dp(7));
        bubble.addView(dots, dotsParams);
        for (int index = 0; index < 3; index++) {
            TextView dot = new TextView(this);
            dot.setText("•");
            dot.setTextColor(Color.rgb(5, 39, 95));
            dot.setTextSize(22);
            dot.setGravity(Gravity.CENTER);
            dots.addView(dot, new LinearLayout.LayoutParams(dp(11), dp(25)));
            ObjectAnimator animation = ObjectAnimator.ofFloat(dot, View.ALPHA, .25f, 1f, .25f);
            animation.setDuration(850L);
            animation.setStartDelay(index * 170L);
            animation.setRepeatCount(ValueAnimator.INFINITE);
            animation.start();
            typingAnimations.add(animation);
        }

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(-2, -2);
        bubbleParams.gravity = Gravity.START;
        bubbleParams.setMargins(0, dp(8), dp(54), 0);
        messages.addView(bubble, bubbleParams);
        typingIndicator = bubble;
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void hideTypingIndicator() {
        for (ObjectAnimator animation : typingAnimations) animation.cancel();
        typingAnimations.clear();
        if (typingIndicator != null) {
            messages.removeView(typingIndicator);
            typingIndicator = null;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    @Override protected void onDestroy() {
        if (activeCall != null) activeCall.cancel();
        hideTypingIndicator();
        super.onDestroy();
    }
}
