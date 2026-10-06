package com.inter.efficientia_mobile.signature;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignaturePadView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float lastX;
    private float lastY;
    private boolean hasSignature;
    private Runnable onSignatureStartedListener;

    public SignaturePadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
    }

    public void setOnSignatureStartedListener(Runnable listener) {
        onSignatureStartedListener = listener;
    }

    public boolean hasSignature() {
        return hasSignature;
    }

    public Bitmap renderSignature() {
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        bounds.inset(-dpToPx(12), -dpToPx(12));
        float width = Math.max(1f, bounds.width());
        float height = Math.max(1f, bounds.height());
        float scale = Math.min(1024f / width, 320f / height);
        Bitmap image = Bitmap.createBitmap(
                Math.max(1, (int) Math.ceil(width * scale)),
                Math.max(1, (int) Math.ceil(height * scale)),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.scale(scale, scale);
        canvas.translate(-bounds.left, -bounds.top);
        canvas.drawPath(path, paint);
        return image;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = clampX(event.getX());
        float y = clampY(event.getY());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                path.moveTo(x, y);
                lastX = x;
                lastY = y;
                if (!hasSignature && onSignatureStartedListener != null) {
                    onSignatureStartedListener.run();
                }
                hasSignature = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                for (int index = 0; index < event.getHistorySize(); index++) {
                    addPoint(clampX(event.getHistoricalX(index)), clampY(event.getHistoricalY(index)));
                }
                addPoint(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                addPoint(x, y);
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return false;
        }
    }

    private void addPoint(float x, float y) {
        float middleX = (lastX + x) / 2f;
        float middleY = (lastY + y) / 2f;
        path.quadTo(lastX, lastY, middleX, middleY);
        lastX = x;
        lastY = y;
    }

    private float clampX(float x) {
        float inset = paint.getStrokeWidth() / 2f;
        float minimum = getPaddingLeft() + inset;
        float maximum = Math.max(minimum, getWidth() - getPaddingRight() - inset);
        return Math.max(minimum, Math.min(x, maximum));
    }

    private float clampY(float y) {
        float inset = paint.getStrokeWidth() / 2f;
        float minimum = getPaddingTop() + inset;
        float maximum = Math.max(minimum, getHeight() - getPaddingBottom() - inset);
        return Math.max(minimum, Math.min(y, maximum));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
