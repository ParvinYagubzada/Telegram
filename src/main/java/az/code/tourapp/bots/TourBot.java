package az.code.tourapp.bots;

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
import az.code.tourapp.utils.Mappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
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

import static az.code.tourapp.configs.RabbitConfig.*;
import static az.code.tourapp.helpers.BotHelper.*;
import static az.code.tourapp.utils.CalendarUtil.IGNORE;

@Setter
@Builder
@ToString
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "commands")
public class TourBot extends TelegramWebhookBot {

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

    private final Map<Command, Consumer<Update>> commands = new HashMap<>();

    public TourBot init() throws TelegramApiException {
        commands.put(new Command("stop", "Stops bot current interrogation."), this::stop);
        commands.put(new Command("start", "Starts bot interrogation!"), this::interrogate);
        execute(SetWebhook.builder().url(domain + api).dropPendingUpdates(true).build());
        execute(SetMyCommands.builder().commands(new ArrayList<>(commands.keySet())).build());
        return this;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        try {
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
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private void handleTextMessage(Update update) throws TelegramApiException {
        String msg = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        if (msg.startsWith("/")) {
            msg = msg.substring(1);
            Consumer<Update> action;
            if ((action = commands.get(new Command(msg))) != null) {
                action.accept(update);
            } else {
                Request request = requestRepo.findByChatIdAndActiveIsTrue(chatId);
                sendErrorMessage(
                        new CommandNotFoundException(), request != null ? request.getLang() : Locale.DEFAULT, chatId
                );
            }
        } else {
            Message message = update.getMessage();
            if (message.getReplyToMessage() != null && message.getReplyToMessage().hasPhoto()) {
                handleReplyTextMessage(message, message.getReplyToMessage());
            } else {
                handleMessage(chatId, message.getText(), message.getFrom());
            }
        }
    }

    @SneakyThrows
    private void interrogate(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (requestRepo.existsByChatIdAndActiveIsTrue(chatId) || userDataRepo.findByChatId(chatId) != null) {
            handleActiveRequest(chatId, new AlreadyHaveSessionException());
            return;
        }
        Question question = questionRepo.findById(firstQuestionId).orElseThrow(MissingFirstQuestionException::new);
        UserData data = UserData.builder().currentQuestion(question).build();
        userDataRepo.saveByChatId(chatId, data);
        sendQuestion(chatId, Locale.DEFAULT, question);
    }

    @SneakyThrows
    private void stop(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if (!requestRepo.existsByChatIdAndActiveIsTrue(chatId) && userDataRepo.findByChatId(chatId) == null) {
            sendErrorMessage(new NoSuchSessionException(), chatId);
        } else {
            UserData data = userDataRepo.findByChatId(chatId);
            Request request = requestRepo.findByChatIdAndActiveIsTrue(chatId);
            Locale locale = data != null ? data.userLang() : null;
            if (request != null) {
                deleteLoadMoreButton(chatId, request.getUuid());
                deactivateRequestAndClearCache(request);
                locale = request.getLang();
            }
            userDataRepo.deleteByChatId(chatId);
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
                .id(new RequestId(offer.getAgencyName(), uuid))
                .chatId(chatId)
                .photoUrl(fileName).build();
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
            if (offerRepo.existsById_Uuid(uuid)) {
                requestRepo.save(request.setExpirationTime(LocalDateTime.now().plusDays(expirationDays)));
                execute(createCustomMessage(chatId, getText(messages.get("expirationInfoMessage"), request.getLang())));
            } else {
                requestRepo.save(request.setActive(false));
                deleteLoadMoreButton(chatId, uuid);
                execute(createCustomMessage(chatId, getText(messages.get("stopMessage"), request.getLang())));
            }
        }
    }

    private void deleteLoadMoreButton(String chatId, String uuid) throws TelegramApiException {
        Integer messageId = lastMessageRepo.findLastMessageId(chatId);
        if (messageId != null) {
            lastMessageRepo.deleteLastMessageId(chatId);
            List<Offer> offers = offerRepo.findAllByChatIdAndId_UuidAndBaseMessageIdIsNull(chatId, uuid);
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
            Request request = requestRepo.findByUuid(offer.getId().getUuid());
            Locale locale = request.getLang();
            userRepo.save(mappers.contactToBotUser(username, contact));
            rabbit.convertAndSend(ACCEPTED_EXCHANGE, ACCEPTED_KEY, mappers.contactToAcceptedOffer(
                    offer.getId().getUuid(),
                    offer.getId().getAgencyName(),
                    username,
                    contact
            ));
            sendInfoMessage(offer, locale);
        }
    }

    private void handleReplyTextMessage(Message userMessage, Message replyToMessage) throws TelegramApiException {
        String chatId = replyToMessage.getChatId().toString();
        Integer messageId = replyToMessage.getMessageId();
        Offer offer = offerRepo.getByMessageId(chatId, messageId.toString());
        Request request = requestRepo.findByUuid(offer.getId().getUuid());
        if (!isValid(chatId, request)) return;
        Locale locale = request.getLang();
        SendMessage message = createRequestContactMessage(userMessage, messageId, offer, locale);
        offerRepo.save(offer.toBuilder()
                .messageId(execute(message).getMessageId().toString())
                .build());
    }

    private boolean isValid(String chatId, Request request) throws TelegramApiException {
        if (!request.isActive() || request.getExpirationTime() != null &&
                request.getExpirationTime().isBefore(LocalDateTime.now())) {
            sendErrorMessage(new RequestExpiredException(), request.getLang(), chatId);
            return deactivateRequestAndClearCache(request);
        }
        return true;
    }

    private boolean deactivateRequestAndClearCache(Request request) {
        requestRepo.save(request.setActive(false));
        rabbit.convertAndSend(STOP_EXCHANGE, STOP_KEY, request.getUuid());
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
                .text(String.format(getText(messages.get("sendContactMessage"), locale), offer.getId().getAgencyName()))
                .build();
        createPermissionMessage(locale, user, result);
        return result;
    }

    private void createPermissionMessage(Locale locale, Optional<BotUser> user, SendMessage result) {
        if (user.isPresent()) {
            result.setReplyMarkup(createRequestContactKeyboard(
                    Pair.of(getText(messages.get("sendInfoWithPhone"), locale) + " (" + user.get().getPhoneNumber() + ")",
                            ButtonType.DEFAULT),
                    Pair.of(getText(messages.get("sendContactEdit"), locale), ButtonType.CONTACT),
                    Pair.of(getText(messages.get("sendInfoWithoutPhone"), locale), ButtonType.DEFAULT)));
        } else {
            result.setReplyMarkup(createRequestContactKeyboard(
                    Pair.of(getText(messages.get("saveAndSendContact"), locale), ButtonType.CONTACT),
                    Pair.of(getText(messages.get("sendInfoWithoutPhone"), locale), ButtonType.DEFAULT)));
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
                    String messageId = sendOfferPhoto(offer.getPhotoUrl(), chatId, offer.getId().getAgencyName());
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
        if (locale == null || (cacheData.currentQuestion() != null && !message.getText()
                .replaceAll(DATE_REGEX, "%s")
                .equals(getText(cacheData.currentQuestion(), locale))
        )) {
            execute(createDeleteMessage(chatId, messageId));
        } else if (choice.equals(IGNORE)) {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(query.getId())
                    .text(getText(messages.get("calendarNotification"), locale))
                    .build()
            );
        } else {
            if (!choice.startsWith("<") && !choice.startsWith(">")) {
                execute(createEditMessage(chatId, messageId, choice));
                handleMessage(chatId, choice, null);
            } else {
                try {
                    Action action = cacheData.currentQuestion().getActions().get(0);
                    LocalDate start = convertRepresentation(action.getText(), LocalDate.class);
                    LocalDate end = convertRepresentation(action.getTextAz(), LocalDate.class);
                    execute(handleCalendarControls(chatId, locale, messageId, choice, start, end));
                } catch (TelegramApiException exception) {
                    if (!exception.getMessage().startsWith("Error editing message reply markup:")) {
                        log.error(exception.getMessage(), exception);
                    }
                }
            }
        }
    }

    private void handleMoreOffers(String chatId, String uuid, Request request) throws TelegramApiException {
        int count = offerRepo.countAllByChatIdAndId_UuidAndBaseMessageIdIsNull(chatId, uuid);
        if (count != 0) {
            if (!lastMessageRepo.containsKey(chatId)) {
                lastMessageRepo.saveLastMessageId(chatId,
                        sendLoadMoreButton(chatId, uuid, request.getLang(), count).getMessageId());
            } else {
                editLoadMoreButton(chatId, uuid, lastMessageRepo.findLastMessageId(chatId), request.getLang(), count);
            }
        } else if (!request.isActive()) {
            lastMessageRepo.deleteLastMessageId(chatId);
            offerCountRepo.deleteOfferCount(chatId);
        }
    }

    private Message sendLoadMoreButton(String chatId, String uuid, Locale locale, Integer count) throws TelegramApiException {
        return execute(SendMessage.builder()
                .chatId(chatId)
                .text(String.format(getText(messages.get("loadMoreMessage"), locale), count))
                .replyMarkup(createSingleButtonKeyboard(uuid, getText(messages.get("loadMore"), locale))).build()
        );
    }

    private void editLoadMoreButton(String chatId, String uuid, Integer messageId, Locale locale, Integer count) throws TelegramApiException {
        execute(EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.format(getText(messages.get("loadMoreMessage"), locale), count))
                .replyMarkup(createSingleButtonKeyboard(uuid, getText(messages.get("loadMore"), locale))).build()
        );
    }

    private void handleMessage(String chatId, String text, User user) throws TelegramApiException {
        if (contactRepo.containsKey(chatId)) {
            handleContactAnswer(chatId, text, user);
            return;
        }
        UserData data = userDataRepo.findByChatId(chatId);
        if (data == null || data.currentQuestion() == null) {
            handleActiveRequest(chatId, new NoSuchSessionException());
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
        } catch (IllegalOptionException | InputMismatchException | DateMismatchException exception) {
            sendErrorMessage(exception, chatId);
        } catch (JsonProcessingException parseException) {
            log.error(parseException.getMessage(), parseException);
        }
    }

    private void handleContactAnswer(String chatId, String text, User user) throws TelegramApiException {
        Integer messageId = contactRepo.findMessageId(chatId);
        Offer offer = offerRepo.getByMessageId(chatId, messageId.toString());
        Request request = requestRepo.findByUuid(offer.getId().getUuid());
        Locale locale = request.getLang();
        try {
            switch (extractKey(text, locale)) {
                case "sendInfoWithPhone" -> {
                    Optional<BotUser> botUser = userRepo.findById(user.getId());
                    botUser.ifPresent(value -> rabbit.convertAndSend(ACCEPTED_EXCHANGE, ACCEPTED_KEY,
                            mappers.botUserToAcceptedOffer(offer.getId().getUuid(), offer.getId().getAgencyName(), value)));
                    sendInfoMessage(offer, locale);
                }
                case "sendInfoWithoutPhone" -> {
                    checkUsername(user);
                    sendPreUserInfo(user, offer.getId(), rabbit, mappers);
                    sendInfoMessage(offer, locale);
                }
                default -> sendErrorMessage(new IllegalOptionException(), locale, chatId);
            }
        } catch (MissingUsernameException e) {
            sendErrorMessage(e, locale, chatId);
        }
    }

    private void checkUsername(User user) {
        if (user.getUserName() == null) throw new MissingUsernameException();
    }

    private void sendInfoMessage(Offer offer, Locale locale) throws TelegramApiException {
        String chatId = offer.getChatId();
        execute(createCustomMessage(chatId,
                String.format(getText(messages.get("agencyInformed"), locale), offer.getId().getAgencyName())));
        contactRepo.deleteMessageId(chatId);
    }

    private void handleActiveRequest(String chatId, Translatable fallbackException) throws TelegramApiException {
        Request request = requestRepo.findByChatIdAndActiveIsTrue(chatId);
        if (request != null) {
            if (request.getExpirationTime() != null && request.getExpirationTime().isBefore(LocalDateTime.now())) {
                deactivateRequestAndClearCache(request);
                sendErrorMessage(fallbackException, chatId);
            } else {
                String message = getText(offerRepo.existsById_Uuid(request.getUuid()) ?
                        messages.get("acceptanceInfo") : messages.get("pleaseWait"), request.getLang());
                execute(createCustomMessage(chatId, message));
            }
        } else {
            sendErrorMessage(fallbackException, chatId);
        }
    }

    private void handleNextQuestion(UserData data, String chatId, User user, Question nextQuestion) throws TelegramApiException, JsonProcessingException {
        if (sendQuestion(chatId, data.userLang(), nextQuestion)) {
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
                    .build());
            data.data().put("uuid", uuid);
            log.info("USER=" + user.getFirstName() + "\n" +
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.data()));
            rabbit.convertAndSend(REQUEST_EXCHANGE, REQUEST_KEY, data.data());
            userDataRepo.deleteByChatId(chatId);
        }
    }

    private boolean sendQuestion(String chatId, Locale locale, Question question) throws TelegramApiException {
        boolean result = true;
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(getText(question, locale))
                .build();
        List<Action> actions = actionRepo.findAllByBaseQuestionOrderById(question);
        if (actions.size() == 0) {
            message.setReplyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build());
            result = false;
        } else if (actions.get(0).getType() == ActionType.DATE) {
            Action action = actions.get(0);
            configureCalendarMessage(locale, message, action);
        } else if (actions.get(0).getType() == ActionType.BUTTON) {
            message.setReplyMarkup(createKeyboard(actions, locale));
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
                .findFirst().orElse("unknown");
    }

    private void sendErrorMessage(Translatable exception, String chatId) throws TelegramApiException {
        Locale locale = userDataRepo.findByChatId(chatId) != null ? userDataRepo.findByChatId(chatId).userLang() : null;
        sendErrorMessage(exception, locale, chatId);
    }

    private void sendErrorMessage(Translatable exception, Locale locale, String chatId) throws TelegramApiException {
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
