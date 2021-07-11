package az.code.tourapp.bots;

import az.code.tourapp.configs.TelegramConfiguration;
import az.code.tourapp.configs.dev.DevRabbitConfig;
import az.code.tourapp.enums.ActionType;
import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.InputMismatchException;
import az.code.tourapp.exceptions.*;
import az.code.tourapp.models.Command;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.UserData;
import az.code.tourapp.models.dto.AcceptedOffer;
import az.code.tourapp.models.dto.RawOffer;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.models.entities.Offer;
import az.code.tourapp.models.entities.Question;
import az.code.tourapp.models.entities.Request;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.OfferRepository;
import az.code.tourapp.repositories.QuestionRepository;
import az.code.tourapp.repositories.RequestRepository;
import az.code.tourapp.repositories.cache.LastMessageIdRepository;
import az.code.tourapp.repositories.cache.OfferCountRepository;
import az.code.tourapp.repositories.cache.UserDataRepository;
import az.code.tourapp.services.FilesStorageService;
import az.code.tourapp.utils.CalendarUtil;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TourBot extends TelegramWebhookBot {

    public static final String OFFER_QUEUE = "offerQueue";
    public static final String IGNORE = "ignore";
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final FilesStorageService store;
    private final RabbitTemplate rabbit;

    //SQL Repositories
    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;
    private final RequestRepository requestRepo;
    private final OfferRepository offerRepo;

    //Redis Repos
    private final UserDataRepository cache;
    private final LastMessageIdRepository lastMessageRepo;
    private final OfferCountRepository offerCountRepo;

    private final String token;
    private final String username;
    private final String domain;
    private final String api;

    private final Map<Command, Consumer<Update>> commands = new HashMap<>();
    private final Map<String, CustomMessage> messages;

    public TourBot(TelegramConfiguration properties) {
        store = properties.getStore();
        rabbit = properties.getTemplate();
        questionRepo = properties.getQuestionRepo();
        actionRepo = properties.getActionRepo();
        requestRepo = properties.getRequestRepo();
        offerRepo = properties.getOfferRepo();
        cache = properties.getUserDataRepo();
        lastMessageRepo = properties.getLastMessageRepo();
        offerCountRepo = properties.getOfferCountRepo();
        token = properties.getToken();
        username = properties.getUsername();
        domain = properties.getDomain();
        api = properties.getApi();
        messages = properties.getMessages();
    }

    @PostConstruct
    private void init() throws TelegramApiException {
        commands.put(new Command("stop", "Stops bot current interrogation."), this::stop);
        commands.put(new Command("start", "Starts bot interrogation!"), this::interrogate);
        execute(SetWebhook.builder().url(domain + api).dropPendingUpdates(true).build());
        execute(SetMyCommands.builder().commands(new ArrayList<>(commands.keySet())).build());
        cache.setExpire(Duration.ofDays(3));
        lastMessageRepo.setExpire(Duration.ofDays(14));
        offerCountRepo.setExpire(Duration.ofDays(14));
    }

    @SneakyThrows
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                String msg = update.getMessage().getText();
                if (msg.startsWith("/")) {
                    msg = msg.substring(1);
                    Consumer<Update> action;
                    if ((action = commands.get(new Command(msg))) != null) {
                        action.accept(update);
                    }
                } else {
                    Message message = update.getMessage();
                    if (message.getReplyToMessage() != null && message.getReplyToMessage().hasPhoto()) {
                        handleReplyMessage(message.getReplyToMessage());
                    } else {
                        handleMessage(message.getChatId().toString(), message.getText(), message.getFrom());
                    }
                }
            } else {
                Message message = update.getMessage();
                handleContact(message.getReplyToMessage(), message.getContact());
            }
        }
        return null;
    }

    @SneakyThrows
    public void interrogate(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (requestRepo.existsAllByChatIdAndStatusIsTrue(chatId) || cache.findByChatId(chatId) != null) {
            sendErrorMessage(new AlreadyHaveSessionException(), chatId);
            return;
        }
        Question question = questionRepo.findById(1L).orElseThrow(MissingFirstQuestionException::new);
        UserData data = UserData.builder().userLang(null).currentQuestion(question).build();
        cache.saveByChatId(chatId, data);
        sendQuestion(data, chatId, question);
    }

    @SneakyThrows
    public void stop(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (!requestRepo.existsAllByChatIdAndStatusIsTrue(chatId) && cache.findByChatId(chatId) == null) {
            sendErrorMessage(new NoSuchSessionException(), chatId);
        } else {
            cache.deleteByChatId(chatId);
            Request request = requestRepo.findUuidByChatId(chatId);
            Locale locale = null;
            if (request != null) {
                rabbit.convertAndSend(DevRabbitConfig.STOP_EXCHANGE, DevRabbitConfig.STOP_KEY, request.getUuid());
                locale = request.getLang();
            }
            requestRepo.deactivate(chatId);
            sendCustomMessage(chatId, messages.get("stopMessage").getText(locale));
        }
    }

    @RabbitListener(queues = OFFER_QUEUE)
    public void receiveResponse(RawOffer offer) throws TelegramApiException {
        String uuid = offer.getUuid();
        String fileName = UUID.randomUUID().toString();
        Request request = requestRepo.findByUuidAndStatusIsTrue(uuid).orElse(null);
        if (request == null)
            return;
        String chatId = request.getChatId();
        store.save(new ByteArrayInputStream(offer.getData()), fileName);
        Offer newOffer = Offer.builder()
                .uuid(uuid)
                .chatId(chatId)
                .photoUrl(fileName)
                .agencyName(offer.getAgencyName()).build();
        if (!offerCountRepo.containsKey(chatId, uuid) || offerCountRepo.findOfferCount(chatId, uuid) < 5) {
            Integer value = offerCountRepo.findOfferCount(chatId, uuid) != null ?
                    offerCountRepo.incrementOfferCount(chatId, uuid) : 1;
            offerCountRepo.saveOfferCount(chatId, uuid, value);
            newOffer.setBaseMessageId(sendOfferPhoto(fileName, chatId, offer.getAgencyName()));
            offerRepo.save(newOffer);
            store.delete(fileName);
        } else {
            offerRepo.save(newOffer);
            handleMoreOffers(chatId, uuid, request);
        }
    }

    @SneakyThrows
    private String sendOfferPhoto(String fileName, String chatId, String agencyName) {
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(chatId)
                .caption(agencyName)
                .photo(new InputFile(store.load(fileName).getFile())).build();
        return execute(sendPhoto).getMessageId().toString();
    }

    private SendMessage createLoadMore(String chatId, String uuid, Locale locale, Integer count) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(String.format(messages.get("loadMoreMessage").getText(locale), count));
        InlineKeyboardMarkup kb = createInlineKeyboardMarkup(uuid, locale);
        message.setReplyMarkup(kb);
        return message;
    }

    private EditMessageText editLoadMore(String chatId, String uuid, Integer messageId, Locale locale, Integer count) {
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

    private void handleContact(Message message, Contact contact) {
        Offer offer = offerRepo.getByMessageId(message.getChatId().toString(),
                message.getMessageId().toString());
        rabbit.convertAndSend(DevRabbitConfig.ACCEPTED_EXCHANGE, DevRabbitConfig.ACCEPTED_KEY,
                new AcceptedOffer(offer.getUuid(), offer.getAgencyName(), contact));
    }

    private void handleReplyMessage(Message replyToMessage) throws TelegramApiException {
        String chatId = replyToMessage.getChatId().toString();
        String messageId = replyToMessage.getMessageId().toString();
        Offer offer = offerRepo.getByMessageId(chatId, messageId);
        Request request = requestRepo.findByUuid(offer.getUuid());
        SendMessage message = createRequestContactMessage(chatId, messageId, offer, request);
        offerRepo.save(offer.toBuilder()
                .messageId(execute(message).getMessageId().toString())
                .build());
    }

    private SendMessage createRequestContactMessage(String chatId, String messageId, Offer offer, Request request) {
        return SendMessage.builder()
                .chatId(chatId)
                .replyToMessageId(Integer.parseInt(messageId))
                .text(String.format(messages.get("acceptOfferMessage").getText(request.getLang()),
                        offer.getAgencyName()))
                .replyMarkup(createRequestContactKeyboard(request))
                .build();
    }

    private ReplyKeyboardMarkup createRequestContactKeyboard(Request request) {
        KeyboardRow row = new KeyboardRow();
        row.add(KeyboardButton.builder()
                .requestContact(true)
                .text(messages.get("acceptOffer").getText(request.getLang()))
                .build());
        return ReplyKeyboardMarkup.builder()
                .keyboardRow(row)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

    private void handleCallbackQuery(CallbackQuery query) throws TelegramApiException {
        if (!query.getData().startsWith("loadMore")) {
            handleCalendar(query);
            return;
        }
        String chatId = query.getMessage().getChatId().toString();
        String uuid = query.getData().split("&")[1];
        List<Offer> list = offerRepo.findTop5(chatId, uuid, PageRequest.of(0, 5));
        Request request = requestRepo.findByUuid(uuid);
        offerRepo.saveAll(list.stream()
                .map(offer -> {
                    String messageId = sendOfferPhoto(offer.getPhotoUrl(), chatId, offer.getAgencyName());
                    return offer.toBuilder().baseMessageId(messageId).build();
                })
                .collect(Collectors.toList()));
        try {
            execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(lastMessageRepo.findLastMessageId(chatId, uuid)).build());
        } catch (OfferExpiredException exc) {
            sendErrorMessage(exc, chatId);
        }
        store.deleteAll(list);
        lastMessageRepo.deleteLastMessageId(chatId, uuid);
        handleMoreOffers(chatId, uuid, request);
    }

    private void handleCalendar(CallbackQuery query) throws TelegramApiException {
        String chatId = query.getMessage().getChatId().toString();
        Integer messageId = query.getMessage().getMessageId();
        String choice = query.getData();
        if (!choice.equals(IGNORE)) {
            if (!choice.startsWith("<") && !choice.startsWith(">")) {
                handleCalendarChoice(chatId, messageId, choice);
            } else {
                LocalDate newDate = choice.startsWith("<") ?
                        LocalDate.parse(choice.substring(1), formatter).minusMonths(1):
                        LocalDate.parse(choice.substring(1), formatter).plusMonths(1);
                execute(EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(CalendarUtil.generateKeyboard(newDate))
                        .build());
            }
        }
    }

    private void handleCalendarChoice(String chatId, Integer messageId, String choice) throws TelegramApiException {
        execute(EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(choice)
                .build());
        handleMessage(chatId, choice, null);
    }

    private void handleMoreOffers(String chatId, String uuid, Request request) throws TelegramApiException {
        int count = offerRepo.countAllByChatIdAndUuidAndBaseMessageIdIsNull(chatId, uuid);
        if (count != 0) {
            if (!lastMessageRepo.containsKey(chatId, uuid)) {
                lastMessageRepo.saveLastMessageId(chatId, uuid,
                        execute(createLoadMore(chatId, uuid, request.getLang(), count)).getMessageId());
            } else {
                execute(editLoadMore(chatId, uuid, lastMessageRepo.findLastMessageId(chatId, uuid),
                        request.getLang(), count));
            }
        }
        if (count == 0 && !request.getStatus()) {
            lastMessageRepo.deleteLastMessageId(chatId, uuid);
            offerCountRepo.deleteOfferCount(chatId, uuid);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void handleMessage(String chatId, String text, User user) throws TelegramApiException {
        UserData data = cache.findByChatId(chatId);
        if (data == null || data.currentQuestion() == null) {
            sendErrorMessage(new NoSuchSessionException(), chatId);
            return;
        }
        Question currentQuestion = data.currentQuestion();
        if ((data = handleLanguage(data, text, chatId)).userLang() == null) return;
        if (data.data() == null)
            data.data(new HashMap<>());
        data.data().put(currentQuestion.getFieldName(), text);
        try {
            Question nextQuestion = currentQuestion.findNext(text, data.userLang());
            handleNextQuestion(data, chatId, user, nextQuestion);
        } catch (IllegalOptionException | InputMismatchException exception) {
            sendErrorMessage(exception, chatId);
        }
    }

    private UserData handleLanguage(UserData data, String text, String chatId) throws TelegramApiException {
        if (data.userLang() == null) {
            try {
                data.userLang(Locale.valueOf(text));
                return data;
            } catch (Exception e) {
                sendErrorMessage(new IllegalOptionException(), chatId);
                return data;
            }
        }
        return data;
    }

    private void handleNextQuestion(UserData data, String chatId, User user, Question nextQuestion) throws TelegramApiException {
        if (sendQuestion(data, chatId, nextQuestion)) {
            cache.saveByChatId(chatId, data.currentQuestion(nextQuestion));
        } else {
            String uuid = UUID.randomUUID().toString();
            String userData = data.data().toString();
            requestRepo.save(Request.builder()
                    .uuid(uuid)
                    .chatId(chatId)
                    .clientId(user.getId().toString())
                    .creationTime(LocalDateTime.now())
                    .data(data.data().toString())
                    .lang(extractLocale(userData))
                    .status(true)
                    .build());
            System.out.println("USER=" + user.getFirstName() + " DATA=" + userData);
            data.data().put("uuid", uuid);
            rabbit.convertAndSend(DevRabbitConfig.REQUEST_EXCHANGE, DevRabbitConfig.REQUEST_KEY, userData);
            cache.deleteByChatId(chatId);
        }
    }

    private boolean sendQuestion(UserData data, String chatId, Question question) throws TelegramApiException {
        boolean result = true;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(question.getText(data.userLang()))
                .build();
        List<Action> actions = actionRepo.findAllByBaseQuestionOrderById(question);
        if (actions.size() == 0) {
            message.setReplyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
            result = false;
        } else if (actions.get(0).getType().equals(ActionType.DATE)) {
            message.setReplyMarkup(CalendarUtil.generateKeyboard(LocalDate.now()));
        } else if (actions.get(0).getType().equals(ActionType.BUTTON)) {
            message.setReplyMarkup(createKeyboard(data, actions));
        } else {
            message.setReplyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        }
        execute(message);
        return result;
    }

    private ReplyKeyboardMarkup createKeyboard(UserData data, List<Action> actions) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < actions.size(); i++) {
            String buttonText = actions.get(i).getText(data.userLang());
            row.add(buttonText);
            if ((i + 1) % 2 == 0 || i + 1 == actions.size()) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }
        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .build();
    }

    private void sendErrorMessage(CustomException exception, String chatId) throws TelegramApiException {
        Locale text = cache.findByChatId(chatId) != null ? cache.findByChatId(chatId).userLang() : null;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(exception.getText(text))
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

    private Locale extractLocale(String data) {
        String fieldName = "language=";
        int start = data.indexOf(fieldName) + fieldName.length();
        int end = data.indexOf(",", start);
        return Locale.valueOf(data.substring(start, end));
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
        return api + domain;
    }
}
