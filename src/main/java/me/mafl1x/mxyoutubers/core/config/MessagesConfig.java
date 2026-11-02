package me.mafl1x.mxyoutubers.core.config;

import de.exlll.configlib.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public final class MessagesConfig {

    public String prefix = "&8[&dMedia&8] &f";
    public String noPermission = "&cУ тебя нет прав.";
    public String playerOnly = "&cКоманда доступна только игроку.";
    public String loading = "&7Загружаю данные, подожди немного...";
    public String enterVideoLink = "&fОтправь в чат ссылку на YouTube видео. Для отмены напиши &cотмена&f.";
    public String enterRestoreLink = "&fОтправь ссылку на новый ролик для восстановления статуса. Для отмены напиши &cотмена&f.";
    public String enterPromoCode = "&fНапиши новый реферальный код в чат. Для отмены напиши &cотмена&f.";
    public String enterRejectReason = "&fНапиши причину отказа в чат. Для отмены напиши &cотмена&f.";
    public String enterSuspendReason = "&fНапиши причину снятия статуса в чат. Для отмены напиши &cотмена&f.";
    public String inputCancelled = "&7Действие отменено.";
    public String invalidVideoLink = "&cНе удалось распознать ссылку на YouTube видео.";
    public String invalidPromoCode = "&cКод должен быть длиной от 3 до 16 символов и содержать только латиницу, цифры, '_' или '-'.";
    public String pendingApplicationExists = "&cУ тебя уже есть активная заявка.";
    public String applicationCooldown = "&cПовторная подача будет доступна через &d%time%&c.";
    public String restorationCooldown = "&cПовторная попытка восстановления будет доступна через &d%time%&c.";
    public String activeMediaExists = "&cКанал уже привязан и активен.";
    public String channelInactive = "&cТвой медиа-статус сейчас неактивен.";
    public String channelNotLinked = "&cУ тебя пока нет привязанного YouTube канала.";
    public String autoRejectReason = "&cЗаявка отклонена автоматически: &f%reason%";
    public String confirmationInstruction = "&fДобавь код &d%code%&f в описание ролика, затем нажми ниже.";
    public String confirmButton = "&a[Подтвердить проверку]";
    public String cancelButton = "&c[Отменить]";
    public String codeNotFound = "&cКод подтверждения не найден в описании ролика.";
    public String sentToReview = "&aЗаявка отправлена администрации на проверку.";
    public String applicationApproved = "&aЗаявка одобрена. Медиа-статус активирован.";
    public String applicationRejected = "&cЗаявка отклонена: &f%reason%";
    public String mediaSuspended = "&cТвой медиа-статус временно снят: &f%reason%";
    public String mediaRestoreHint = "&7Открой /media и нажми кнопку восстановления.";
    public String mediaRestored = "&aТвой медиа-статус восстановлен.";
    public String promoChanged = "&aРеферальный код обновлён: &f%code%";
    public String promoCooldown = "&cИзменить код можно через &d%time%&c.";
    public String promoApplied = "&aКод принят. Сыграй ещё &d%minutes% минут&a, чтобы стать активным рефералом.";
    public String promoAlreadyUsed = "&cТы уже привязан к другому медиа.";
    public String promoOwnCode = "&cТы не можешь использовать собственный код.";
    public String promoUnavailable = "&cЭтот код сейчас недоступен.";
    public String referralActivated = "&aРеферал &d%player% &aстал активным.";
    public String manualRefreshCooldown = "&cОбновить статистику можно через &d%time%&c.";
    public String statsUpdated = "&aСтатистика канала обновлена.";
    public String noPendingReview = "&cЗаявка уже неактуальна.";
    public String adminOnly = "&cЭта функция только для администрации.";
    public String actionCompleted = "&aДействие выполнено.";
    public String adminNotification = "&dНовая медиа-заявка &8(&f%type%&8)&d от &f%player%&d.";
    public String reasonHistoryTitle = "&7Последние отказы:";
    public String inactiveTag = "&cНеактивен";
    public String activeTag = "&aАктивен";
    public List<String> instructionLines = new ArrayList<String>(Arrays.asList(
            "&d&lПодключение YouTube канала",
            "&7Минимум подписчиков: &f%min_subscribers%",
            "&7Минимум просмотров ролика: &f%min_views%",
            "&7В описании должен быть IP: &f%server_ip%",
            "&7Обязательный хэштег: &f%hashtag%",
            "&7В названии должно быть упоминание сервера."
    ));
    public String instructionCopyIp = "&d[Скопировать IP]";
    public String instructionCopyHashtag = "&d[Скопировать хэштег]";
    public String instructionRunMedia = "&d[Открыть /media]";
}
