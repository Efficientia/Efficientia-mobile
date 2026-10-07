package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;

public class RouteDiaryStepTwoActivity extends AppCompatActivity {
    private RouteDiaryDraft draft;
    private static final int[] FIELDS = {R.id.inputRouteArrivalDate, R.id.inputRouteArrivalTime,
            R.id.inputRouteUnloadingTime, R.id.inputRouteArrivalKm, R.id.inputRouteCorral};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_two);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        draft.restoreText(this, FIELDS);
        String alarm = draft.get("reverse_alarm");
        if ("yes".equals(alarm)) findViewById(R.id.btnAlarmYes).performClick();
        else if ("no".equals(alarm)) findViewById(R.id.btnAlarmNo).performClick();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepTwoRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.btnRouteStepTwoBack).setOnClickListener(view -> finish());
        configureDatePicker(findViewById(R.id.inputRouteArrivalDate));
        configureTimePicker(findViewById(R.id.inputRouteArrivalTime));
        configureTimePicker(findViewById(R.id.inputRouteUnloadingTime));
        findViewById(R.id.btnRouteStepTwoContinue).setOnClickListener(view ->
                startActivity(draft.next(this, RouteDiaryArrivalConditionActivity.class)));
    }

    @Override protected void onPause() {
        draft.saveText(this, FIELDS);
        com.google.android.material.button.MaterialButtonToggleGroup group = findViewById(R.id.alarmToggleGroup);
        draft.put("reverse_alarm", group.getCheckedButtonId() == R.id.btnAlarmYes ? "yes"
                : group.getCheckedButtonId() == R.id.btnAlarmNo ? "no" : "");
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        draft.saveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    private void configureDatePicker(EditText input) {
        input.setOnClickListener(view -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) ->
                    input.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)),
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private void configureTimePicker(EditText input) {
        input.setOnClickListener(view -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this, (picker, hour, minute) ->
                    input.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute)),
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        });
    }
}
