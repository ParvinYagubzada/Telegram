package az.code.tourapp.bots;

import az.code.tourapp.configs.RabbitConfig;
import az.code.tourapp.enums.ActionType;
import az.code.tourapp.enums.ButtonType;
import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.api.MissingFirstQuestionException;
import az.code.tourapp.exceptions.user.InputMismatchException;
import az.code.tourapp.exceptions.user.*;
import az.code.tourapp.models.Command;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.Translatable;
import az.code.tourapp.models.UserData;
import az.code.tourapp.models.dto.RawOffer;
import az.code.tourapp.models.entities.*;
import az.code.tourapp.repositories.*;
import az.code.tourapp.repositories.cache.ContactRepository;
import az.code.tourapp.repositories.cache.LastMessageIdRepository;
import az.code.tourapp.repositories.cache.OfferCountRepository;
import az.code.tourapp.repositories.cache.UserDataRepository;
import az.code.tourapp.services.FilesStorageService;
import az.code.tourapp.utils.CalendarUtil;
import az.code.tourapp.utils.Mappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static az.code.tourapp.helpers.BotHelper.*;
import static az.code.tourapp.utils.CalendarUtil.IGNORE;

@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "commands")
public class TourBot extends TelegramWebhookBot {

    public static final Logger logger = LoggerFactory.getLogger(TourBot.class);
    public static final String OFFER_QUEUE = "offerQueue";
    public static final String EXPIRATION_QUEUE = "expirationQueue";

    private FilesStorageService store;
    private RabbitTemplate rabbit;
    private Mappers mappers;

    //SQL Repositories
    private QuestionRepository questionRepo;
    private ActionRepository actionRepo;
    private RequestRepository requestRepo;
    private OfferRepository offerRepo;
    private UserRepository userRepo;
    //Redis Repos
    private UserDataRepository userDataRepo;
    private LastMessageIdRepository lastMessageRepo;
    private OfferCountRepository offerCountRepo;
    private ContactRepository contactRepo;

    private String token;
    private String username;
    private String domain;
    private String api;
    private Long firstQuestionId;
    private Integer expirationDays;
    private Map<String, CustomMessage> messages;

    private Map<Command, Consumer<Update>> commands = new HashMap<>();

    public TourBot init() throws TelegramApiException {
        commands.put(new Command("stop", "Stops bot current interrogation."), this::stop);
        commands.put(new Command("start", "Starts bot interrogation!"), this::interrogate);
        execute(SetWebhook.builder().url(domain + api).dropPendingUpdates(true).build());
        execute(SetMyCommands.builder().commands(new ArrayList<>(commands.keySet())).build());
        return this;
    }

