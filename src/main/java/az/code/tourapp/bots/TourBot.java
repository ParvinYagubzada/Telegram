package az.code.tourapp.bots;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.InputMismatchException;
import az.code.tourapp.exceptions.*;
import az.code.tourapp.models.Command;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.UserData;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.models.entities.Offer;
import az.code.tourapp.models.entities.Question;
import az.code.tourapp.models.entities.Request;
import az.code.tourapp.repositories.*;
import az.code.tourapp.services.FilesStorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class TourBot extends TelegramWebhookBot {

    private final FilesStorageService store;

    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;
    private final RequestRepository requestRepo;
    private final OfferRepository offerRepository;
    private final RedisRepository cache;

    private final String token;
    private final String username;
    private final String baseUrl;
    private final String apiUrl;

    private final Map<Command, Consumer<Update>> commands = new HashMap<>();
    private final Map<String, Integer> userOffers = new HashMap<>();
    private final Map<String, Integer> loadMoreMessages = new HashMap<>();
    private final Map<String, CustomMessage> messages = new HashMap<>();

    @PostConstruct
    private void init() {
        messages.put("loadMore", CustomMessage.builder()
                .context("Load more.")
                .context_az("Daha çox yüklə.")
                .context_ru("Tercumeciye ehtiyac var.")
                .build());
        messages.put("loadMoreMessage", CustomMessage.builder()
                .context("You have %d more offers.")
                .context_az("Sizin daha %d təklifiniz var.")
                .context_ru("%d tercumeciye ehtiyac var.")
                .build());
    }

    @SneakyThrows
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
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
        if (!requestRepo.existsAllByChatIdAndStatusIsTrue(chatId) && cache.findByChatId(chatId) == null) {
            createErrorMessage(new NoSuchSessionException(), chatId);
        } else {
            cache.deleteByChatId(chatId);
            userOffers.remove(chatId);
            requestRepo.deactivate(chatId);
            sendCustomMessage(chatId, "Your request cancelled!");
        }
    }

    public boolean sendResponse(String uuid, MultipartFile file) throws IOException, TelegramApiException {
        String fileName = UUID.randomUUID().toString();
        Request request = requestRepo.findByUuidAndStatusIsTrue(uuid).orElseThrow(NoSuchRequestException::new);
        String chatId = request.getChatId();
        store.save(file, fileName);
        if (!userOffers.containsKey(chatId) || userOffers.get(chatId) < 5) {
            Integer value = userOffers.get(chatId) != null ? userOffers.get(chatId) + 1 : 1;
            userOffers.put(chatId, value);
            sendOfferPhoto(fileName, chatId);
            store.delete(fileName);
        } else {
            offerRepository.save(Offer.builder().uuid(uuid).chatId(chatId).photoUrl(fileName).build());
            handleLoadMore(chatId, uuid, request);
        }
        return true;
    }

    @SneakyThrows
    private void sendOfferPhoto(String fileName, String chatId) {
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(store.load(fileName).getFile())).build();
        execute(sendPhoto);
    }

    private Locale extractLocale(String data) {
        String fieldName = "language=";
        int start = data.indexOf(fieldName) + fieldName.length();
        int end = data.indexOf(",", start);
        return Locale.valueOf(data.substring(start, end));
    }

    private SendMessage loadMore(String chatId, String uuid, Locale locale, Integer count) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(String.format(messages.get("loadMoreMessage").getText(locale), count));
        InlineKeyboardMarkup kb = createInlineKeyboardMarkup(uuid, locale);
        message.setReplyMarkup(kb);
        return message;
    }

    private EditMessageText refreshMessage(String chatId, String uuid, Integer messageId, Locale locale, Integer count) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(String.format(messages.get("loadMoreMessage").getText(locale), count));
        InlineKeyboardMarkup kb = createInlineKeyboardMarkup(uuid, locale);
        message.setReplyMarkup(kb);
        return message;
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(String uuid, Locale locale) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> sub = new ArrayList<>();
        sub.add(InlineKeyboardButton.builder()
                .callbackData("loadMore&" + uuid)
                .text(messages.get("loadMore").getText(locale))
                .build());
        keyboard.add(sub);
        kb.setKeyboard(keyboard);
        return kb;
    }

    private void handleCallbackQuery(CallbackQuery query) throws TelegramApiException {
        String chatId = query.getMessage().getChatId().toString();
        String uuid = query.getData().split("&")[1];
        List<Offer> list = offerRepository.findTop5ByChatIdOrderByTimeStampAsc(chatId);
        Request request = requestRepo.findByUuid(uuid);
        offerRepository.deleteAll(list);
        execute(DeleteMessage.builder()
                .chatId(chatId)
                .messageId(loadMoreMessages.get(chatId)).build());
        list.forEach(offer -> sendOfferPhoto(offer.getPhotoUrl(), chatId));
        store.deleteAll(list);
        loadMoreMessages.remove(chatId);
        handleLoadMore(chatId, uuid, request);
    }

    private void handleLoadMore(String chatId, String uuid, Request request) throws TelegramApiException {
        int count = offerRepository.countAllByChatId(chatId);
        if (count != 0) {
            if (!loadMoreMessages.containsKey(chatId)) {
                loadMoreMessages.put(chatId, execute(loadMore(chatId, uuid, extractLocale(request.getData()),
                        count)).getMessageId());
            } else {
                execute(refreshMessage(chatId, uuid, loadMoreMessages.get(chatId),
                        extractLocale(request.getData()),
                        count));
            }
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
        data.data().put(currentQuestion.getFieldName(), message.getText());
        try {
            Question nextQuestion = currentQuestion.findNext(message.getText(), data.userLang());
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
