
package net.kb.ebankbot.telegram;

import jakarta.annotation.PostConstruct;
import net.kb.ebankbot.agents.EbankAIAgent;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final EbankAIAgent aiAgent;
    private final String botUsername;
    private final String telegramBotToken;

    public TelegramBot(
            @Value("${telegram.token}") String telegramBotToken,
            @Value("${telegram.username}") String botUsername,
            EbankAIAgent aiAgent) {

        super(telegramBotToken);

        this.telegramBotToken = telegramBotToken;
        this.botUsername = botUsername;
        this.aiAgent = aiAgent;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken(){
        return telegramBotToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage()) {
            return;
        }

        var message = update.getMessage();

        Long chatId = message.getChatId();

        try {

            String query = message.hasText()
                    ? message.getText()
                    : message.getCaption();

            List<Media> mediaList = new ArrayList<>();

            if (message.hasPhoto()) {

                List<PhotoSize> photos = message.getPhoto();

                PhotoSize photo = photos.get(photos.size() - 1);

                GetFile getFile = new GetFile();
                getFile.setFileId(photo.getFileId());

                org.telegram.telegrambots.meta.api.objects.File file =
                        execute(getFile);

                String filePath = file.getFilePath();

                String fileUrl =
                        "https://api.telegram.org/file/bot"
                                + telegramBotToken + "/" + filePath;

                URL url = new URL(fileUrl);

                mediaList.add(
                        Media.builder()
                                .id(photo.getFileId())
                                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                                .data(new UrlResource(url))
                                .build()
                );

                if (query == null || query.isBlank()) {
                    query = "Décris cette image.";
                }
            }

            if (query == null || query.isBlank()) {
                return;
            }

            String conversationId =
                    "telegram-" + chatId + "-user-" +
                            message.getFrom().getId();

            UserMessage userMessage = UserMessage.builder()
                    .text(query)
                    .media(mediaList)
                    .build();

            String answer = aiAgent.chat(
                    userMessage,
                    conversationId
            );

            SendMessage response = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(answer)
                    .build();

            execute(response);

        } catch (Exception e) {

            e.printStackTrace();

            try {
                SendMessage errorMessage = SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("Une erreur est survenue. Veuillez réessayer.")
                        .build();

                execute(errorMessage);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void  sendTextMessage(long chatId, String text) throws TelegramApiException{
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId),text);
        execute(sendMessage);
    }

    private void SendTypingQuestion(long chatId) throws TelegramApiException{
        SendChatAction sendChatAction = new SendChatAction();
        sendChatAction.setChatId(String.valueOf(chatId));
        sendChatAction.setAction(ActionType.TYPING);
        execute(sendChatAction);
    }
}
