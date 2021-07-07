package az.code.tourapp.bots;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.*;
import az.code.tourapp.models.Action;
import az.code.tourapp.models.Command;
import az.code.tourapp.models.Question;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.QuestionRepository;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TourBot extends TelegramWebhookBot {

    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;

    private final String token;
    private final String username;
    private final String baseUrl;
    private final String apiUrl;

    private final Map<Command, Consumer<Update>> commands = new HashMap<>();
    private final Map<String, Map<Long, String>> userData = new HashMap<>();
    private final Map<String, Question> userState = new HashMap<>();
    private final Map<String, Locale> userLang = new HashMap<>();

    public TourBot(QuestionRepository questionRepo, ActionRepository actionRepo, String token, String username,
                   String baseUrl, String apiUrl) {
        this.questionRepo = questionRepo;
        this.actionRepo = actionRepo;
        this.token = token;
        this.username = username;
        this.baseUrl = baseUrl;
        this.apiUrl = apiUrl;
    }

    @SneakyThrows
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        /*        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }*/
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
        //TODO: Search org.springframework.data.domain.Example
        if (userState.get(update.getMessage().getChatId().toString()) != null)
            createErrorMessage(new AlreadyHaveSessionException(),update.getMessage().getChatId().toString());
        Question question = questionRepo.findById(1L).orElseThrow(MissingFirstQuestionException::new);
        userLang.put(update.getMessage().getChatId().toString(), null);
        userState.put(update.getMessage().getChatId().toString(), question);
        createReply(update.getMessage().getChatId().toString(), question);
    }

    private void handleMessage(Message message) throws TelegramApiException {
        String chatId = message.getChatId().toString();
        Question currentQuestion = userState.get(chatId);
        String answer = message.getText();
        if (handleLanguage(message, chatId)) return;
        userData.computeIfAbsent(chatId, k -> new HashMap<>());
        userData.get(chatId).put(currentQuestion.getId(), answer);
        try {
            Question nextQuestion = currentQuestion.findNext(message.getText(), userLang.get(chatId));
            handleNextQuestion(message, chatId, nextQuestion);
        } catch (IllegalOptionException | InputMismatchException exception) {
            createErrorMessage(exception, chatId);
        }
    }

    private void handleNextQuestion(Message message, String chatId, Question nextQuestion) throws TelegramApiException {
        if (nextQuestion != null) {
            userState.put(chatId, nextQuestion);
            createReply(message.getChatId().toString(), nextQuestion);
        } else {
            userState.put(chatId, null);
            System.out.println(userData.get(chatId));
        }
    }

    private boolean handleLanguage(Message message, String chatId) throws TelegramApiException {
        if (userLang.get(chatId) == null) {
            try {
                userLang.put(chatId, Locale.valueOf(message.getText()));
            } catch (Exception e) {
                createErrorMessage(new IllegalOptionException(), chatId);
                return true;
            }
        }
        return false;
    }

    /*    private void handleCallback(CallbackQuery query) {
        System.out.println(query.getData());
    }*/

    private void createErrorMessage(CustomException exception, String chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(exception.getText(userLang.get(chatId)))
                .build();
        execute(message);
    }

    private void createReply(String chatId, Question question) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(question.getText(userLang.get(chatId)))
                .build();
        List<Action> actions = actionRepo.findAllByBaseQuestionOrderById(question);
        if (actions.size() != 1) {
            message.setReplyMarkup(createKeyboard(chatId, actions));
        }
        execute(message);
    }

    private ReplyKeyboardMarkup createKeyboard(String chatId, List<Action> actions) {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < actions.size(); i++) {
            String buttonText = actions.get(i).getText(userLang.get(chatId));
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

    /*    private InlineKeyboardMarkup createKeyboard(List<Action> actions) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> sub = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            String buttonText = actions.get(i).getText();
            sub.add(InlineKeyboardButton.builder()
                        .callbackData(buttonText)
                        .text(buttonText)
                        .build());
            if (i + 1 % 3 == 0 || i + 1 == actions.size()) {
                keyboard.add(sub);
                sub = new ArrayList<>();
            }
        }
        kb.setKeyboard(keyboard);
        return kb;
    }*/

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
