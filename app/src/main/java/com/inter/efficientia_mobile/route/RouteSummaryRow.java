package com.inter.efficientia_mobile.route;
import android.view.View;
import android.widget.TextView;
import com.inter.efficientia_mobile.R;
final class RouteSummaryRow {
    private RouteSummaryRow() {}
    static void bind(View root, int label, int value) {
        bind(root, label, root.getContext().getString(value));
    }
    static void bind(View root, int label, String value) {
        ((TextView) root.findViewById(R.id.summaryLabel)).setText(label);
        ((TextView) root.findViewById(R.id.summaryValue)).setText(value);
    }
}
