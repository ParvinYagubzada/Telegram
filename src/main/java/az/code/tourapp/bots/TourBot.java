package az.code.tourapp.bots;

import az.code.tourapp.configs.DevRabbitConfig;
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
import az.code.tourapp.repositories.*;
import az.code.tourapp.services.FilesStorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TourBot extends TelegramWebhookBot {

    public static final String OFFER_QUEUE = "offerQueue";

    private final FilesStorageService store;
    private final RabbitTemplate rabbit;

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
    private final Map<String, CustomMessage> messages = new HashMap<>();

    private final Map<String, Integer> userOffers = new HashMap<>();
    private final Map<String, Integer> loadMoreMessages = new HashMap<>();

    @SuppressWarnings("SpellCheckingInspection")
    @PostConstruct
    private void init() {
        cache.setExpire(Duration.ofDays(1));
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
        messages.put("acceptOffer", CustomMessage.builder()
                .context("Yes, send my contact info.")
                .context_az("Bəli, şəxsi məlumatlarım yollanılsın.")
                .context_ru("Tercumeye ehtiyac var.")
                .build());
        messages.put("acceptOfferMessage", CustomMessage.builder()
                .context("Do you want to accept %s's offer?")
                .context_az("%s agentliyinin təklifini qəbul etmək istəyirsinizmi?")
                .context_ru("%s tercumeye ehtiyac var.")
                .build());
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
                        handleMessage(update.getMessage());
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
            userOffers.remove(chatId);
            String uuid = requestRepo.findUuidByChatId(chatId);
            if (uuid != null) {
                rabbit.convertAndSend(DevRabbitConfig.STOP_EXCHANGE, DevRabbitConfig.STOP_KEY, uuid);
            }
            requestRepo.deactivate(chatId);
            sendCustomMessage(chatId, "Your request cancelled!");
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
        if (!userOffers.containsKey(chatId) || userOffers.get(chatId) < 5) {
            Integer value = userOffers.get(chatId) != null ? userOffers.get(chatId) + 1 : 1;
            userOffers.put(chatId, value);
            newOffer.setBaseMessageId(sendOfferPhoto(fileName, chatId, offer.getAgencyName()));
            offerRepository.save(newOffer);
            store.delete(fileName);
        } else {
            offerRepository.save(newOffer);
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
        Offer offer = offerRepository.getByMessageId(message.getChatId().toString(),
                message.getMessageId().toString());
        rabbit.convertAndSend(DevRabbitConfig.ACCEPTED_EXCHANGE, DevRabbitConfig.ACCEPTED_KEY,
                new AcceptedOffer(offer.getUuid(), offer.getAgencyName(), contact));
    }

    private void handleReplyMessage(Message replyToMessage) throws TelegramApiException {
        String chatId = replyToMessage.getChatId().toString();
        String messageId = replyToMessage.getMessageId().toString();
        Offer offer = offerRepository.getByMessageId(chatId, messageId);
        Request request = requestRepo.findByUuid(offer.getUuid());
        SendMessage message = createRequestContactMessage(chatId, messageId, offer, request);
        offerRepository.save(offer.toBuilder()
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
        String chatId = query.getMessage().getChatId().toString();
        String uuid = query.getData().split("&")[1];
        List<Offer> list = offerRepository.findTop5(chatId, PageRequest.of(0, 5));
        Request request = requestRepo.findByUuid(uuid);
        offerRepository.saveAll(list.stream()
                .map(offer -> {
                    String messageId = sendOfferPhoto(offer.getPhotoUrl(), chatId, offer.getAgencyName());
                    return offer.toBuilder().baseMessageId(messageId).build();
                })
                .collect(Collectors.toList())
        );
        execute(DeleteMessage.builder()
                .chatId(chatId)
                .messageId(loadMoreMessages.get(chatId)).build());
        store.deleteAll(list);
        loadMoreMessages.remove(chatId);
        handleMoreOffers(chatId, uuid, request);
    }

    private void handleMoreOffers(String chatId, String uuid, Request request) throws TelegramApiException {
        int count = offerRepository.countAllByChatIdAndBaseMessageIdIsNull(chatId);
        if (count != 0) {
            if (!loadMoreMessages.containsKey(chatId)) {
                loadMoreMessages.put(chatId,
                        execute(createLoadMore(chatId, uuid, request.getLang(), count)).getMessageId());
            } else {
                execute(editLoadMore(chatId, uuid, loadMoreMessages.get(chatId), request.getLang(), count));
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void handleMessage(Message message) throws TelegramApiException {
        String chatId = message.getChatId().toString();
        UserData data = cache.findByChatId(chatId);
        if (data == null || data.currentQuestion() == null) {
            sendErrorMessage(new NoSuchSessionException(), chatId);
            return;
        }
        Question currentQuestion = data.currentQuestion();
        if ((data = handleLanguage(data, message, chatId)).userLang() == null) return;
        if (data.data() == null)
            data.data(new HashMap<>());
        data.data().put(currentQuestion.getFieldName(), message.getText());
        try {
            Question nextQuestion = currentQuestion.findNext(message.getText(), data.userLang());
            handleNextQuestion(data, message, chatId, nextQuestion);
        } catch (IllegalOptionException | InputMismatchException exception) {
            sendErrorMessage(exception, chatId);
        }
    }

    private UserData handleLanguage(UserData data, Message message, String chatId) throws TelegramApiException {
        if (data.userLang() == null) {
            try {
                data.userLang(Locale.valueOf(message.getText()));
                return data;
            } catch (Exception e) {
                sendErrorMessage(new IllegalOptionException(), chatId);
                return data;
            }
        }
        return data;
    }

    private void handleNextQuestion(UserData data, Message message, String chatId, Question nextQuestion) throws TelegramApiException {
        if (sendQuestion(data, message.getChatId().toString(), nextQuestion)) {
            cache.updateByChatId(chatId, data.currentQuestion(nextQuestion));
        } else {
            String uuid = UUID.randomUUID().toString();
            String userData = data.data().toString();
            requestRepo.save(Request.builder()
                    .uuid(uuid)
                    .chatId(chatId)
                    .clientId(message.getFrom().getId().toString())
                    .creationTime(LocalDateTime.now())
                    .data(data.data().toString())
                    .lang(extractLocale(userData))
                    .status(true)
                    .build());
            System.out.println("USER=" + message.getFrom().getFirstName() + " DATA=" + userData);
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

    private void sendErrorMessage(CustomException exception, String chatId) throws TelegramApiException {
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
        return apiUrl + baseUrl;
    }

    public Map<Command, Consumer<Update>> getCommands() {
        return commands;
    }
}
