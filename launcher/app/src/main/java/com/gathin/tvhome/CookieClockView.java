package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;

/**
 * Material You "Cookie Clock" view matching the end-4 Hyprland aesthetic.
 * Features a 12-lobed scalloped dial, fine radial tick marks, stacked semi-translucent
 * background digital time, rounded analog hands with center hole pivot, and satellite badges
 * (top-left day of month and bottom-right ticking seconds).
 * Automatically animates its color palette to match the current wallpaper.
 */
public class CookieClockView extends View {

    private final Paint dialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint digitalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hourHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minuteHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badge1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeSecPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path scallopPath = new Path();
    private final Rect textBounds = new Rect();
    private final RectF badgeRect = new RectF();
    private final RectF handRect = new RectF();

    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private ValueAnimator colorAnimator;

    // Palette Colors
    private int curDialColor = 0xFF133E48;
    private int curHourColor = 0xFF9AD9E8;
    private int curMinuteColor = 0xFFE8A4C9;
    private int curBadge1Color = 0xFF5B6175;
    private int curBadgeSecColor = 0xFF1E2530;
    private int curTicksColor = 0x66FFFFFF;
    private int curDigitalColor = 0x2EFFFFFF;

    private int targetDialColor = curDialColor;
    private int targetHourColor = curHourColor;
    private int targetMinuteColor = curMinuteColor;
    private int targetBadge1Color = curBadge1Color;
    private int targetBadgeSecColor = curBadgeSecColor;
    private int targetTicksColor = curTicksColor;
    private int targetDigitalColor = curDigitalColor;

    private int startDialColor, startHourColor, startMinuteColor, startBadge1Color, startBadgeSecColor;
    private int startTicksColor, startDigitalColor;

