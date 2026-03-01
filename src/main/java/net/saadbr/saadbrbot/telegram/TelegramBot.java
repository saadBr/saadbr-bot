package net.saadbr.saadbrbot.telegram;

import jakarta.annotation.PostConstruct;
import net.saadbr.saadbrbot.agents.AIAgent;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.api.key}")
    private String botToken;

    private final AIAgent aiAgent;

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
        if (!update.hasMessage()) return;

        try {
            var msg = update.getMessage();
            Long chatId = msg.getChatId();

            String text = msg.getText();        // null for photos
            String caption = msg.getCaption();  // null if user sends photo without caption
            List<PhotoSize> photos = msg.getPhoto();

            List<Media> mediaList = new ArrayList<>();

            // If message contains photo(s), attach ONE best-quality photo
            if (photos != null && !photos.isEmpty()) {
                PhotoSize best = photos.stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize, Comparator.nullsLast(Integer::compareTo)))
                        .orElse(photos.get(photos.size() - 1));

                String fileId = best.getFileId();
                GetFile getFile = new GetFile(fileId);
                File file = execute(getFile);

                String filePath = file.getFilePath();
                String url = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

                mediaList.add(Media.builder()
                        .id(fileId)
                        .mimeType(MimeTypeUtils.IMAGE_JPEG)
                        .data(new UrlResource(new URL(url)))
                        .build());
            }

            // Decide what text to send to the LLM (MUST NOT be null/blank)
            String query = firstNonBlank(text, caption);

            // If user sent only an image without caption/text, use a default prompt
            if ((query == null || query.isBlank()) && !mediaList.isEmpty()) {
                query = "Describe the image.";
            }

            // If still empty, ignore update (or you can send a friendly message)
            if (query == null || query.isBlank()) {
                sendMessage(chatId, "Send me a message or a photo with an optional caption 🙂");
                return;
            }

            UserMessage userMessage = UserMessage.builder()
                    .text(query)
                    .media(mediaList)
                    .build();

            sendTypingMessage(chatId);

            String answer = aiAgent.sendMessage(new Prompt(userMessage));
            sendMessage(chatId, answer);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
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
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), message);
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