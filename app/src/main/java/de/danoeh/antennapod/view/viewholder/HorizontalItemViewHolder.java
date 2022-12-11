package de.danoeh.antennapod.view.viewholder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.CoverLoader;
import de.danoeh.antennapod.adapter.actionbutton.ItemActionButton;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.util.DateFormatter;
import de.danoeh.antennapod.core.util.PlaybackStatus;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.common.CircularProgressBar;
import de.danoeh.antennapod.ui.common.SquareImageView;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class HorizontalItemViewHolder extends RecyclerView.ViewHolder {
    public final CardView card;
    public final ImageView secondaryActionIcon;
    private final SquareImageView cover;
    private final TextView title;
    private final TextView date;
    private final ProgressBar progressBar;
    private final CircularProgressBar circularProgressBar;
    private final View progressBarReplacementSpacer;

    private final MainActivity activity;
    private FeedItem item;

    public HorizontalItemViewHolder(MainActivity activity, ViewGroup parent) {
        super(LayoutInflater.from(activity).inflate(R.layout.horizontal_itemlist_item, parent, false));
        this.activity = activity;

        card = itemView.findViewById(R.id.card);
        cover = itemView.findViewById(R.id.cover);
        title = itemView.findViewById(R.id.titleLabel);
        date = itemView.findViewById(R.id.dateLabel);
        secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
        circularProgressBar = itemView.findViewById(R.id.circularProgressBar);
        progressBar = itemView.findViewById(R.id.progressBar);
        progressBarReplacementSpacer = itemView.findViewById(R.id.progressBarReplacementSpacer);
        itemView.setTag(this);
    }

    public void bind(FeedItem item) {
        this.item = item;

        card.setAlpha(1.0f);
        new CoverLoader(activity)
                .withUri(ImageResourceUtils.getEpisodeListImageLocation(item))
                .withFallbackUri(item.getFeed().getImageUrl())
                .withCoverView(cover)
                .load();
        title.setText(item.getTitle());
        date.setText(DateFormatter.formatAbbrev(activity, item.getPubDate()));
        ItemActionButton actionButton = ItemActionButton.forItem(item);
        actionButton.configure(secondaryActionIcon, secondaryActionIcon, activity);
        secondaryActionIcon.setFocusable(false);

        FeedMedia media = item.getMedia();
        if (media == null) {
            circularProgressBar.setPercentage(0, item);
            setProgressBar(false, 0);
        } else {
            if (PlaybackStatus.isCurrentlyPlaying(media)) {
                card.setCardBackgroundColor(ThemeUtils.getColorFromAttr(activity, R.attr.card_background_playing));
            } else {
                card.setCardBackgroundColor(ThemeUtils.getColorFromAttr(activity, R.attr.card_background));
            }

            if (item.getMedia().getDuration() > 0 && item.getMedia().getPosition() > 0) {
                setProgressBar(true, 100.0f * item.getMedia().getPosition() / item.getMedia().getDuration());
            } else {
                setProgressBar(false, 0);
            }

            if (DownloadService.isDownloadingFile(media.getDownload_url())) {
                final DownloadRequest downloadRequest = DownloadService.findRequest(media.getDownload_url());
                float percent = 0.01f * downloadRequest.getProgressPercent();
                circularProgressBar.setPercentage(Math.max(percent, 0.01f), item);
            } else if (media.isDownloaded()) {
                circularProgressBar.setPercentage(1, item); // Do not animate 100% -> 0%
            } else {
                circularProgressBar.setPercentage(0, item); // Animate X% -> 0%
            }
        }
    }

    public void bindDummy() {
        card.setAlpha(0.1f);
        new CoverLoader(activity)
                .withResource(android.R.color.transparent)
                .withCoverView(cover)
                .load();
        title.setText("████ █████");
        date.setText("███");
        secondaryActionIcon.setImageDrawable(null);
        circularProgressBar.setPercentage(0, null);
        setProgressBar(true, 50);
    }

    public boolean isCurrentlyPlayingItem() {
        return item != null && item.getMedia() != null && PlaybackStatus.isCurrentlyPlaying(item.getMedia());
    }

    public void notifyPlaybackPositionUpdated(PlaybackPositionEvent event) {
        setProgressBar(true, 100.0f * event.getPosition() / event.getDuration());
    }

    private void setProgressBar(boolean visible, float progress) {
        progressBar.setVisibility(visible ? ViewGroup.VISIBLE : ViewGroup.GONE);
        progressBarReplacementSpacer.setVisibility(visible ? View.GONE : ViewGroup.VISIBLE);
        progressBar.setProgress(Math.max(5, (int) progress)); // otherwise invisible below the edge radius
    }
}