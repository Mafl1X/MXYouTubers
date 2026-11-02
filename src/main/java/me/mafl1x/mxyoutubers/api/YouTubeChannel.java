package me.mafl1x.mxyoutubers.api;

public final class YouTubeChannel {

    private final String id;
    private final String title;
    private final String url;
    private final long subscribers;
    private final long totalViews;
    private final long videoCount;

    public YouTubeChannel(String id, String title, String url, long subscribers, long totalViews, long videoCount) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.subscribers = subscribers;
        this.totalViews = totalViews;
        this.videoCount = videoCount;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public long getSubscribers() {
        return subscribers;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public long getVideoCount() {
        return videoCount;
    }
}
