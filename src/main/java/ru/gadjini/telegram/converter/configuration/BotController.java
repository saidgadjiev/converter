package ru.gadjini.telegram.converter.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.gadjini.telegram.converter.exception.UserException;
import ru.gadjini.telegram.converter.filter.BotFilter;
import ru.gadjini.telegram.converter.model.TgMessage;
import ru.gadjini.telegram.converter.model.bot.api.method.send.HtmlMessage;
import ru.gadjini.telegram.converter.model.bot.api.object.Update;
import ru.gadjini.telegram.converter.service.UserService;
import ru.gadjini.telegram.converter.service.message.MessageService;

@RestController
@RequestMapping("/bot")
public class BotController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotController.class);

    private BotFilter botFilter;

    private MessageService messageService;

    private UserService userService;

    @Autowired
    public BotController(BotFilter botFilter,
                         @Qualifier("messagelimits") MessageService messageService,
                         UserService userService) {
        this.botFilter = botFilter;
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("/health")
    public Mono<ServerResponse> hello() {
        return ServerResponse.ok().build();
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody Update update) {
        try {
            botFilter.doFilter(update);
        } catch (UserException ex) {
            if (ex.isPrintLog()) {
                LOGGER.error(ex.getMessage(), ex);
            }
            messageService.sendMessage(new HtmlMessage(TgMessage.getChatId(update), ex.getHumanMessage()).setReplyToMessageId(ex.getReplyToMessageId()));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            TgMessage tgMessage = TgMessage.from(update);
            messageService.sendErrorMessage(tgMessage.getChatId(), userService.getLocaleOrDefault(tgMessage.getUser().getId()));
        }

        return ResponseEntity.ok().build();
    }
}
