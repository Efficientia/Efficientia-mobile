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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_two);
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
                startActivity(new Intent(this, RouteDiaryArrivalConditionActivity.class)));
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
