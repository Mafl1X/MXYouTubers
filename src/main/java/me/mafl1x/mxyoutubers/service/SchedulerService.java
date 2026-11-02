package me.mafl1x.mxyoutubers.service;

import me.mafl1x.mxyoutubers.core.config.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SchedulerService {

    private final JavaPlugin plugin;
    private final MainConfig config;
    private final MediaService mediaService;
    private final ReferralService referralService;

    public SchedulerService(JavaPlugin plugin, MainConfig config, MediaService mediaService, ReferralService referralService) {
        this.plugin = plugin;
        this.config = config;
        this.mediaService = mediaService;
        this.referralService = referralService;
    }

    public void start() {
        schedule(config.schedules.statsRefreshMinutes, new Runnable() {
            @Override
            public void run() {
                mediaService.refreshAllMedia();
            }
        });
        schedule(config.schedules.videoScanMinutes, new Runnable() {
            @Override
            public void run() {
                mediaService.scanForNewQualifyingVideos();
            }
        });
        schedule(config.schedules.referralCheckMinutes, new Runnable() {
            @Override
            public void run() {
                referralService.processPendingActivations();
            }
        });
        schedule(config.schedules.activityCheckMinutes, new Runnable() {
            @Override
            public void run() {
                mediaService.checkActivity();
            }
        });
    }

    private void schedule(int minutes, Runnable runnable) {
        long period = Math.max(1L, minutes) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, period, period);
    }
}
