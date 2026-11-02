package me.mafl1x.mxyoutubers.media;

import me.mafl1x.mxyoutubers.api.YouTubeChannel;
import me.mafl1x.mxyoutubers.api.YouTubeVideo;
import me.mafl1x.mxyoutubers.core.config.MainConfig;

import java.util.Locale;

public final class VideoRequirementsChecker {

    private static final String DASH_VARIANTS = "[\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015\\u2212]+";

    private final MainConfig.Requirements requirements;

    public VideoRequirementsChecker(MainConfig.Requirements requirements) {
        this.requirements = requirements;
    }

    public String validate(YouTubeChannel channel, YouTubeVideo video) {
        if (channel.getSubscribers() < requirements.minSubscribers) {
            return "недостаточно подписчиков: нужно минимум " + requirements.minSubscribers;
        }
        if (video.getViewCount() < requirements.minVideoViews) {
            return "недостаточно просмотров у ролика: нужно минимум " + requirements.minVideoViews;
        }
        String description = normalize(video.getDescription());
        if (!description.contains(normalize(requirements.serverIp))) {
            return "в описании отсутствует IP " + requirements.serverIp;
        }
        if (!description.contains(normalize(requirements.requiredHashtag))) {
            return "в описании отсутствует хэштег " + requirements.requiredHashtag;
        }
        if (!containsTitleKeyword(video.getTitle())) {
            return "в названии ролика нет упоминания сервера";
        }
        return null;
    }

    public boolean containsConfirmationCode(YouTubeVideo video, String code) {
        String description = normalizeCodeText(video.getDescription());
        String normalizedCode = normalizeCodeText(code);
        if (description.contains(normalizedCode)) {
            return true;
        }

        String digits = extractDigits(code);
        if (digits.length() != 4) {
            return false;
        }

        String compactDescription = normalize(video.getDescription()).replaceAll(DASH_VARIANTS, "-");
        compactDescription = compactDescription.replaceAll("\\s+", "");
        return compactDescription.contains("lonegrief-" + digits) || compactDescription.contains("lonegrief" + digits);
    }

    public boolean isQualifyingVideo(YouTubeChannel channel, YouTubeVideo video) {
        return validate(channel, video) == null;
    }

    private boolean containsTitleKeyword(String title) {
        String normalized = normalize(title);
        for (String keyword : requirements.titleKeywords) {
            if (normalized.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private String normalizeCodeText(String text) {
        String normalized = normalize(text);
        normalized = normalized.replaceAll(DASH_VARIANTS, "-");
        normalized = normalized.replace('\u00A0', ' ');
        normalized = normalized.replaceAll("\\s+", "");
        return normalized;
    }

    private String extractDigits(String text) {
        return text == null ? "" : text.replaceAll("\\D", "");
    }
}
