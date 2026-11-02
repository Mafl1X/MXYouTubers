package me.mafl1x.mxyoutubers.gui;

import me.mafl1x.mxyoutubers.core.config.GuiConfig;
import me.mafl1x.mxyoutubers.core.config.MainConfig;
import me.mafl1x.mxyoutubers.core.config.MessagesConfig;
import me.mafl1x.mxyoutubers.core.util.ColorUtil;
import me.mafl1x.mxyoutubers.core.util.ItemFactory;
import me.mafl1x.mxyoutubers.core.util.PlaceholderUtil;
import me.mafl1x.mxyoutubers.database.model.MediaApplication;
import me.mafl1x.mxyoutubers.database.model.MediaChannel;
import me.mafl1x.mxyoutubers.database.model.MediaStatus;
import me.mafl1x.mxyoutubers.database.model.RefusalEntry;
import me.mafl1x.mxyoutubers.database.repository.ApplicationRepository;
import me.mafl1x.mxyoutubers.service.ApplicationService;
import me.mafl1x.mxyoutubers.service.InputSession;
import me.mafl1x.mxyoutubers.service.InputSessionService;
import me.mafl1x.mxyoutubers.service.InputType;
import me.mafl1x.mxyoutubers.service.LevelService;
import me.mafl1x.mxyoutubers.service.MediaService;
import me.mafl1x.mxyoutubers.service.MessagingService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GuiManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final MessagesConfig messages;
    private final GuiConfig gui;
    private final ApplicationRepository applicationRepository;
    private final MediaService mediaService;
    private final ApplicationService applicationService;
    private final InputSessionService inputSessionService;
    private final MessagingService messagingService;
    private final LevelService levelService;

    public GuiManager(
            JavaPlugin plugin,
            MainConfig mainConfig,
            MessagesConfig messages,
            GuiConfig gui,
            ApplicationRepository applicationRepository,
            MediaService mediaService,
            ApplicationService applicationService,
            InputSessionService inputSessionService,
            MessagingService messagingService,
            LevelService levelService
    ) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        this.messages = messages;
        this.gui = gui;
        this.applicationRepository = applicationRepository;
        this.mediaService = mediaService;
        this.applicationService = applicationService;
        this.inputSessionService = inputSessionService;
        this.messagingService = messagingService;
        this.levelService = levelService;
    }

    public void openPlayerMenu(Player player) {
        CompletableFuture<Optional<MediaChannel>> mediaFuture = mediaService.findByPlayer(player.getUniqueId());
        CompletableFuture<Optional<MediaApplication>> pendingFuture = applicationRepository.findPendingByPlayer(player.getUniqueId());
        CompletableFuture<List<RefusalEntry>> refusalsFuture = applicationRepository.listRefusals(player.getUniqueId(), 5);
        mediaFuture.thenCombine(pendingFuture, PlayerPair::new).thenCombine(refusalsFuture, PlayerContext::new).thenAccept(context -> {
            runSync(new Runnable() {
                @Override
                public void run() {
                    MenuHolder holder = new MenuHolder(GuiType.PLAYER, 0, null, null);
                    Inventory inventory = Bukkit.createInventory(holder, gui.playerMenu.size, ColorUtil.colorize(gui.playerMenu.title));
                    holder.setInventory(inventory);
                    fillInventory(inventory, gui.playerMenu.filler);
                    inventory.setItem(gui.playerMenu.instructionSlot, ItemFactory.create(gui.playerMenu.instruction, Collections.<String, String>emptyMap()));
                    for (int index = 0; index < gui.playerMenu.refusalSlots.size(); index++) {
                        if (index >= context.refusals.size()) {
                            break;
                        }
                        RefusalEntry refusal = context.refusals.get(index);
                        inventory.setItem(gui.playerMenu.refusalSlots.get(index), ItemFactory.create(gui.playerMenu.refusal, PlaceholderUtil.map(
                                "reason", refusal.getReason(),
                                "date", refusal.getCreatedAt() == null ? "-" : DATE_FORMATTER.format(refusal.getCreatedAt())
                        )));
                    }
                    if (context.media.isPresent()) {
                        MediaChannel media = context.media.get();
                        inventory.setItem(gui.playerMenu.channelInfoSlot, ItemFactory.create(gui.playerMenu.channel, mediaPlaceholders(media)));
                        inventory.setItem(gui.playerMenu.refreshSlot, ItemFactory.create(gui.playerMenu.refresh, Collections.<String, String>emptyMap()));
                        if (media.getStatus() == MediaStatus.ACTIVE) {
                            inventory.setItem(gui.playerMenu.mainActionSlot, ItemFactory.create(gui.playerMenu.waiting, Collections.singletonMap("status", "active")));
                            inventory.setItem(gui.playerMenu.promoSlot, ItemFactory.create(gui.playerMenu.promo, mediaPlaceholders(media)));
                        } else {
                            inventory.setItem(gui.playerMenu.mainActionSlot, ItemFactory.create(gui.playerMenu.restore, Collections.<String, String>emptyMap()));
                            inventory.setItem(gui.playerMenu.promoSlot, ItemFactory.create(gui.playerMenu.blocked, Collections.<String, String>emptyMap()));
                        }
                    } else if (context.pending.isPresent()) {
                        inventory.setItem(gui.playerMenu.mainActionSlot, ItemFactory.create(gui.playerMenu.waiting, Collections.<String, String>emptyMap()));
                    } else {
                        inventory.setItem(gui.playerMenu.mainActionSlot, ItemFactory.create(gui.playerMenu.apply, Collections.<String, String>emptyMap()));
                    }
                    player.openInventory(inventory);
                }
            });
        });
    }

    public void openAdminRoot(Player player) {
        runSync(new Runnable() {
            @Override
            public void run() {
                MenuHolder holder = new MenuHolder(GuiType.ADMIN_ROOT, 0, null, null);
                Inventory inventory = Bukkit.createInventory(holder, gui.adminMenu.size, ColorUtil.colorize(gui.adminMenu.title));
                holder.setInventory(inventory);
                fillInventory(inventory, gui.adminMenu.filler);
                inventory.setItem(gui.adminMenu.applicationsSlot, ItemFactory.create(gui.adminMenu.applications, Collections.<String, String>emptyMap()));
                inventory.setItem(gui.adminMenu.activeMediaSlot, ItemFactory.create(gui.adminMenu.activeMedia, Collections.<String, String>emptyMap()));
                player.openInventory(inventory);
            }
        });
    }

    public void openApplicationsMenu(Player player, int page) {
        applicationRepository.findPendingReview().thenAccept(applications -> runSync(new Runnable() {
            @Override
            public void run() {
                MenuHolder holder = new MenuHolder(GuiType.APPLICATIONS, page, null, null);
                Inventory inventory = Bukkit.createInventory(holder, gui.applicationsMenu.size, ColorUtil.colorize(gui.applicationsMenu.title));
                holder.setInventory(inventory);
                fillInventory(inventory, gui.applicationsMenu.filler);
                inventory.setItem(gui.applicationsMenu.backSlot, ItemFactory.create(gui.applicationsMenu.back, Collections.<String, String>emptyMap()));
                if (page > 0) {
                    inventory.setItem(gui.applicationsMenu.previousSlot, ItemFactory.create(gui.applicationsMenu.previous, Collections.<String, String>emptyMap()));
                }
                int perPage = gui.applicationsMenu.contentSlots.size();
                int from = Math.max(0, page * perPage);
                int to = Math.min(applications.size(), from + perPage);
                if (to < applications.size()) {
                    inventory.setItem(gui.applicationsMenu.nextSlot, ItemFactory.create(gui.applicationsMenu.next, Collections.<String, String>emptyMap()));
                }
                for (int index = from; index < to; index++) {
                    MediaApplication application = applications.get(index);
                    int slot = gui.applicationsMenu.contentSlots.get(index - from);
                    holder.getApplicationSlots().put(Integer.valueOf(slot), Long.valueOf(application.getId()));
                    inventory.setItem(slot, ItemFactory.create(gui.applicationsMenu.entry, PlaceholderUtil.map(
                            "player", application.getPlayerName(),
                            "channel", application.getChannelTitle(),
                            "type", application.getType().name(),
                            "subscribers", String.valueOf(application.getChannelSubscribers()),
                            "views", String.valueOf(application.getVideoViews())
                    )));
                }
                player.openInventory(inventory);
            }
        }));
    }

    public void openReviewMenu(Player player, long applicationId, int page) {
        applicationRepository.findById(applicationId).thenAccept(optionalApplication -> {
            if (!optionalApplication.isPresent()) {
                messagingService.send(player, messages.noPendingReview);
                return;
            }
            MediaApplication application = optionalApplication.get();
            runSync(new Runnable() {
                @Override
                public void run() {
                    MenuHolder holder = new MenuHolder(GuiType.REVIEW, page, Long.valueOf(applicationId), null);
                    Inventory inventory = Bukkit.createInventory(holder, gui.reviewMenu.size, ColorUtil.colorize(gui.reviewMenu.title));
                    holder.setInventory(inventory);
                    fillInventory(inventory, gui.reviewMenu.filler);
                    inventory.setItem(gui.reviewMenu.infoSlot, ItemFactory.create(gui.reviewMenu.info, PlaceholderUtil.map(
                            "player", application.getPlayerName(),
                            "channel", application.getChannelTitle(),
                            "type", application.getType().name(),
                            "subscribers", String.valueOf(application.getChannelSubscribers()),
                            "views", String.valueOf(application.getVideoViews()),
                            "video", application.getVideoTitle()
                    )));
                    inventory.setItem(gui.reviewMenu.acceptSlot, ItemFactory.create(gui.reviewMenu.accept, Collections.<String, String>emptyMap()));
                    inventory.setItem(gui.reviewMenu.rejectSlot, ItemFactory.create(gui.reviewMenu.reject, Collections.<String, String>emptyMap()));
                    inventory.setItem(gui.reviewMenu.backSlot, ItemFactory.create(gui.reviewMenu.back, Collections.<String, String>emptyMap()));
                    player.openInventory(inventory);
                }
            });
        });
    }

    public void openActiveMediaMenu(Player player, int page) {
        mediaService.findAll().thenAccept(mediaList -> runSync(new Runnable() {
            @Override
            public void run() {
                MenuHolder holder = new MenuHolder(GuiType.ACTIVE_MEDIA, page, null, null);
                Inventory inventory = Bukkit.createInventory(holder, gui.activeMediaMenu.size, ColorUtil.colorize(gui.activeMediaMenu.title));
                holder.setInventory(inventory);
                fillInventory(inventory, gui.activeMediaMenu.filler);
                inventory.setItem(gui.activeMediaMenu.backSlot, ItemFactory.create(gui.activeMediaMenu.back, Collections.<String, String>emptyMap()));
                if (page > 0) {
                    inventory.setItem(gui.activeMediaMenu.previousSlot, ItemFactory.create(gui.activeMediaMenu.previous, Collections.<String, String>emptyMap()));
                }
                int perPage = gui.activeMediaMenu.contentSlots.size();
                int from = Math.max(0, page * perPage);
                int to = Math.min(mediaList.size(), from + perPage);
                if (to < mediaList.size()) {
                    inventory.setItem(gui.activeMediaMenu.nextSlot, ItemFactory.create(gui.activeMediaMenu.next, Collections.<String, String>emptyMap()));
                }
                for (int index = from; index < to; index++) {
                    MediaChannel media = mediaList.get(index);
                    int slot = gui.activeMediaMenu.contentSlots.get(index - from);
                    holder.getTargetSlots().put(Integer.valueOf(slot), media.getPlayerUuid());
                    inventory.setItem(slot, ItemFactory.create(gui.activeMediaMenu.entry, mediaPlaceholders(media)));
                }
                player.openInventory(inventory);
            }
        }));
    }

    public void openManageMenu(Player player, UUID targetUuid, int page) {
        mediaService.findByPlayer(targetUuid).thenAccept(optionalMedia -> {
            if (!optionalMedia.isPresent()) {
                messagingService.send(player, messages.channelNotLinked);
                return;
            }
            MediaChannel media = optionalMedia.get();
            runSync(new Runnable() {
                @Override
                public void run() {
                    MenuHolder holder = new MenuHolder(GuiType.MANAGE_MEDIA, page, null, targetUuid);
                    Inventory inventory = Bukkit.createInventory(holder, gui.manageMediaMenu.size, ColorUtil.colorize(gui.manageMediaMenu.title));
                    holder.setInventory(inventory);
                    fillInventory(inventory, gui.manageMediaMenu.filler);
                    inventory.setItem(gui.manageMediaMenu.infoSlot, ItemFactory.create(gui.manageMediaMenu.info, mediaPlaceholders(media)));
                    inventory.setItem(gui.manageMediaMenu.suspendSlot, ItemFactory.create(gui.manageMediaMenu.suspend, Collections.<String, String>emptyMap()));
                    inventory.setItem(gui.manageMediaMenu.restoreSlot, ItemFactory.create(gui.manageMediaMenu.restore, Collections.<String, String>emptyMap()));
                    inventory.setItem(gui.manageMediaMenu.unlinkSlot, ItemFactory.create(gui.manageMediaMenu.unlink, Collections.<String, String>emptyMap()));
                    inventory.setItem(gui.manageMediaMenu.backSlot, ItemFactory.create(gui.manageMediaMenu.back, Collections.<String, String>emptyMap()));
                    player.openInventory(inventory);
                }
            });
        });
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        int slot = event.getRawSlot();
        switch (holder.getType()) {
            case PLAYER:
                handlePlayerClick(player, slot);
                break;
            case ADMIN_ROOT:
                handleAdminRootClick(player, slot);
                break;
            case APPLICATIONS:
                handleApplicationsClick(player, holder, slot);
                break;
            case REVIEW:
                handleReviewClick(player, holder, slot);
                break;
            case ACTIVE_MEDIA:
                handleActiveMediaClick(player, holder, slot);
                break;
            case MANAGE_MEDIA:
                handleManageMediaClick(player, holder, slot);
                break;
            default:
                break;
        }
    }

    private void handlePlayerClick(Player player, int slot) {
        if (slot == gui.playerMenu.instructionSlot) {
            messagingService.sendInstruction(player, mainConfig);
            return;
        }
        if (slot == gui.playerMenu.mainActionSlot) {
            mediaService.findByPlayer(player.getUniqueId()).thenCompose(optionalMedia -> applicationRepository.findPendingByPlayer(player.getUniqueId()).thenAccept(pending -> {
                if (pending.isPresent()) {
                    messagingService.send(player, messages.pendingApplicationExists);
                    return;
                }
                if (optionalMedia.isPresent() && optionalMedia.get().getStatus() == MediaStatus.SUSPENDED) {
                    prompt(player, new InputSession(InputType.RESTORE_VIDEO, null, null), messages.enterRestoreLink);
                    return;
                }
                if (!optionalMedia.isPresent()) {
                    prompt(player, new InputSession(InputType.SUBMIT_VIDEO, null, null), messages.enterVideoLink);
                }
            }));
            return;
        }
        if (slot == gui.playerMenu.promoSlot) {
            mediaService.findByPlayer(player.getUniqueId()).thenAccept(optionalMedia -> {
                if (!optionalMedia.isPresent() || optionalMedia.get().getStatus() != MediaStatus.ACTIVE) {
                    messagingService.send(player, messages.channelInactive);
                    return;
                }
                prompt(player, new InputSession(InputType.PROMO_CODE, null, null), messages.enterPromoCode);
            });
            return;
        }
        if (slot == gui.playerMenu.refreshSlot) {
            mediaService.manualRefresh(player);
        }
    }

    private void handleAdminRootClick(Player player, int slot) {
        if (slot == gui.adminMenu.applicationsSlot) {
            openApplicationsMenu(player, 0);
        } else if (slot == gui.adminMenu.activeMediaSlot) {
            openActiveMediaMenu(player, 0);
        }
    }

    private void handleApplicationsClick(Player player, MenuHolder holder, int slot) {
        if (slot == gui.applicationsMenu.backSlot) {
            openAdminRoot(player);
            return;
        }
        if (slot == gui.applicationsMenu.previousSlot && holder.getPage() > 0) {
            openApplicationsMenu(player, holder.getPage() - 1);
            return;
        }
        if (slot == gui.applicationsMenu.nextSlot) {
            openApplicationsMenu(player, holder.getPage() + 1);
            return;
        }
        Long applicationId = holder.getApplicationSlots().get(Integer.valueOf(slot));
        if (applicationId != null) {
            openReviewMenu(player, applicationId.longValue(), holder.getPage());
        }
    }

    private void handleReviewClick(Player player, MenuHolder holder, int slot) {
        if (slot == gui.reviewMenu.backSlot) {
            openApplicationsMenu(player, holder.getPage());
            return;
        }
        if (slot == gui.reviewMenu.infoSlot && holder.getApplicationId() != null) {
            applicationRepository.findById(holder.getApplicationId().longValue()).thenAccept(optionalApplication -> {
                if (optionalApplication.isPresent()) {
                    messagingService.sendRaw(player, "&dСсылка на видео: &f" + optionalApplication.get().getVideoUrl());
                }
            });
            return;
        }
        if (slot == gui.reviewMenu.acceptSlot && holder.getApplicationId() != null) {
            applicationService.approve(player, holder.getApplicationId().longValue()).thenRun(new Runnable() {
                @Override
                public void run() {
                    openApplicationsMenu(player, holder.getPage());
                }
            });
            return;
        }
        if (slot == gui.reviewMenu.rejectSlot && holder.getApplicationId() != null) {
            prompt(player, new InputSession(InputType.ADMIN_REJECTION_REASON, holder.getApplicationId(), null), messages.enterRejectReason);
        }
    }

    private void handleActiveMediaClick(Player player, MenuHolder holder, int slot) {
        if (slot == gui.activeMediaMenu.backSlot) {
            openAdminRoot(player);
            return;
        }
        if (slot == gui.activeMediaMenu.previousSlot && holder.getPage() > 0) {
            openActiveMediaMenu(player, holder.getPage() - 1);
            return;
        }
        if (slot == gui.activeMediaMenu.nextSlot) {
            openActiveMediaMenu(player, holder.getPage() + 1);
            return;
        }
        UUID targetUuid = holder.getTargetSlots().get(Integer.valueOf(slot));
        if (targetUuid != null) {
            openManageMenu(player, targetUuid, holder.getPage());
        }
    }

    private void handleManageMediaClick(Player player, MenuHolder holder, int slot) {
        if (slot == gui.manageMediaMenu.backSlot) {
            openActiveMediaMenu(player, holder.getPage());
            return;
        }
        if (holder.getTargetPlayerUuid() == null) {
            return;
        }
        if (slot == gui.manageMediaMenu.suspendSlot) {
            prompt(player, new InputSession(InputType.ADMIN_SUSPEND_REASON, null, holder.getTargetPlayerUuid()), messages.enterSuspendReason);
            return;
        }
        if (slot == gui.manageMediaMenu.restoreSlot) {
            mediaService.findByPlayer(holder.getTargetPlayerUuid()).thenCompose(optionalMedia -> {
                if (!optionalMedia.isPresent()) {
                    return CompletableFuture.completedFuture(null);
                }
                return mediaService.restoreMedia(optionalMedia.get(), player, "решение администрации");
            }).thenRun(new Runnable() {
                @Override
                public void run() {
                    openManageMenu(player, holder.getTargetPlayerUuid(), holder.getPage());
                }
            });
            return;
        }
        if (slot == gui.manageMediaMenu.unlinkSlot) {
            mediaService.findByPlayer(holder.getTargetPlayerUuid()).thenCompose(optionalMedia -> {
                if (!optionalMedia.isPresent()) {
                    return CompletableFuture.completedFuture(null);
                }
                return mediaService.unlinkMedia(optionalMedia.get(), player, "решение администрации");
            }).thenRun(new Runnable() {
                @Override
                public void run() {
                    openActiveMediaMenu(player, holder.getPage());
                }
            });
        }
    }

    private void prompt(Player player, InputSession session, String message) {
        inputSessionService.set(player.getUniqueId(), session);
        runSync(new Runnable() {
            @Override
            public void run() {
                player.closeInventory();
            }
        });
        messagingService.send(player, message);
    }

    private void fillInventory(Inventory inventory, GuiConfig.ItemDefinition filler) {
        ItemStack item = ItemFactory.create(filler, Collections.<String, String>emptyMap());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    private Map<String, String> mediaPlaceholders(MediaChannel media) {
        return PlaceholderUtil.map(
                "player", media.getPlayerName(),
                "channel", media.getChannelTitle(),
                "status", media.getStatus() == MediaStatus.ACTIVE ? messages.activeTag : messages.inactiveTag,
                "subscribers", String.valueOf(media.getSubscribers()),
                "views", String.valueOf(media.getTotalViews()),
                "promo", media.getPromoCode() == null || media.getPromoCode().isEmpty() ? "-" : media.getPromoCode(),
                "level", levelService.getDisplayName(media.getLevelId())
        );
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private static final class PlayerPair {
        private final Optional<MediaChannel> media;
        private final Optional<MediaApplication> pending;

        private PlayerPair(Optional<MediaChannel> media, Optional<MediaApplication> pending) {
            this.media = media;
            this.pending = pending;
        }
    }

    private static final class PlayerContext {
        private final Optional<MediaChannel> media;
        private final Optional<MediaApplication> pending;
        private final List<RefusalEntry> refusals;

        private PlayerContext(PlayerPair pair, List<RefusalEntry> refusals) {
            this.media = pair.media;
            this.pending = pair.pending;
            this.refusals = refusals;
        }
    }
}
