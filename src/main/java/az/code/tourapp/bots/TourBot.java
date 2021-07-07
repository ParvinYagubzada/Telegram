package az.code.tourapp.bots;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.InputMismatchException;
import az.code.tourapp.exceptions.*;
import az.code.tourapp.models.Action;
import az.code.tourapp.models.Command;
import az.code.tourapp.models.Question;
import az.code.tourapp.models.Request;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.QuestionRepository;
import az.code.tourapp.repositories.RequestRepository;
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

public class TourBot extends TelegramWebhookBot {

    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;
    private final RequestRepository requestRepo;

    private final String token;
    private final String username;
    private final String baseUrl;
    private final String apiUrl;

    private final Map<Command, Consumer<Update>> commands = new HashMap<>();
    private final Map<String, Map<Long, String>> userData = new HashMap<>();
    private final Map<String, Question> userState = new HashMap<>();
    private final Map<String, Locale> userLang = new HashMap<>();

    public TourBot(QuestionRepository questionRepo, ActionRepository actionRepo, RequestRepository requestRepo,
                   String token, String username, String baseUrl, String apiUrl) {
        this.questionRepo = questionRepo;
        this.actionRepo = actionRepo;
        this.requestRepo = requestRepo;
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
        String chatId = update.getMessage().getChatId().toString();
        if (requestRepo.existsAllByChatIdAndStatusIsTrue(chatId)) {
            createErrorMessage(new AlreadyHaveSessionException(), chatId);
            return;
        }
        Question question = questionRepo.findById(1L).orElseThrow(MissingFirstQuestionException::new);
        userLang.put(chatId, null);
        userState.put(chatId, question);
        createReply(chatId, question);
    }

    @SneakyThrows
    public void stop(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (!requestRepo.existsAllByChatIdAndStatusIsTrue(chatId) && userState.get(chatId) == null) {
            createErrorMessage(new NoSuchSessionException(), chatId);
        } else {
            userState.remove(chatId);
            userData.remove(chatId);
            userLang.remove(chatId);
            requestRepo.deactivate(chatId);
            sendCustomMessage(chatId, "Your request cancelled!");
        }
    }

    private void handleMessage(Message message) throws TelegramApiException {
        String chatId = message.getChatId().toString();
        Question currentQuestion = userState.get(chatId);
        if (currentQuestion == null) {
            createErrorMessage(new NoSuchSessionException(), chatId);
            return;
        }
        if (handleLanguage(message, chatId)) return;
        userData.computeIfAbsent(chatId, k -> new HashMap<>());
        userData.get(chatId).put(currentQuestion.getId(), message.getText());
        try {
            Question nextQuestion = currentQuestion.findNext(message.getText(), userLang.get(chatId));
            handleNextQuestion(message, chatId, nextQuestion);
        } catch (IllegalOptionException | InputMismatchException exception) {
            createErrorMessage(exception, chatId);
        }
    }

    private void handleNextQuestion(Message message, String chatId, Question nextQuestion) throws TelegramApiException {
        if (createReply(message.getChatId().toString(), nextQuestion)) {
            userState.put(chatId, nextQuestion);
        } else {
            userState.put(chatId, null);
            String uuid = UUID.randomUUID().toString();
            requestRepo.save(Request.builder()
                    .uuid(uuid)
                    .chatId(chatId)
                    .clientId(message.getFrom().getId().toString())
                    .creationTime(LocalDateTime.now())
                    .data(userData.get(chatId).toString())
                    .status(true)
                    .build());
            System.out.println("USER=" + message.getFrom().getFirstName() + " DATA=" + userData.get(chatId));
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

    private void sendCustomMessage(String chatId, String myMessage) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                .text(myMessage)
                .build();
        execute(message);
    }

    private boolean createReply(String chatId, Question question) throws TelegramApiException {
        boolean result = true;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(question.getText(userLang.get(chatId)))
                .build();
        List<Action> actions = actionRepo.findAllByBaseQuestionOrderById(question);
        if (actions.size() == 0) {
            message.setReplyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
            result = false;
        } else if (actions.size() != 1) {
            message.setReplyMarkup(createKeyboard(chatId, actions));
        }
        execute(message);
        return result;
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
