package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the compact Continue Watching horizontal flexbar.
 */
public class ContinueWatchingAdapter extends RecyclerView.Adapter<ContinueWatchingAdapter.ViewHolder> {

    private final Context context;
    private final List<ContinueWatchingItem> items = new ArrayList<>();
    private final float density;

    private static final android.util.LruCache<String, android.graphics.Bitmap> BITMAP_CACHE =
            new android.util.LruCache<>(30);

    private int focusColorStart = 0xFF9AD9E8;
    private int focusColorEnd = 0xFFE8A4C9;

    public interface OnItemClickListener {
        void onItemClick(ContinueWatchingItem item);
    }

    public interface NavigationCallback {
        void onNavigate();
    }

    private OnItemClickListener listener;
    private NavigationCallback onNavigateUp;
    private NavigationCallback onNavigateDown;

    public ContinueWatchingAdapter(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setNavigationCallbacks(NavigationCallback up, NavigationCallback down) {
        this.onNavigateUp = up;
        this.onNavigateDown = down;
    }

    public void setPalette(WallpaperColorExtractor.Palette palette) {
        if (palette != null) {
            this.focusColorStart = palette.hourHandColor;
            this.focusColorEnd = palette.minuteHandColor;
            notifyDataSetChanged();
        }
    }

    public void setItems(List<ContinueWatchingItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_continue_watching, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContinueWatchingItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ContinueWatchingCardView cardRoot;
        final ImageView poster;
        final TextView badge;
        final TextView title;
        final TextView subtitle;
        final FrameLayout progressTrack;
        final View progressFill;

        ViewHolder(View itemView) {
            super(itemView);
            cardRoot = (ContinueWatchingCardView) itemView;
            poster = itemView.findViewById(R.id.cwPoster);
            badge = itemView.findViewById(R.id.cwBadge);
            title = itemView.findViewById(R.id.cwTitle);
            subtitle = itemView.findViewById(R.id.cwSubtitle);
            progressTrack = itemView.findViewById(R.id.cwProgressTrack);
            progressFill = itemView.findViewById(R.id.cwProgressFill);

            cardRoot.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos >= 0 && pos < items.size()) {
                    ContinueWatchingItem it = items.get(pos);
                    if (listener != null) {
                        listener.onItemClick(it);
                    } else {
                        launchItem(it);
                    }
                }
            });

            cardRoot.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.bringToFront();
                    if (v.getParent() instanceof View) {
                        ((View) v.getParent()).invalidate();
                    }
                }
            });

            cardRoot.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                    if (onNavigateUp != null) {
                        onNavigateUp.onNavigate();
                        return true;
                    }
                } else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (onNavigateDown != null) {
                        onNavigateDown.onNavigate();
                        return true;
                    }
                }
                return false;
            });
        }

        void bind(ContinueWatchingItem item) {
            title.setText(item.title);
            subtitle.setText(item.subtitle);
            badge.setText(item.appName);

            // Dynamically match focus border and progress fill with current wallpaper
            cardRoot.setFocusGradient(focusColorStart, focusColorEnd);
            progressFill.setBackgroundColor(focusColorStart);

            // Ultra-translucent glass background so wallpaper shines through clearly
            int startColor, endColor;
            if ("in.startv.hotstar".equals(item.packageName)) {
                startColor = 0x1A142848;
                endColor = 0x0E0A1424;
            } else if ("com.netflix.ninja".equals(item.packageName)) {
                startColor = 0x1A3A141A;
                endColor = 0x0E1A080C;
            } else if ("com.amazon.amazonvideo.livingroom".equals(item.packageName)) {
                startColor = 0x1A0E3048;
                endColor = 0x0E061824;
            } else if ("com.apple.atve.androidtv.appletv".equals(item.packageName)) {
                startColor = 0x1A303038;
                endColor = 0x0E18181C;
            } else {
                startColor = 0x1A202838;
                endColor = 0x0E10141E;
            }

            GradientDrawable grad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{startColor, endColor}
            );
            grad.setCornerRadius(8f * density);
            // Match unfocused card border with current wallpaper palette
            int strokeColor = (focusColorStart != 0) ? ((focusColorStart & 0x00FFFFFF) | 0x44000000) : 0x22FFFFFF;
            grad.setStroke((int) (0.8f * density), strokeColor);
            cardRoot.setBackground(grad);

            // Load artwork poster if available
            poster.setImageDrawable(null);
            if (item.posterUri != null && !item.posterUri.isEmpty()) {
                final String uriStr = item.posterUri;
                poster.setTag(uriStr);

                android.graphics.Bitmap cached = BITMAP_CACHE.get(uriStr);
                if (cached != null) {
                    poster.setImageBitmap(cached);
                    poster.setVisibility(View.VISIBLE);
                } else {
                    poster.setVisibility(View.INVISIBLE);
                    new Thread(() -> {
                        try {
                            Uri u = Uri.parse(uriStr);
                            java.io.InputStream is = null;
                            if ("content".equals(u.getScheme()) || "android.resource".equals(u.getScheme()) || "file".equals(u.getScheme())) {
                                is = context.getContentResolver().openInputStream(u);
                            } else if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                                java.net.URLConnection conn = new java.net.URL(uriStr).openConnection();
                                conn.setConnectTimeout(4000);
                                conn.setReadTimeout(4000);
                                is = conn.getInputStream();
                            }
                            if (is != null) {
                                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                                is.close();
                                if (bmp != null) {
                                    BITMAP_CACHE.put(uriStr, bmp);
                                    cardRoot.post(() -> {
                                        if (uriStr.equals(poster.getTag())) {
                                            poster.setImageBitmap(bmp);
                                            poster.setVisibility(View.VISIBLE);
                                        }
                                    });
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }).start();
                }
            } else {
                poster.setVisibility(View.GONE);
            }

            // Progress bar
            progressTrack.post(() -> {
                int trackWidth = progressTrack.getWidth();
                if (trackWidth > 0) {
                    ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
                    lp.width = Math.round(trackWidth * (item.progressPercent / 100f));
                    progressFill.setLayoutParams(lp);
                }
            });
        }

        private void launchItem(ContinueWatchingItem item) {
            if (item.launchIntent != null) {
                try {
                    context.startActivity(item.launchIntent);
                    return;
                } catch (Exception ignored) {
                }
            }

            if (item.packageName != null) {
                PackageManager pm = context.getPackageManager();
                Intent intent = pm.getLeanbackLaunchIntentForPackage(item.packageName);
                if (intent == null) {
                    intent = pm.getLaunchIntentForPackage(item.packageName);
                }
                if (intent != null) {
                    try {
                        context.startActivity(intent);
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }

            Toast.makeText(context, item.title, Toast.LENGTH_SHORT).show();
        }
    }
}
