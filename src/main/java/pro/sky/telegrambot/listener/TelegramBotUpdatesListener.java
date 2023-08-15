package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;



@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;

    public TelegramBotUpdatesListener(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            // Process your updates here
            long chatId = update.message().chat().id();

            String messageText;
            if (update.message() != null && "/start".equals(update.message().text())) {
                messageText = "Добро пожаловать!";
            } else {
                messageText = "Сорян, пока я больше ничего не умею, я глупенький";
            }

            SendMessage welcomeMessage = new SendMessage(chatId, messageText);
            try {
                telegramBot.execute(welcomeMessage);
            } catch (Exception e) {
                logger.error("Error sending message", e);
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
