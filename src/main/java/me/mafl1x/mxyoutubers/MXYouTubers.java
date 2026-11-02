package me.mafl1x.mxyoutubers;

import me.mafl1x.mxyoutubers.api.YouTubeApiClient;
import me.mafl1x.mxyoutubers.command.MediaAdminCommand;
import me.mafl1x.mxyoutubers.command.MediaCommand;
import me.mafl1x.mxyoutubers.command.PromoCommand;
import me.mafl1x.mxyoutubers.core.config.ConfigManager;
import me.mafl1x.mxyoutubers.database.DatabaseManager;
import me.mafl1x.mxyoutubers.database.repository.ApplicationRepository;
import me.mafl1x.mxyoutubers.database.repository.AuditLogRepository;
import me.mafl1x.mxyoutubers.database.repository.MediaRepository;
import me.mafl1x.mxyoutubers.database.repository.ReferralRepository;
import me.mafl1x.mxyoutubers.database.repository.RewardRepository;
import me.mafl1x.mxyoutubers.gui.GuiManager;
import me.mafl1x.mxyoutubers.listener.ChatInputListener;
import me.mafl1x.mxyoutubers.listener.GuiListener;
import me.mafl1x.mxyoutubers.media.VideoRequirementsChecker;
import me.mafl1x.mxyoutubers.service.ApplicationService;
import me.mafl1x.mxyoutubers.service.AuditLogService;
import me.mafl1x.mxyoutubers.service.CommandService;
import me.mafl1x.mxyoutubers.service.DiscordWebhookService;
import me.mafl1x.mxyoutubers.service.InputSessionService;
import me.mafl1x.mxyoutubers.service.LevelService;
import me.mafl1x.mxyoutubers.service.MediaService;
import me.mafl1x.mxyoutubers.service.MessagingService;
import me.mafl1x.mxyoutubers.service.ReferralService;
import me.mafl1x.mxyoutubers.service.RewardService;
import me.mafl1x.mxyoutubers.service.SchedulerService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MXYouTubers extends JavaPlugin {

    private ConfigManager configManager;
    private ExecutorService executorService;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        try {
            this.configManager = new ConfigManager(this);
            this.configManager.load();

            if (!"MYSQL".equalsIgnoreCase(configManager.getMainConfig().database.type)) {
                File sqliteFile = new File(configManager.getMainConfig().database.sqliteFile);
                if (!sqliteFile.isAbsolute()) {
                    configManager.getMainConfig().database.sqliteFile = new File(getDataFolder(), configManager.getMainConfig().database.sqliteFile).getAbsolutePath();
                }
            }

            this.executorService = Executors.newFixedThreadPool(Math.max(2, configManager.getMainConfig().async.workerThreads));
            this.databaseManager = new DatabaseManager(configManager.getMainConfig().database, executorService);
            this.databaseManager.connect();

            MediaRepository mediaRepository = new MediaRepository(databaseManager);
            ApplicationRepository applicationRepository = new ApplicationRepository(databaseManager);
            ReferralRepository referralRepository = new ReferralRepository(databaseManager);
            RewardRepository rewardRepository = new RewardRepository(databaseManager);
            AuditLogRepository auditLogRepository = new AuditLogRepository(databaseManager);

            MessagingService messagingService = new MessagingService(this, configManager.getMessagesConfig());
            CommandService commandService = new CommandService(this);
            AuditLogService auditLogService = new AuditLogService(this, auditLogRepository);
            InputSessionService inputSessionService = new InputSessionService();
            LevelService levelService = new LevelService(configManager.getMainConfig().levels, commandService);
            RewardService rewardService = new RewardService(configManager.getMainConfig().rewards, rewardRepository, commandService);
            YouTubeApiClient youTubeApiClient = new YouTubeApiClient(configManager.getMainConfig().youtube, executorService);
            VideoRequirementsChecker requirementsChecker = new VideoRequirementsChecker(configManager.getMainConfig().requirements);
            DiscordWebhookService discordWebhookService = new DiscordWebhookService(configManager.getMainConfig().discord, executorService);

            MediaService mediaService = new MediaService(
                    this,
                    configManager.getMainConfig(),
                    configManager.getMessagesConfig(),
                    mediaRepository,
                    referralRepository,
                    youTubeApiClient,
                    requirementsChecker,
                    levelService,
                    rewardService,
                    auditLogService,
                    commandService,
                    messagingService
            );
            ApplicationService applicationService = new ApplicationService(
                    this,
                    configManager.getMainConfig(),
                    configManager.getMessagesConfig(),
                    applicationRepository,
                    mediaRepository,
                    youTubeApiClient,
                    requirementsChecker,
                    mediaService,
                    messagingService,
                    discordWebhookService,
                    auditLogService
            );
            ReferralService referralService = new ReferralService(
                    configManager.getMainConfig(),
                    configManager.getMessagesConfig(),
                    mediaRepository,
                    referralRepository,
                    rewardService,
                    messagingService,
                    auditLogService
            );
            GuiManager guiManager = new GuiManager(
                    this,
                    configManager.getMainConfig(),
                    configManager.getMessagesConfig(),
                    configManager.getGuiConfig(),
                    applicationRepository,
                    mediaService,
                    applicationService,
                    inputSessionService,
                    messagingService,
                    levelService
            );
            SchedulerService schedulerService = new SchedulerService(this, configManager.getMainConfig(), mediaService, referralService);

            getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);
            getServer().getPluginManager().registerEvents(new ChatInputListener(
                    configManager.getMessagesConfig(),
                    messagingService,
                    inputSessionService,
                    applicationService,
                    mediaService
            ), this);

            getCommand("media").setExecutor(new MediaCommand(
                    configManager.getMessagesConfig(),
                    messagingService,
                    guiManager,
                    applicationService
            ));
            getCommand("mediaadmin").setExecutor(new MediaAdminCommand(
                    configManager.getMessagesConfig(),
                    messagingService,
                    guiManager
            ));
            getCommand("promo").setExecutor(new PromoCommand(
                    configManager.getMessagesConfig(),
                    messagingService,
                    referralService
            ));

            schedulerService.start();
            getLogger().info("MXYouTubers enabled successfully.");
        } catch (Exception exception) {
            getLogger().severe("Could not enable MXYouTubers: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
