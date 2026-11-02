package me.mafl1x.mxyoutubers.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.mafl1x.mxyoutubers.core.config.MainConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public final class YouTubeApiClient {

    private static final String VIDEO_URL = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&id=%s&key=%s";
    private static final String CHANNEL_URL = "https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&id=%s&key=%s";
    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=%s&order=date&type=video&maxResults=%s&publishedAfter=%s&key=%s";

    private final MainConfig.Youtube config;
    private final ExecutorService executor;
    private final Gson gson = new Gson();
    private final Map<String, CacheEntry<YouTubeVideo>> videoCache = new ConcurrentHashMap<String, CacheEntry<YouTubeVideo>>();
    private final Map<String, CacheEntry<YouTubeChannel>> channelCache = new ConcurrentHashMap<String, CacheEntry<YouTubeChannel>>();

    public YouTubeApiClient(MainConfig.Youtube config, ExecutorService executor) {
        this.config = config;
        this.executor = executor;
    }

    public CompletableFuture<YouTubeVideo> fetchVideo(String videoId) {
        return fetchVideo(videoId, false);
    }

    public CompletableFuture<YouTubeVideo> fetchFreshVideo(String videoId) {
        return fetchVideo(videoId, true);
    }

    private CompletableFuture<YouTubeVideo> fetchVideo(String videoId, boolean bypassCache) {
        CacheEntry<YouTubeVideo> cached = videoCache.get(videoId);
        if (!bypassCache && cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.value);
        }
        return CompletableFuture.supplyAsync(() -> {
            ensureConfigured();
            try {
                JsonObject root = requestJson(String.format(VIDEO_URL, encode(videoId), encode(config.apiKey)));
                JsonArray items = root.getAsJsonArray("items");
                if (items == null || items.size() == 0) {
                    throw new IllegalStateException("Video not found: " + videoId);
                }
                YouTubeVideo video = parseVideo(items.get(0).getAsJsonObject());
                videoCache.put(videoId, new CacheEntry<YouTubeVideo>(video, Instant.now().plusSeconds(config.cacheMinutes * 60L)));
                return video;
            } catch (Exception exception) {
                throw new IllegalStateException("Could not fetch YouTube video " + videoId, exception);
            }
        }, executor);
    }

    public CompletableFuture<YouTubeChannel> fetchChannel(String channelId) {
        CacheEntry<YouTubeChannel> cached = channelCache.get(channelId);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.value);
        }
        return CompletableFuture.supplyAsync(() -> {
            ensureConfigured();
            try {
                JsonObject root = requestJson(String.format(CHANNEL_URL, encode(channelId), encode(config.apiKey)));
                JsonArray items = root.getAsJsonArray("items");
                if (items == null || items.size() == 0) {
                    throw new IllegalStateException("Channel not found: " + channelId);
                }
                YouTubeChannel channel = parseChannel(items.get(0).getAsJsonObject());
                channelCache.put(channelId, new CacheEntry<YouTubeChannel>(channel, Instant.now().plusSeconds(config.cacheMinutes * 60L)));
                return channel;
            } catch (Exception exception) {
                throw new IllegalStateException("Could not fetch YouTube channel " + channelId, exception);
            }
        }, executor);
    }

    public CompletableFuture<List<YouTubeVideo>> fetchRecentVideos(String channelId, Instant publishedAfter) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConfigured();
            try {
                String url = String.format(
                        SEARCH_URL,
                        encode(channelId),
                        Integer.toString(Math.max(1, config.recentVideoLimit)),
                        encode(publishedAfter.toString()),
                        encode(config.apiKey)
                );
                JsonObject root = requestJson(url);
                JsonArray items = root.getAsJsonArray("items");
                if (items == null || items.size() == 0) {
                    return Collections.<YouTubeVideo>emptyList();
                }
                List<String> ids = new ArrayList<String>();
                for (JsonElement element : items) {
                    JsonObject idObject = element.getAsJsonObject().getAsJsonObject("id");
                    if (idObject != null && idObject.has("videoId")) {
                        ids.add(idObject.get("videoId").getAsString());
                    }
                }
                if (ids.isEmpty()) {
                    return Collections.<YouTubeVideo>emptyList();
                }
                return fetchVideosByIds(ids);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not fetch recent videos for channel " + channelId, exception);
            }
        }, executor);
    }

    private List<YouTubeVideo> fetchVideosByIds(List<String> ids) throws Exception {
        JsonObject root = requestJson(String.format(VIDEO_URL, encode(join(ids)), encode(config.apiKey)));
        JsonArray items = root.getAsJsonArray("items");
        List<YouTubeVideo> videos = new ArrayList<YouTubeVideo>();
        if (items == null) {
            return videos;
        }
        for (JsonElement element : items) {
            YouTubeVideo video = parseVideo(element.getAsJsonObject());
            videos.add(video);
            videoCache.put(video.getId(), new CacheEntry<YouTubeVideo>(video, Instant.now().plusSeconds(config.cacheMinutes * 60L)));
        }
        return videos;
    }

    private YouTubeVideo parseVideo(JsonObject object) {
        JsonObject snippet = object.getAsJsonObject("snippet");
        JsonObject statistics = object.getAsJsonObject("statistics");
        String id = object.get("id").getAsString();
        String channelId = getString(snippet, "channelId");
        return new YouTubeVideo(
                id,
                "https://www.youtube.com/watch?v=" + id,
                channelId,
                getString(snippet, "channelTitle"),
                getString(snippet, "title"),
                getString(snippet, "description"),
                getLong(statistics, "viewCount"),
                Instant.parse(getString(snippet, "publishedAt"))
        );
    }

    private YouTubeChannel parseChannel(JsonObject object) {
        JsonObject snippet = object.getAsJsonObject("snippet");
        JsonObject statistics = object.getAsJsonObject("statistics");
        String id = object.get("id").getAsString();
        return new YouTubeChannel(
                id,
                getString(snippet, "title"),
                "https://www.youtube.com/channel/" + id,
                getLong(statistics, "subscriberCount"),
                getLong(statistics, "viewCount"),
                getLong(statistics, "videoCount")
        );
    }

    private JsonObject requestJson(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(config.timeoutMillis);
        connection.setReadTimeout(config.timeoutMillis);
        connection.setRequestProperty("Accept", "application/json");
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("YouTube API responded with status " + responseCode);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        try {
            return gson.fromJson(reader, JsonObject.class);
        } finally {
            reader.close();
            connection.disconnect();
        }
    }

    private void ensureConfigured() {
        if (config.apiKey == null || config.apiKey.trim().isEmpty() || "PASTE_YOUR_YOUTUBE_API_KEY".equals(config.apiKey)) {
            throw new IllegalStateException("YouTube API key is not configured");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String join(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < ids.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(ids.get(index));
        }
        return builder.toString();
    }

    private String getString(JsonObject object, String key) {
        return object != null && object.has(key) ? object.get(key).getAsString() : "";
    }

    private long getLong(JsonObject object, String key) {
        return object != null && object.has(key) ? object.get(key).getAsLong() : 0L;
    }

    private static final class CacheEntry<T> {
        private final T value;
        private final Instant expiresAt;

        private CacheEntry(T value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
