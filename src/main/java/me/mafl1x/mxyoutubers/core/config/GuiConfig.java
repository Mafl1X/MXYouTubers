package me.mafl1x.mxyoutubers.core.config;

import de.exlll.configlib.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public final class GuiConfig {

    public PlayerMenu playerMenu = new PlayerMenu();
    public AdminMenu adminMenu = new AdminMenu();
    public ApplicationsMenu applicationsMenu = new ApplicationsMenu();
    public ReviewMenu reviewMenu = new ReviewMenu();
    public ActiveMediaMenu activeMediaMenu = new ActiveMediaMenu();
    public ManageMediaMenu manageMediaMenu = new ManageMediaMenu();

    @Configuration
    public static final class ItemDefinition {
        public String material = "PAPER";
        public String base64 = "";
        public int amount = 1;
        public int customModelData = 0;
        public boolean enchanted = false;
        public boolean hideFlags = true;
        public String name = "&fПредмет";
        public List<String> lore = new ArrayList<String>();
    }

    @Configuration
    public static final class PlayerMenu {
        public String title = "&8Меню &dмедиа";
        public int size = 45;
        public int instructionSlot = 13;
        public int mainActionSlot = 31;
        public int channelInfoSlot = 4;
        public int promoSlot = 20;
        public int refreshSlot = 24;
        public List<Integer> refusalSlots = new ArrayList<Integer>(Arrays.asList(36, 37, 38, 39, 40));
        public ItemDefinition filler = createItem("BLACK_STAINED_GLASS_PANE", "&0");
        public ItemDefinition instruction = createItem("PAPER", "&d&lИнструкция", Arrays.asList(
                "&7Нажми, чтобы продублировать",
                "&7условия в чат."
        ));
        public ItemDefinition apply = createItem("RED_WOOL", "&d&lПодать заявку", Arrays.asList(
                "&7Отправить ссылку на ролик",
                "&7для первичной проверки."
        ));
        public ItemDefinition waiting = createItem("CLOCK", "&d&lЗаявка в процессе", Arrays.asList(
                "&7У тебя уже есть активная",
                "&7заявка или подтверждение."
        ));
        public ItemDefinition channel = createItem("PLAYER_HEAD", "&d&l%channel%", Arrays.asList(
                "&7Статус: %status%",
                "&7Подписчики: &f%subscribers%",
                "&7Просмотры: &f%views%",
                "&7Уровень: &f%level%"
        ));
        public ItemDefinition promo = createItem("NAME_TAG", "&d&lРеферальный код", Arrays.asList(
                "&7Текущий код: &f%promo%",
                "&7Нажми, чтобы изменить."
        ));
        public ItemDefinition restore = createItem("LIME_DYE", "&d&lВосстановить статус", Arrays.asList(
                "&7Подай новую заявку без",
                "&7повторной привязки канала."
        ));
        public ItemDefinition blocked = createItem("GRAY_DYE", "&7&lНедоступно", Arrays.asList(
                "&7Функция временно заблокирована."
        ));
        public ItemDefinition refresh = createItem("SUNFLOWER", "&d&lОбновить статистику", Arrays.asList(
                "&7Ручное обновление данных",
                "&7канала через YouTube API."
        ));
        public ItemDefinition refusal = createItem("BARRIER", "&cОтказ", Arrays.asList(
                "&7%reason%",
                "&8%date%"
        ));
    }

    @Configuration
    public static final class AdminMenu {
        public String title = "&8Панель &dмедиа";
        public int size = 27;
        public int applicationsSlot = 11;
        public int activeMediaSlot = 15;
        public ItemDefinition filler = createItem("BLACK_STAINED_GLASS_PANE", "&0");
        public ItemDefinition applications = createItem("WRITABLE_BOOK", "&d&lЗаявки", Arrays.asList(
                "&7Открыть список активных",
                "&7заявок на проверку."
        ));
        public ItemDefinition activeMedia = createItem("NETHER_STAR", "&d&lАктивные медиа", Arrays.asList(
                "&7Управление привязанными",
                "&7YouTube каналами."
        ));
    }

    @Configuration
    public static final class ApplicationsMenu {
        public String title = "&8Заявки &dмедиа";
        public int size = 54;
        public List<Integer> contentSlots = new ArrayList<Integer>(Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        ));
        public int backSlot = 45;
        public int previousSlot = 48;
        public int nextSlot = 50;
        public ItemDefinition filler = createItem("BLACK_STAINED_GLASS_PANE", "&0");
        public ItemDefinition entry = createItem("PLAYER_HEAD", "&d%player% &8| &f%channel%", Arrays.asList(
                "&7Тип: &f%type%",
                "&7Подписчики: &f%subscribers%",
                "&7Просмотры ролика: &f%views%",
                "&7Нажми для модерации."
        ));
        public ItemDefinition previous = createItem("ARROW", "&dПредыдущая страница");
        public ItemDefinition next = createItem("ARROW", "&dСледующая страница");
        public ItemDefinition back = createItem("BARRIER", "&cНазад");
    }

    @Configuration
    public static final class ReviewMenu {
        public String title = "&8Проверка &dзаявки";
        public int size = 45;
        public int infoSlot = 13;
        public int acceptSlot = 29;
        public int rejectSlot = 33;
        public int backSlot = 40;
        public ItemDefinition filler = createItem("BLACK_STAINED_GLASS_PANE", "&0");
        public ItemDefinition info = createItem("PLAYER_HEAD", "&d%player% &8| &f%channel%", Arrays.asList(
                "&7Тип: &f%type%",
                "&7Подписчики: &f%subscribers%",
                "&7Просмотры ролика: &f%views%",
                "&7Видео: &f%video%",
                "&7Нажми, чтобы получить ссылку в чат."
        ));
        public ItemDefinition accept = createItem("LIME_WOOL", "&aОдобрить");
        public ItemDefinition reject = createItem("RED_WOOL", "&cОтклонить");
        public ItemDefinition back = createItem("BARRIER", "&cНазад");
    }

    @Configuration
    public static final class ActiveMediaMenu {
        public String title = "&8Активные &dмедиа";
        public int size = 54;
        public List<Integer> contentSlots = new ArrayList<Integer>(Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        ));
        public int backSlot = 45;
        public int previousSlot = 48;
        public int nextSlot = 50;
        public ItemDefinition filler = createItem("BLACK_STAINED_GLASS_PANE", "&0");
        public ItemDefinition entry = createItem("PLAYER_HEAD", "&d%player% &8| &f%channel%", Arrays.asList(
                "&7Статус: %status%",
                "&7Подписчики: &f%subscribers%",
                "&7Просмотры: &f%views%",
                "&7Уровень: &f%level%"
        ));
        public ItemDefinition previous = createItem("ARROW", "&dПредыдущая страница");
        public ItemDefinition next = createItem("ARROW", "&dСледующая страница");
        public ItemDefinition back = createItem("BARRIER", "&cНазад");
    }

    @Configuration
    public static final class ManageMediaMenu {
        public String title = "&8Управление &dмедиа";
        public int size = 45;
        public int infoSlot = 13;
        public int suspendSlot = 29;
        public int restoreSlot = 31;
        public int unlinkSlot = 33;
        public int backSlot = 40;
        public ItemDefinition filler = createItem("BLACK_STAINED_GLASS_PANE", "&0");
        public ItemDefinition info = createItem("PLAYER_HEAD", "&d%player% &8| &f%channel%", Arrays.asList(
                "&7Статус: %status%",
                "&7Подписчики: &f%subscribers%",
                "&7Просмотры: &f%views%",
                "&7Промо-код: &f%promo%"
        ));
        public ItemDefinition suspend = createItem("GRAY_DYE", "&cПриостановить");
        public ItemDefinition restore = createItem("LIME_DYE", "&aВосстановить");
        public ItemDefinition unlink = createItem("BARRIER", "&4Отвязать канал");
        public ItemDefinition back = createItem("BARRIER", "&cНазад");
    }

    private static ItemDefinition createItem(String material, String name) {
        ItemDefinition definition = new ItemDefinition();
        definition.material = material;
        definition.name = name;
        return definition;
    }

    private static ItemDefinition createItem(String material, String name, List<String> lore) {
        ItemDefinition definition = createItem(material, name);
        definition.lore = new ArrayList<String>(lore);
        return definition;
    }
}