    @SneakyThrows
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                handleTextMessage(update);
            } else if (message.isReply()) {
                handleContact(message.getReplyToMessage(), message.getFrom().getUserName(), message.getContact());
            }
        }
        return null;
    }

    private void handleTextMessage(Update update) throws TelegramApiException {
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
                handleReplyTextMessage(message, message.getReplyToMessage());
            } else {
                handleMessage(message.getChatId().toString(), message.getText(), message.getFrom());
            }
        }
    }

    @SneakyThrows
    private void interrogate(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (requestRepo.existsByChatIdAndActiveIsTrue(chatId) || userDataRepo.findByChatId(chatId) != null) {
            sendErrorMessage(new AlreadyHaveSessionException(), chatId);
            return;
        }
        Question question = questionRepo.findById(firstQuestionId).orElseThrow(MissingFirstQuestionException::new);
        UserData data = UserData.builder().userLang(null).currentQuestion(question).build();
        userDataRepo.saveByChatId(chatId, data);
        sendQuestion(data, chatId, question);
    }

    @SneakyThrows
    private void stop(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (!requestRepo.existsByChatIdAndActiveIsTrue(chatId) && userDataRepo.findByChatId(chatId) == null) {
            sendErrorMessage(new NoSuchSessionException(), chatId);
        } else {
            userDataRepo.deleteByChatId(chatId);
            Request request = requestRepo.findByChatIdAndActiveIsTrue(chatId);
            Locale locale = null;
            if (request != null) {
                deleteLoadMoreButton(chatId);
                deactivateRequestAndClearCache(request);
                locale = request.getLang();
            }
            execute(createCustomMessage(chatId, getText(messages.get("stopMessage"), locale)));
        }
    }

    @RabbitListener(queues = OFFER_QUEUE)
    private void receiveOffer(RawOffer offer) throws TelegramApiException {
        String uuid = offer.getUuid();
        String fileName = UUID.randomUUID().toString();
        Request request = requestRepo.findByUuidAndActiveIsTrue(uuid);
        if (request == null) return;
        String chatId = request.getChatId();
        store.save(new ByteArrayInputStream(offer.getData()), fileName);
        Offer newOffer = Offer.builder()
                .uuid(uuid)
                .chatId(chatId)
                .photoUrl(fileName)
                .agencyName(offer.getAgencyName()).build();
        if (!offerCountRepo.containsKey(chatId) || offerCountRepo.findOfferCount(chatId) < 5) {
            offerCountRepo.incrementOfferCount(chatId);
            newOffer.setBaseMessageId(sendOfferPhoto(fileName, chatId, offer.getAgencyName()));
            offerRepo.save(newOffer);
            store.delete(fileName);
        } else {
            offerRepo.save(newOffer);
            handleMoreOffers(chatId, uuid, request);
        }
    }

    @RabbitListener(queues = EXPIRATION_QUEUE)
    private void receiveExpirations(List<String> expiredRequestUuids) throws TelegramApiException {
        for (String uuid : expiredRequestUuids) {
            Request request = requestRepo.findByUuid(uuid);
            String chatId = request.getChatId();
            if (offerRepo.existsByUuid(uuid)) {
                requestRepo.save(request.setExpirationTime(LocalDateTime.now().plusDays(expirationDays)));
                execute(createCustomMessage(chatId, getText(messages.get("expirationInfoMessage"), request.getLang())));
            } else {
                requestRepo.save(request.setActive(false));
                deleteLoadMoreButton(chatId);
                execute(createCustomMessage(chatId, getText(messages.get("stopMessage"), request.getLang())));
            }
        }
    }

    private void deleteLoadMoreButton(String chatId) throws TelegramApiException {
        Integer messageId = lastMessageRepo.findLastMessageId(chatId);
        if (messageId != null) {
            lastMessageRepo.deleteLastMessageId(chatId);
            List<Offer> offers = offerRepo.findAll(
                    Example.of(Offer.builder().chatId(chatId).baseMessageId(null).build())
            );
            offerRepo.deleteAll(offers);
            store.deleteAll(offers);
            execute(createDeleteMessage(chatId, messageId));
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

    private void handleContact(Message replyMessage, String username, Contact contact) throws TelegramApiException {
        Offer offer = offerRepo.getByMessageId(replyMessage.getChatId().toString(),
                replyMessage.getMessageId().toString());
        if (offer != null) {
            Request request = requestRepo.findByUuid(offer.getUuid());
            Locale locale = request.getLang();
            userRepo.save(mappers.contactToBotUser(username, contact));
            rabbit.convertAndSend(RabbitConfig.ACCEPTED_EXCHANGE, RabbitConfig.ACCEPTED_KEY,
                    mappers.contactToAcceptedOffer(offer.getUuid(), offer.getAgencyName(), username, contact));
            sendInfoMessage(offer, locale);
            requestRepo.save(request.setAccepted(true));
        }
    }

    private void handleReplyTextMessage(Message userMessage, Message replyToMessage) throws TelegramApiException {
        String chatId = replyToMessage.getChatId().toString();
        Integer messageId = replyToMessage.getMessageId();
        Offer offer = offerRepo.getByMessageId(chatId, messageId.toString());
        Request request = requestRepo.findByUuid(offer.getUuid());
        if (!isValid(chatId, request)) return;
        Locale locale = request.getLang();
        SendMessage message = createRequestContactMessage(userMessage, messageId, offer, locale);
        offerRepo.save(offer.toBuilder()
                .messageId(execute(message).getMessageId().toString())
                .build());
    }

    private boolean isValid(String chatId, Request request) throws TelegramApiException {
        if (request.isAccepted()) {
            sendErrorMessage(new MultipleAcceptanceException(), chatId);
            return deactivateRequestAndClearCache(request);
        }
        if (!request.isActive() || request.getExpirationTime().isBefore(LocalDateTime.now())) {
            sendErrorMessage(new RequestExpiredException(), chatId);
            return deactivateRequestAndClearCache(request);
        }
        return true;
    }

    private boolean deactivateRequestAndClearCache(Request request) {
        requestRepo.save(request.setActive(false));
        rabbit.convertAndSend(RabbitConfig.STOP_EXCHANGE, RabbitConfig.STOP_KEY, request.getUuid());
        contactRepo.deleteMessageId(request.getChatId());
        offerCountRepo.deleteOfferCount(request.getChatId());
        return false;
    }

    private SendMessage createRequestContactMessage(Message message, Integer messageId, Offer offer, Locale locale) {
        String chatId = message.getChatId().toString();
        Long userId = message.getFrom().getId();
        Optional<BotUser> user = userRepo.findById(userId);
        contactRepo.saveMessageId(chatId, messageId);
        SendMessage result = SendMessage.builder()
                .chatId(chatId)
                .replyToMessageId(messageId)
                .text(String.format(getText(messages.get("sendContactMessage"), locale), offer.getAgencyName()))
                .build();
        createPermissionMessage(locale, user, result);
        return result;
    }

    private void createPermissionMessage(Locale locale, Optional<BotUser> user, SendMessage result) {
        if (user.isPresent()) {
            result.setReplyMarkup(createRequestContactKeyboard(
                    Pair.of(getText(messages.get("sendContact"), locale) + " (" + user.get().getPhoneNumber() + ")",
                            ButtonType.DEFAULT),
                    Pair.of(getText(messages.get("sendContactEdit"), locale), ButtonType.CONTACT),
                    Pair.of(getText(messages.get("sendContactCancel"), locale), ButtonType.DEFAULT)));
        } else {
            result.setReplyMarkup(createRequestContactKeyboard(
                    Pair.of(getText(messages.get("saveAndSendContact"), locale), ButtonType.CONTACT),
                    Pair.of(getText(messages.get("sendContactCancel"), locale), ButtonType.DEFAULT)));
        }
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
        execute(createDeleteMessage(chatId, lastMessageRepo.findLastMessageId(chatId)));
        store.deleteAll(list);
        lastMessageRepo.deleteLastMessageId(chatId);
        handleMoreOffers(chatId, uuid, request);
    }

    private void handleCalendar(CallbackQuery query) throws TelegramApiException {
        Message message = query.getMessage();
        String chatId = message.getChatId().toString();
        UserData cacheData = userDataRepo.findByChatId(chatId);
        Locale locale = cacheData != null ? cacheData.userLang() : null;
        Integer messageId = message.getMessageId();
        String choice = query.getData();
        if (locale == null || (Objects.requireNonNull(cacheData).currentQuestion() != null
                && !message.getText().equals(getText(cacheData.currentQuestion(), locale)))) {
            execute(createDeleteMessage(chatId, messageId));
        } else if (!choice.equals(IGNORE)) {
            if (!choice.startsWith("<") && !choice.startsWith(">")) {
                execute(createEditMessage(chatId, messageId, choice));
                handleMessage(chatId, choice, null);
            } else {
                try {
                    execute(handleCalendarControls(chatId, locale, messageId, choice));
                } catch (TelegramApiException exception) {
                    if (!exception.getMessage().startsWith("Error editing message reply markup:")) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }

    private void handleMoreOffers(String chatId, String uuid, Request request) throws TelegramApiException {
        int count = offerRepo.countAllByChatIdAndUuidAndBaseMessageIdIsNull(chatId, uuid);
        if (count != 0) {
            if (!lastMessageRepo.containsKey(chatId)) {
                lastMessageRepo.saveLastMessageId(chatId,
                        execute(createLoadMore(chatId, uuid, request.getLang(), count)).getMessageId());
            } else {
                execute(editLoadMore(chatId, uuid, lastMessageRepo.findLastMessageId(chatId),
                        request.getLang(), count));
            }
        } else if (!request.isActive()) {
            lastMessageRepo.deleteLastMessageId(chatId);
            offerCountRepo.deleteOfferCount(chatId);
        }
    }

    private SendMessage createLoadMore(String chatId, String uuid, Locale locale, Integer count) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(String.format(getText(messages.get("loadMoreMessage"), locale), count))
                .replyMarkup(createSingleButtonKeyboard(uuid, getText(messages.get("loadMore"), locale))).build();
    }

    private EditMessageText editLoadMore(String chatId, String uuid, Integer messageId, Locale locale, Integer count) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.format(getText(messages.get("loadMoreMessage"), locale), count))
                .replyMarkup(createSingleButtonKeyboard(uuid, getText(messages.get("loadMore"), locale))).build();
    }

    private void handleMessage(String chatId, String text, User user) throws TelegramApiException {
        if (contactRepo.containsKey(chatId)) {
            handleContactAnswer(chatId, text, user);
            return;
        }
        UserData data = userDataRepo.findByChatId(chatId);
        if (data == null || data.currentQuestion() == null) {
            sendErrorMessage(new NoSuchSessionException(), chatId);
            return;
        }
        Question currentQuestion = data.currentQuestion();
        try {
            Action currentAction = currentQuestion.findNext(text, data.userLang());
            String answer = currentAction.getType() == ActionType.BUTTON ? currentAction.getFieldName() : text;
            if (data.userLang() == null) {
                data.data(new HashMap<>());
                data.userLang(Locale.valueOf(answer));
            }
            data.data().put(currentQuestion.getFieldName(), answer);
            handleNextQuestion(data, chatId, user, currentAction.getNextQuestion());
        } catch (IllegalOptionException | InputMismatchException exception) {
            sendErrorMessage(exception, chatId);
        } catch (JsonProcessingException parseException) {
            parseException.printStackTrace();
        }
    }

    private void handleContactAnswer(String chatId, String text, User user) throws TelegramApiException {
        Integer messageId = contactRepo.findMessageId(chatId);
        Offer offer = offerRepo.getByMessageId(chatId, messageId.toString());
        Request request = requestRepo.findByUuid(offer.getUuid());
        Locale locale = request.getLang();
        switch (extractKey(text, locale)) {
            case "sendContact" -> {
                Optional<BotUser> botUser = userRepo.findById(user.getId());
                botUser.ifPresent(value -> rabbit.convertAndSend(RabbitConfig.ACCEPTED_EXCHANGE, RabbitConfig.ACCEPTED_KEY,
                        mappers.botUserToAcceptedOffer(offer.getUuid(), offer.getAgencyName(), value)));
                sendInfoMessage(offer, locale);
                requestRepo.save(request.setAccepted(true));
            }
            case "sendContactCancel" -> {
                sendPreUserInfo(user, offer, rabbit, mappers);
                sendInfoMessage(offer, locale);
                requestRepo.save(request.setAccepted(true));
            }
            default -> sendErrorMessage(new IllegalOptionException(), chatId);
        }
    }

    private void sendInfoMessage(Offer offer, Locale locale) throws TelegramApiException {
        String chatId = offer.getChatId();
        execute(createCustomMessage(chatId,
                String.format(getText(messages.get("agencyInformed"), locale), offer.getAgencyName())));
        contactRepo.deleteMessageId(chatId);
    }

    private void handleNextQuestion(UserData data, String chatId, User user, Question nextQuestion) throws TelegramApiException, JsonProcessingException {
        if (sendQuestion(data, chatId, nextQuestion)) {
            userDataRepo.saveByChatId(chatId, data.currentQuestion(nextQuestion));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            String uuid = UUID.randomUUID().toString();
            String userData = mapper.writeValueAsString(data.data());
            requestRepo.save(Request.builder()
                    .uuid(uuid)
                    .chatId(chatId)
                    .clientId(user.getId().toString())
                    .data(userData)
                    .lang(extractLocale(userData))
                    .active(true)
                    .accepted(false)
                    .build());
            data.data().put("uuid", uuid);
            logger.info("USER=" + user.getFirstName() + "\n" +
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.data()));
            rabbit.convertAndSend(RabbitConfig.REQUEST_EXCHANGE, RabbitConfig.REQUEST_KEY, data.data());
            userDataRepo.deleteByChatId(chatId);
        }
    }

    private boolean sendQuestion(UserData data, String chatId, Question question) throws TelegramApiException {
        boolean result = true;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(getText(question, data.userLang()))
                .build();
        List<Action> actions = actionRepo.findAllByBaseQuestionOrderById(question);
        if (actions.size() == 0) {
            message.setReplyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
            result = false;
        } else if (actions.get(0).getType().equals(ActionType.DATE)) {
            message.setReplyMarkup(CalendarUtil.createCalendar(LocalDate.now(), data.userLang().getJavaLocale()));
        } else if (actions.get(0).getType().equals(ActionType.BUTTON)) {
            message.setReplyMarkup(createKeyboard(actions, data.userLang()));
        } else {
            message.setReplyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
        }
        execute(message);
        return result;
    }

    private String extractKey(String text, Locale locale) {
        return messages.entrySet().stream()
                .filter(entry -> text.startsWith(getText(entry.getValue(), locale)))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(RuntimeException::new);
    }

    private void sendErrorMessage(Translatable exception, String chatId) throws TelegramApiException {
        Locale locale = userDataRepo.findByChatId(chatId) != null ? userDataRepo.findByChatId(chatId).userLang() : null;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(getText(exception, locale))
                .build();
        execute(message);
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
