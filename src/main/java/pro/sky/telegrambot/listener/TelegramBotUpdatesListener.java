package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;

    private static final Pattern REMINDER_PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");

    private final NotificationTaskRepository notificationTaskRepository;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskRepository notificationTaskRepository) {
        this.telegramBot = telegramBot;
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    private void sendMessage(long chatId, String messageText) {
        SendMessage message = new SendMessage(chatId, messageText);
        try {
            telegramBot.execute(message);
        } catch (Exception e) {
            logger.error("Error sending message", e);
        }
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);

            long chatId = update.message().chat().id();

            if (update.message() != null && "/start".equals(update.message().text())) {
                sendMessage(chatId, "Добро пожаловать!");
            } else if (update.message() != null && update.message().text() != null) {
                processReminderMessages(updates);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void processReminderMessages(List<Update> updates) {
        updates.forEach(update -> {
            Matcher matcher = REMINDER_PATTERN.matcher(update.message().text());
            long chatId = update.message().chat().id();

            if (!matcher.matches()) {
                sendMessage(chatId, "Проверьте формат: \"дд.мм.гггг чч:мм Напоминание\"");
                return;
            }

            String dateTimeString = matcher.group(1);
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            if (dateTime.isBefore(LocalDateTime.now())) {
                sendMessage(chatId, "Выбранное время уже прошло");
                return;
            }

            String reminderText = matcher.group(3);

            if (notificationTaskRepository.existsByNotificationDateAndNotificationText(dateTime, reminderText)) {
                sendMessage(chatId, "Такая задача на это время уже существует");
                return;
            }

            NotificationTask notificationTask = new NotificationTask(chatId, reminderText, dateTime);
            notificationTaskRepository.save(notificationTask);
            sendMessage(chatId, dateTimeString + " напомню " + reminderText);
        });
    }
}
