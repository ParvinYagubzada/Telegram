package az.code.tourapp.bots;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.InputMismatchException;
import az.code.tourapp.exceptions.*;
import az.code.tourapp.models.Command;
import az.code.tourapp.models.UserData;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.models.entities.Question;
import az.code.tourapp.models.entities.Request;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.QuestionRepository;
import az.code.tourapp.repositories.RedisRepository;
import az.code.tourapp.repositories.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class TourBot extends TelegramWebhookBot {

    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;
    private final RequestRepository requestRepo;
    private final RedisRepository cache;

    private final String token;
    private final String username;
    private final String baseUrl;
    private final String apiUrl;

    private final Map<Command, Consumer<Update>> commands = new HashMap<>();

    @SneakyThrows
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();
            if (msg.startsWith("/")) {
                msg = msg.substring(1);
                Consumer<Update> action;
                if ((action = commands.get(new Command(msg))) != null) {
                    action.accept(update);
                }
            } else {
                handleMessage(update.getMessage());
            }
        }
        return null;
    }

    @SneakyThrows
    public void interrogate(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (requestRepo.existsAllByChatIdAndStatusIsTrue(chatId) || cache.findByChatId(chatId) != null) {
            createErrorMessage(new AlreadyHaveSessionException(), chatId);
            return;
        }
        Question question = questionRepo.findById(1L).orElseThrow(MissingFirstQuestionException::new);
        UserData data = UserData.builder().userLang(null).currentQuestion(question).build();
        cache.saveByChatId(chatId, data);
        createReply(data, chatId, question);
    }

    @SneakyThrows
    public void stop(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (!requestRepo.existsAllByChatIdAndStatusIsTrue(chatId) && cache.findByChatId(chatId) == null) {//userState.get(chatId)
            createErrorMessage(new NoSuchSessionException(), chatId);
        } else {
            cache.deleteByChatId(chatId);
            requestRepo.deactivate(chatId);
            sendCustomMessage(chatId, "Your request cancelled!");
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void handleMessage(Message message) throws TelegramApiException {
        String chatId = message.getChatId().toString();
        UserData data = cache.findByChatId(chatId);
        Question currentQuestion = data.currentQuestion();
        if (currentQuestion == null) {
            createErrorMessage(new NoSuchSessionException(), chatId);
            return;
        }
        if ((data = handleLanguage(data, message, chatId)).userLang() == null) return;
        if (data.data() == null)
            data.data(new HashMap<>());
        data.data().put(currentQuestion.getId(), message.getText());
        try {
            Question nextQuestion = currentQuestion.findNext(message.getText(), data.userLang());//userLang.get(chatId)
            handleNextQuestion(data, message, chatId, nextQuestion);
        } catch (IllegalOptionException | InputMismatchException exception) {
            createErrorMessage(exception, chatId);
        }
    }

    private void handleNextQuestion(UserData data, Message message, String chatId, Question nextQuestion) throws TelegramApiException {
        if (createReply(data, message.getChatId().toString(), nextQuestion)) {
            cache.updateByChatId(chatId, data.currentQuestion(nextQuestion));
        } else {
            String uuid = UUID.randomUUID().toString();
            requestRepo.save(Request.builder()
                    .uuid(uuid)
                    .chatId(chatId)
                    .clientId(message.getFrom().getId().toString())
                    .creationTime(LocalDateTime.now())
                    .data(data.data().toString())
                    .status(true)
                    .build());
            System.out.println("USER=" + message.getFrom().getFirstName() + " DATA=" + data.data());//userData.get(chatId)
            cache.deleteByChatId(chatId);
        }
    }

    private UserData handleLanguage(UserData data, Message message, String chatId) throws TelegramApiException {
        if (data.userLang() == null) {
            try {
                data.userLang(Locale.valueOf(message.getText()));
                return data;
            } catch (Exception e) {
                createErrorMessage(new IllegalOptionException(), chatId);
                return data;
            }
        }
        return data;
    }

    private void createErrorMessage(CustomException exception, String chatId) throws TelegramApiException {
        Locale text = cache.findByChatId(chatId) != null ? cache.findByChatId(chatId).userLang() : null;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(exception.getText(text))
                .build();
        execute(message);
    }

    @SuppressWarnings("SameParameterValue")
    private void sendCustomMessage(String chatId, String myMessage) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                .text(myMessage)
                .build();
        execute(message);
    }

    private boolean createReply(UserData data, String chatId, Question question) throws TelegramApiException {
        boolean result = true;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(question.getText(data.userLang()))
                .build();
        List<Action> actions = actionRepo.findAllByBaseQuestionOrderById(question);
        if (actions.size() == 0) {
            message.setReplyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
            result = false;
        } else if (actions.size() != 1) {
            message.setReplyMarkup(createKeyboard(data, actions));
        }
        execute(message);
        return result;
    }

    private ReplyKeyboardMarkup createKeyboard(UserData data, List<Action> actions) {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < actions.size(); i++) {
            String buttonText = actions.get(i).getText(data.userLang());
            row.add(buttonText);
            if ((i + 1) % 3 == 0 || i + 1 == actions.size()) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }
        kb.setKeyboard(keyboard);
        kb.setOneTimeKeyboard(true);
        return kb;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotPath() {
        return apiUrl + baseUrl;
    }

    public Map<Command, Consumer<Update>> getCommands() {
        return commands;
    }
}
