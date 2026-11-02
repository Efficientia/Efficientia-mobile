package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

import java.util.Calendar;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;

public class RouteDiaryStepOneActivity extends AppCompatActivity {
    private RouteDiaryDraft draft;
    private static final int[] FIELDS = {R.id.inputRouteGta, R.id.inputRouteInvoice,
            R.id.inputRouteOrigin, R.id.inputRouteBoardingDate, R.id.inputRouteBoardingTime,
            R.id.inputRouteDepartureTime, R.id.inputRouteDepartureKm, R.id.inputRouteDestination};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_one);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        draft.restoreText(this, FIELDS);
        EditText origin = findViewById(R.id.inputRouteOrigin);
        EditText destination = findViewById(R.id.inputRouteDestination);
        clearSelectedIdOnEdit(origin, "fazenda_id");
        clearSelectedIdOnEdit(destination, "unidade_frigorifica_id");
        findViewById(R.id.btnSearchRouteOrigin).setOnClickListener(view -> RouteLocationPicker.show(this, true, (id, name) -> {
            origin.setText(name);
            draft.put(R.id.inputRouteOrigin, name);
            draft.put("fazenda_id", id > 0 ? String.valueOf(id) : "");
        }));
        findViewById(R.id.btnSearchRouteDestination).setOnClickListener(view -> RouteLocationPicker.show(this, false, (id, name) -> {
            destination.setText(name);
            draft.put(R.id.inputRouteDestination, name);
            draft.put("unidade_frigorifica_id", id > 0 ? String.valueOf(id) : "");
        }));

        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.routeStepOneRoot), true);

        findViewById(R.id.btnRouteStepOneBack).setOnClickListener(view -> finish());
        configureDatePicker(findViewById(R.id.inputRouteBoardingDate));
        configureTimePicker(findViewById(R.id.inputRouteBoardingTime));
        configureTimePicker(findViewById(R.id.inputRouteDepartureTime));
        findViewById(R.id.btnRouteStepOneContinue).setOnClickListener(view -> {
            draft.saveText(this, FIELDS);
            String validation = RouteDiaryPayload.validationError(draft);
            if (!validation.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.route_check_form_title).setMessage(validation)
                        .setPositiveButton(R.string.dialog_understood, null).show();
                return;
            }
            RouteDiarySync.saveAndContinue(this, draft, view,
                    () -> startActivity(draft.next(this, RouteDiaryStepThreeActivity.class)));
        });
    }

    private void clearSelectedIdOnEdit(EditText input, String idKey) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (!draft.get(idKey).isEmpty()) draft.put(idKey, "");
            }
            @Override public void afterTextChanged(Editable text) { }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (draft != null) draft.put("current_stage", "1");
    }

    @Override protected void onPause() {
        draft.saveText(this, FIELDS);
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
