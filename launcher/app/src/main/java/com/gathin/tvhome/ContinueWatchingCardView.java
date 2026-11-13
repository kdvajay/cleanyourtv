package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

/**
 * Hyprland-styled card view for Continue Watching items.
 * Renders a 45° glowing cyan-to-magenta gradient border frame upon D-pad focus
 * and animates a smooth scale lift.
 */
public class ContinueWatchingCardView extends FrameLayout {

    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cardRect = new RectF();
    private final RectF glowRect = new RectF();

    private float density = 1f;
    private LinearGradient gradientShader;
    private int lastWidth = 0;
    private int lastHeight = 0;

    private int focusColorStart = 0xFF00F0FF;
    private int focusColorEnd = 0xFFFF007F;
    private int glowColor = 0x4000F0FF;

    public ContinueWatchingCardView(Context context) {
        super(context);
        init(context);
    }

    public ContinueWatchingCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContinueWatchingCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setClickable(true);
        setWillNotDraw(false);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2.2f * density);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4.5f * density);
        glowPaint.setColor(glowColor);

        // Clip children to smooth rounded rect
        final float cornerRadius = 8f * density;
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);
    }

    public void setFocusGradient(int startColor, int endColor) {
        if (startColor != 0 && endColor != 0) {
            this.focusColorStart = startColor;
            this.focusColorEnd = endColor;
            this.glowColor = (startColor & 0x00FFFFFF) | 0x38000000;
            glowPaint.setColor(this.glowColor);
            updateShader();
            if (isFocused()) invalidate();
        }
    }

    private void updateShader() {
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            gradientShader = new LinearGradient(
                    0, 0, w, h,
                    new int[]{focusColorStart, focusColorEnd},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            );
            borderPaint.setShader(gradientShader);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && (w != lastWidth || h != lastHeight)) {
            lastWidth = w;
            lastHeight = h;
            updateShader();
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            animate().scaleX(1.06f).scaleY(1.06f).translationZ(8f * density).setDuration(180).start();
        } else {
            animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(180).start();
        }
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (isFocused()) {
            float inset = borderPaint.getStrokeWidth() / 2f;
            cardRect.set(inset, inset, getWidth() - inset, getHeight() - inset);

            // Subtle glow under border
            float glowInset = glowPaint.getStrokeWidth() / 2f;
            glowRect.set(glowInset, glowInset, getWidth() - glowInset, getHeight() - glowInset);
            canvas.drawRoundRect(glowRect, 8f * density, 8f * density, glowPaint);

            // 45° angled wallpaper-matched gradient border
            canvas.drawRoundRect(cardRect, 8f * density, 8f * density, borderPaint);
        }
    }
}