    private boolean is24Hour = false;
    private float density = 1f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, Math.max(20, 1000 - (now % 1000)));
        }
    };

    public CookieClockView(Context context) {
        super(context);
        init(context);
    }

    public CookieClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CookieClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        dialPaint.setStyle(Paint.Style.FILL);
        dialPaint.setColor(curDialColor);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
        tickPaint.setColor(curTicksColor);

        digitalPaint.setColor(curDigitalColor);
        digitalPaint.setTextAlign(Paint.Align.CENTER);
        try {
            digitalPaint.setTypeface(UiFont.get(context, R.string.font_medium));
        } catch (Exception ignored) {
            digitalPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        }

        hourHandPaint.setStyle(Paint.Style.FILL);
        hourHandPaint.setColor(curHourColor);

        minuteHandPaint.setStyle(Paint.Style.FILL);
        minuteHandPaint.setColor(curMinuteColor);

        centerPinPaint.setStyle(Paint.Style.FILL);
        centerPinPaint.setColor(curMinuteColor);

        badge1Paint.setStyle(Paint.Style.FILL);
        badge1Paint.setColor(curBadge1Color);

        badgeSecPaint.setStyle(Paint.Style.FILL);
        badgeSecPaint.setColor(curBadgeSecColor);

        badgeTextPaint.setColor(0xFFFFFFFF);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        badgeTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    }

    public void setIs24Hour(boolean is24) {
        this.is24Hour = is24;
        invalidate();
    }

    public void setPalette(WallpaperColorExtractor.Palette palette) {
        if (palette == null) return;

        targetDialColor = palette.dialColor;
        targetHourColor = palette.hourHandColor;
        targetMinuteColor = palette.minuteHandColor;
        targetBadge1Color = palette.badge1Color;
        targetBadgeSecColor = palette.badgeSecColor;
        targetTicksColor = palette.ticksColor;
        targetDigitalColor = palette.digitalTimeColor;

        startDialColor = curDialColor;
        startHourColor = curHourColor;
        startMinuteColor = curMinuteColor;
        startBadge1Color = curBadge1Color;
        startBadgeSecColor = curBadgeSecColor;
        startTicksColor = curTicksColor;
        startDigitalColor = curDigitalColor;

        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }

        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(400);
        colorAnimator.addUpdateListener(anim -> {
            float frac = anim.getAnimatedFraction();
            curDialColor = (int) argbEvaluator.evaluate(frac, startDialColor, targetDialColor);
            curHourColor = (int) argbEvaluator.evaluate(frac, startHourColor, targetHourColor);
            curMinuteColor = (int) argbEvaluator.evaluate(frac, startMinuteColor, targetMinuteColor);
            curBadge1Color = (int) argbEvaluator.evaluate(frac, startBadge1Color, targetBadge1Color);
            curBadgeSecColor = (int) argbEvaluator.evaluate(frac, startBadgeSecColor, targetBadgeSecColor);
            curTicksColor = (int) argbEvaluator.evaluate(frac, startTicksColor, targetTicksColor);
            curDigitalColor = (int) argbEvaluator.evaluate(frac, startDigitalColor, targetDigitalColor);

            dialPaint.setColor(curDialColor);
            hourHandPaint.setColor(curHourColor);
            minuteHandPaint.setColor(curMinuteColor);
            centerPinPaint.setColor(curMinuteColor);
            badge1Paint.setColor(curBadge1Color);
            badgeSecPaint.setColor(curBadgeSecColor);
            tickPaint.setColor(curTicksColor);
            digitalPaint.setColor(curDigitalColor);

            invalidate();
        });
        colorAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
        if (colorAnimator != null) {
            colorAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float size = Math.min(w, h);

        // Radii for the 12-lobed scalloped cookie dial
        float rPeak = size * 0.36f;
        float rValley = rPeak * 0.86f;
        float rMid = (rPeak + rValley) / 2f;
        float rAmp = (rPeak - rValley) / 2f;

        // 1. Build & Draw 12-Lobed Scallop Flower Path
        scallopPath.reset();
        int steps = 96;
        for (int i = 0; i <= steps; i++) {
            float theta = (float) (i * 2 * Math.PI / steps);
            float r = rMid + rAmp * (float) Math.cos(12 * theta);
            float px = cx + r * (float) Math.sin(theta);
            float py = cy - r * (float) Math.cos(theta);
            if (i == 0) scallopPath.moveTo(px, py);
            else scallopPath.lineTo(px, py);
        }
        scallopPath.close();
        canvas.drawPath(scallopPath, dialPaint);

        // 2. Draw Fine Radial Perimeter Ticks (60 marks)
        float rTickOuter = rValley - 3f * density;
        for (int i = 0; i < 60; i++) {
            float phi = (float) (i * 2 * Math.PI / 60);
            boolean isHour = (i % 5 == 0);
            float rTickInner = rTickOuter - (isHour ? (5.5f * density) : (2.5f * density));

            tickPaint.setStrokeWidth((isHour ? 1.8f : 1f) * density);
            tickPaint.setAlpha(isHour ? 180 : 85);

            float x1 = cx + rTickInner * (float) Math.sin(phi);
            float y1 = cy - rTickInner * (float) Math.cos(phi);
            float x2 = cx + rTickOuter * (float) Math.sin(phi);
            float y2 = cy - rTickOuter * (float) Math.cos(phi);
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // Current Calendar Time
        Calendar now = Calendar.getInstance();
        int hour = now.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) hour = 12;
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);

        // 3. Draw Faint Stacked Background Digital Numerals ("HH" over "MM")
        digitalPaint.setTextSize(rPeak * 0.48f);
        digitalPaint.setColor(curDigitalColor);
        String hourStr = String.format(Locale.getDefault(), "%02d", hour);
        String minStr = String.format(Locale.getDefault(), "%02d", minute);

        digitalPaint.getTextBounds(hourStr, 0, hourStr.length(), textBounds);
        canvas.drawText(hourStr, cx, cy - rPeak * 0.08f, digitalPaint);
        canvas.drawText(minStr, cx, cy + rPeak * 0.40f, digitalPaint);

        // 4. Draw Analog Clock Hands
        // Hour Hand: Thick rounded pill with circular ring base
        float hourAngle = (hour % 12 + minute / 60f + second / 3600f) * 30f;
        float hLength = rPeak * 0.50f;
        float hWidth = 10f * density;
        float ringRadius = 9f * density;

        canvas.save();
        canvas.rotate(hourAngle, cx, cy);
        // Pill body extending outward
        handRect.set(cx - hWidth / 2f, cy - hLength, cx + hWidth / 2f, cy);
        canvas.drawRoundRect(handRect, hWidth / 2f, hWidth / 2f, hourHandPaint);
        // Base ring
        canvas.drawCircle(cx, cy, ringRadius, hourHandPaint);
        // Hole cutout in base ring so dial face shows through
        canvas.drawCircle(cx, cy, ringRadius * 0.50f, dialPaint);
        canvas.restore();

        // Minute Hand: Slender elongated rounded pill hand
        float minuteAngle = (minute + second / 60f) * 6f;
        float mLength = rPeak * 0.74f;
        float mWidth = 6.5f * density;

        canvas.save();
        canvas.rotate(minuteAngle, cx, cy);
        handRect.set(cx - mWidth / 2f, cy - mLength, cx + mWidth / 2f, cy + 3f * density);
        canvas.drawRoundRect(handRect, mWidth / 2f, mWidth / 2f, minuteHandPaint);
        canvas.restore();

        // Center Pivot Pin
        canvas.drawCircle(cx, cy, 3.2f * density, centerPinPaint);

        // 5. Satellite Badges
        // Top-Left Badge: Day of Month (e.g. "13" or "1")
        // Positioned around 225° / -135° along the scallop border
        float badgeDist = rPeak + 5f * density;
        float b1X = cx - badgeDist * 0.7071f;
        float b1Y = cy - badgeDist * 0.7071f;
        float b1W = 24f * density;
        float b1H = 22f * density;
        badgeRect.set(b1X - b1W / 2f, b1Y - b1H / 2f, b1X + b1W / 2f, b1Y + b1H / 2f);
        canvas.drawRoundRect(badgeRect, 6f * density, 6f * density, badge1Paint);

        badgeTextPaint.setTextSize(11f * density);
        String dayStr = String.valueOf(dayOfMonth);
        badgeTextPaint.getTextBounds(dayStr, 0, dayStr.length(), textBounds);
        canvas.drawText(dayStr, b1X, b1Y + textBounds.height() / 2f, badgeTextPaint);

        // Bottom-Right Badge: Seconds Capsule (e.g. "06" or "42")
        // Positioned around 45° / +135° along the scallop border
        float secDist = rPeak + 6f * density;
        float secX = cx + secDist * 0.7071f;
        float secY = cy + secDist * 0.7071f;
        float secW = 32f * density;
        float secH = 20f * density;
        badgeRect.set(secX - secW / 2f, secY - secH / 2f, secX + secW / 2f, secY + secH / 2f);
        canvas.drawRoundRect(badgeRect, 10f * density, 10f * density, badgeSecPaint);

        String secStr = String.format(Locale.getDefault(), "%02d", second);
        badgeTextPaint.getTextBounds(secStr, 0, secStr.length(), textBounds);
        canvas.drawText(secStr, secX, secY + textBounds.height() / 2f, badgeTextPaint);
    }
}
