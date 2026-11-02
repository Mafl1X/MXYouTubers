package me.mafl1x.mxyoutubers.service;

import com.google.gson.Gson;
import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.database.model.MediaApplication;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class DiscordWebhookService {

    private final MainConfig.Discord discord;
    private final ExecutorService executor;
    private final Gson gson = new Gson();

    public DiscordWebhookService(MainConfig.Discord discord, ExecutorService executor) {
        this.discord = discord;
        this.executor = executor;
    }

    public CompletableFuture<Void> sendApplication(MediaApplication application) {
        if (!discord.enabled || discord.webhookUrl == null || discord.webhookUrl.trim().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> payload = new LinkedHashMap<String, Object>();
                    payload.put("username", "MXYouTubers");
                    payload.put("embeds", buildEmbeds(application));
                    HttpURLConnection connection = (HttpURLConnection) new URL(discord.webhookUrl).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setDoOutput(true);
                    byte[] bytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
                    OutputStream outputStream = connection.getOutputStream();
                    try {
                        outputStream.write(bytes);
                    } finally {
                        outputStream.close();
                    }
                    connection.getInputStream().close();
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }, executor);
    }

    private List<Map<String, Object>> buildEmbeds(MediaApplication application) {
        Map<String, Object> embed = new LinkedHashMap<String, Object>();
        embed.put("title", "Новая медиа-заявка");
        embed.put("description", application.getPlayerName() + " | " + application.getChannelTitle());
        embed.put("color", Integer.valueOf(discord.embedColor));
        List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
        fields.add(field("Тип", application.getType().name(), true));
        fields.add(field("Подписчики", String.valueOf(application.getChannelSubscribers()), true));
        fields.add(field("Просмотры ролика", String.valueOf(application.getVideoViews()), true));
        fields.add(field("Видео", application.getVideoUrl(), false));
        embed.put("fields", fields);
        Map<String, Object> thumbnail = new LinkedHashMap<String, Object>();
        thumbnail.put("url", "https://minotar.net/avatar/" + application.getPlayerName() + "/128.png");
        embed.put("thumbnail", thumbnail);
        List<Map<String, Object>> embeds = new ArrayList<Map<String, Object>>();
        embeds.add(embed);
        return embeds;
    }

    private Map<String, Object> field(String name, String value, boolean inline) {
        Map<String, Object> field = new LinkedHashMap<String, Object>();
        field.put("name", name);
        field.put("value", value);
        field.put("inline", Boolean.valueOf(inline));
        return field;
    }
}
