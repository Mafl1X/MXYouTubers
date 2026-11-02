package me.mafl1x.mxyoutubers.api;

import java.time.Instant;

public final class YouTubeVideo {

    private final String id;
    private final String url;
    private final String channelId;
    private final String channelTitle;
    private final String title;
    private final String description;
    private final long viewCount;
    private final Instant publishedAt;

    public YouTubeVideo(
            String id,
            String url,
            String channelId,
            String channelTitle,
            String title,
            String description,
            long viewCount,
            Instant publishedAt
    ) {
        this.id = id;
        this.url = url;
        this.channelId = channelId;
        this.channelTitle = channelTitle;
        this.title = title;
        this.description = description;
        this.viewCount = viewCount;
        this.publishedAt = publishedAt;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getViewCount() {
        return viewCount;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
