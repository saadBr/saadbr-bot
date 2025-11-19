package net.saadbr.saadbrbot.telegram;

import jakarta.annotation.PostConstruct;
import net.saadbr.saadbrbot.agents.AIAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * @author saade
 **/
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Value("${telegram.api.key}")
    private String botToken;
    private AIAgent aiAgent;

    public TelegramBot(AIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }
    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onUpdateReceived(Update update) {
        if(!update.hasMessage()) return;
        Long chatId = update.getMessage().getChatId();
        String message = update.getMessage().getText();
        try {
            sendTypingMessage(chatId);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        String answer = aiAgent.sendMessage(message);
        sendMessage(chatId, answer);
    }

    @Override
    public String getBotUsername() {
        return "SaadBr_Bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId),message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void sendTypingMessage(long chatId) throws TelegramApiException {
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(String.valueOf(chatId));
        sendChatAction.setAction(ActionType.TYPING);
        execute(sendChatAction);
    }
}
