package me.mafl1x.mxyoutubers.core.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public final class MainConfig {

    @Comment({
            "YouTube Data API v3 key.",
            "Create it in Google Cloud Console and enable YouTube Data API v3.",
            "Only public data is used, OAuth is not required."
    })
    public Youtube youtube = new Youtube();
    public Database database = new Database();
    public Requirements requirements = new Requirements();
    public Cooldowns cooldowns = new Cooldowns();
    public Schedules schedules = new Schedules();
    public Commands commands = new Commands();
    public Rewards rewards = new Rewards();
    public Discord discord = new Discord();
    public AntiAbuse antiAbuse = new AntiAbuse();
    public Async async = new Async();
    public List<LevelDefinition> levels = new ArrayList<LevelDefinition>(Arrays.asList(
            new LevelDefinition("starter", "&fСтарт", 100L, Arrays.asList("lp user %player% parent add media_starter")),
            new LevelDefinition("partner", "&dПартнёр", 1000L, Arrays.asList("lp user %player% parent add media_partner")),
            new LevelDefinition("legend", "&cЛегенда", 10000L, Arrays.asList("lp user %player% parent add media_legend"))
    ));

    @Configuration
    public static final class Youtube {
        public String apiKey = "PASTE_YOUR_YOUTUBE_API_KEY";
        public int timeoutMillis = 10000;
        public int cacheMinutes = 60;
        public int recentVideoLimit = 10;
    }

    @Configuration
    public static final class Database {
        public String type = "SQLITE";
        public String sqliteFile = "database.db";
        public String host = "127.0.0.1";
        public int port = 3306;
        public String database = "mxyoutubers";
        public String username = "root";
        public String password = "password";
        public boolean useSsl = false;
        public int maximumPoolSize = 10;
    }

    @Configuration
    public static final class Requirements {
        public long minSubscribers = 100L;
        public long minVideoViews = 250L;
        public String serverIp = "play.LoneGrief.ru";
        public String requiredHashtag = "#lonegrief";
        public List<String> titleKeywords = new ArrayList<String>(Arrays.asList(
                "lonegrief",
                "лон гриф",
                "лонгриф"
        ));
    }

    @Configuration
    public static final class Cooldowns {
        public int applicationHours = 24;
        public int restorationHours = 24;
        public int promoChangeHours = 24;
        public int manualRefreshMinutes = 30;
    }

    @Configuration
    public static final class Schedules {
        public int statsRefreshMinutes = 60;
        public int videoScanMinutes = 60;
        public int referralCheckMinutes = 5;
        public int activityCheckMinutes = 180;
        public int activityWindowDays = 7;
    }

    @Configuration
    public static final class Commands {
        public List<String> onApprove = new ArrayList<String>(Arrays.asList(
                "lp user %player% parent add media",
                "broadcast &d%player% &fстал медиа сервера!"
        ));
        public List<String> onSuspend = new ArrayList<String>(Arrays.asList(
                "lp user %player% parent remove media"
        ));
        public List<String> onRestore = new ArrayList<String>(Arrays.asList(
                "lp user %player% parent add media"
        ));
        public List<String> onUnlink = new ArrayList<String>(Arrays.asList(
                "lp user %player% parent remove media"
        ));
    }

    @Configuration
    public static final class Rewards {
        public Publication publication = new Publication();
        public Referral referral = new Referral();
        public List<ViewReward> views = new ArrayList<ViewReward>(Arrays.asList(
                new ViewReward("views_1000", true, 1000L, Arrays.asList("give %player% diamond 4")),
                new ViewReward("views_5000", true, 5000L, Arrays.asList("give %player% diamond_block 2"))
        ));

        @Configuration
        public static final class Publication {
            public boolean enabled = true;
            public List<String> mediaCommands = new ArrayList<String>(Arrays.asList(
                    "give %player% emerald 8"
            ));
        }

        @Configuration
        public static final class Referral {
            public boolean enabled = true;
            public int requiredPlayMinutes = 30;
            public List<String> mediaCommands = new ArrayList<String>(Arrays.asList(
                    "give %player% diamond 2"
            ));
            public List<String> referralCommands = new ArrayList<String>(Arrays.asList(
                    "give %referral% iron_ingot 16"
            ));
        }
    }

    @Configuration
    public static final class ViewReward {
        public String id = "reward";
        public boolean enabled = true;
        public long requiredViews = 1000L;
        public List<String> commands = new ArrayList<String>(Arrays.asList("give %player% diamond 1"));

        public ViewReward() {
        }

        public ViewReward(String id, boolean enabled, long requiredViews, List<String> commands) {
            this.id = id;
            this.enabled = enabled;
            this.requiredViews = requiredViews;
            this.commands = new ArrayList<String>(commands);
        }
    }

    @Configuration
    public static final class Discord {
        public boolean enabled = false;
        public String webhookUrl = "https://discord.com/api/webhooks/xxx/yyy";
        public int embedColor = 0xC2185B;
    }

    @Configuration
    public static final class AntiAbuse {
        public int suspensionStrikeLimit = 3;
        public int strikeWindowDays = 30;
        public boolean requireManualReviewAfterLimit = true;
    }

    @Configuration
    public static final class Async {
        public int workerThreads = 4;
    }

    @Configuration
    public static final class LevelDefinition {
        public String id = "starter";
        public String displayName = "&fСтарт";
        public long minSubscribers = 100L;
        public List<String> commands = new ArrayList<String>();

        public LevelDefinition() {
        }

        public LevelDefinition(String id, String displayName, long minSubscribers, List<String> commands) {
            this.id = id;
            this.displayName = displayName;
            this.minSubscribers = minSubscribers;
            this.commands = new ArrayList<String>(commands);
        }
    }
}
