package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.content.Intent;
import android.app.TimePickerDialog;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Locale;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.inter.efficientia_mobile.R;

public class RouteDiaryStepFourActivity extends AppCompatActivity {
    private RouteDiaryDraft draft;
    private static final int[] FIELDS = {R.id.inputRouteStopReason, R.id.inputRouteStopStart,
            R.id.inputRouteStopEnd, R.id.inputRouteAnomalyAnimals, R.id.inputRouteAnomalyDescription};
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_four);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        draft.restoreText(this, FIELDS);
        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.routeStepFourRoot), true);
        configureTimePicker(findViewById(R.id.inputRouteStopStart));
        configureTimePicker(findViewById(R.id.inputRouteStopEnd));
        findViewById(R.id.btnRouteStepFourBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnAddUnexpectedStop).setOnClickListener(view ->
                Toast.makeText(this, R.string.route_stop_added, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnRouteStepFourContinue).setOnClickListener(view ->
                startActivity(draft.next(this, RouteDiaryStepTwoActivity.class)));
    }

    @Override protected void onPause() {
        draft.saveText(this, FIELDS);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        draft.saveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    private void configureTimePicker(EditText input) {
        input.setOnClickListener(view -> {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);
            String current = input.getText().toString();
            if (current.matches("\\d{2}:\\d{2}")) {
                hour = Integer.parseInt(current.substring(0, 2));
                minute = Integer.parseInt(current.substring(3, 5));
            }
            new TimePickerDialog(this, (picker, selectedHour, selectedMinute) ->
                    input.setText(String.format(Locale.getDefault(), "%02d:%02d",
                            selectedHour, selectedMinute)), hour, minute, true).show();
        });
    }

}
