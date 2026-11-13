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

    public interface OnItemClickListener {
        void onItemClick(ContinueWatchingItem item);
    }

    private OnItemClickListener listener;

    public ContinueWatchingAdapter(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
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
        }

        void bind(ContinueWatchingItem item) {
            title.setText(item.title);
            subtitle.setText(item.subtitle);
            badge.setText(item.appName);

            // Sleek cinematic gradient background based on streaming brand
            int startColor, endColor;
            if ("in.startv.hotstar".equals(item.packageName)) {
                startColor = 0xFF14243B;
                endColor = 0xFF08101E;
            } else if ("com.netflix.ninja".equals(item.packageName)) {
                startColor = 0xFF2D1217;
                endColor = 0xFF120709;
            } else if ("com.amazon.amazonvideo.livingroom".equals(item.packageName)) {
                startColor = 0xFF0E283D;
                endColor = 0xFF05131E;
            } else if ("com.apple.atve.androidtv.appletv".equals(item.packageName)) {
                startColor = 0xFF28282D;
                endColor = 0xFF141416;
            } else {
                startColor = 0xFF1E2430;
                endColor = 0xFF0F131A;
            }

            GradientDrawable grad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{startColor, endColor}
            );
            grad.setCornerRadius(10f * density);
            cardRoot.setBackground(grad);

            // Load artwork poster if available
            poster.setImageDrawable(null);
            if (item.posterUri != null && !item.posterUri.isEmpty()) {
                final String uriStr = item.posterUri;
                poster.setTag(uriStr);
                new Thread(() -> {
                    try {
                        Uri u = Uri.parse(uriStr);
                        java.io.InputStream is = null;
                        if ("content".equals(u.getScheme()) || "android.resource".equals(u.getScheme())) {
                            is = context.getContentResolver().openInputStream(u);
                        } else if ("http".equals(u.getScheme()) || "https".equals(u.getScheme())) {
                            java.net.URLConnection conn = new java.net.URL(uriStr).openConnection();
                            conn.setConnectTimeout(3000);
                            conn.setReadTimeout(3000);
                            is = conn.getInputStream();
                        }
                        if (is != null) {
                            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                            is.close();
                            if (bmp != null) {
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
