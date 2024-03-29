package ru.gadjini.telegram.converter.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.configuration.FormatsConfiguration;
import ru.gadjini.telegram.converter.property.ApplicationProperties;
import ru.gadjini.telegram.smart.bot.commons.common.MessagesProperties;
import ru.gadjini.telegram.smart.bot.commons.domain.FileSource;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.property.BotProperties;
import ru.gadjini.telegram.smart.bot.commons.property.DownloadUploadFileLimitProperties;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UrlMediaExtractor;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.utils.MemoryUtils;
import ru.gadjini.telegram.smart.bot.commons.utils.MimeTypeUtils;

import java.util.Locale;

@Component
public class ConverterMediaUrlExtractor implements UrlMediaExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterMediaUrlExtractor.class);

    private static final long MAX_REMOTE_VIDEO_SIZE = 4000 * 1024 * 1024L;

    private RestTemplate restTemplate;

    private BotProperties botProperties;

    private FormatService formatService;

    private LocalisationService localisationService;

    private ApplicationProperties applicationProperties;

    private UserService userService;

    private DownloadUploadFileLimitProperties mediaLimitProperties;

    @Autowired
    public ConverterMediaUrlExtractor(RestTemplate restTemplate, BotProperties botProperties, FormatService formatService,
                                      LocalisationService localisationService, ApplicationProperties applicationProperties,
                                      UserService userService, DownloadUploadFileLimitProperties mediaLimitProperties) {
        this.restTemplate = restTemplate;
        this.botProperties = botProperties;
        this.formatService = formatService;
        this.localisationService = localisationService;
        this.applicationProperties = applicationProperties;
        this.userService = userService;
        this.mediaLimitProperties = mediaLimitProperties;
    }

    @Override
    public MessageMedia extractMedia(long userId, String url) {
        if (applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER) && !isLinkToMe(url)) {
            Locale locale = userService.getLocaleOrDefault(userId);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try {
                if (containsUnsupportedMediaSource(url)) {
                    throw new IllegalArgumentException("Unsupported media source");
                }
                LOGGER.debug("Start extract media from url({}, {})", userId, url);
                HttpHeaders httpHeaders = restTemplate.headForHeaders(url);
                if (httpHeaders.getContentType() == null || StringUtils.isBlank(httpHeaders.getContentType().getType())) {
                    throw new IllegalArgumentException("Empty content type");
                }
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                String mimeType = MimeTypeUtils.removeCharset(httpHeaders.getContentType().toString());
                Format mediaFormat = formatService.getFormat(fileName, mimeType);
                if (mediaFormat == null) {
                    throw new IllegalArgumentException("Unknown media type " + mimeType);
                }
                if (mediaFormat.getCategory() != FormatCategory.VIDEO) {
                    throw new IllegalArgumentException("Not video " + mediaFormat.name());
                }
                if (httpHeaders.getContentLength() > MAX_REMOTE_VIDEO_SIZE) {
                    throw new IllegalArgumentException("Too big " + MemoryUtils.humanReadableByteCount(httpHeaders.getContentLength()));
                }
                if (StringUtils.isBlank(fileName)) {
                    fileName = localisationService.getMessage(MessagesProperties.MESSAGE_EMPTY_FILE_NAME, locale) + "." + mediaFormat.getExt();
                }

                MessageMedia messageMedia = new MessageMedia();
                messageMedia.setFileName(fileName);
                messageMedia.setFileId(url);
                messageMedia.setSource(FileSource.URL);
                messageMedia.setMimeType(mimeType);
                messageMedia.setFileSize(httpHeaders.getContentLength() <= 0 ? mediaLimitProperties.getLightFileMaxWeight() + 1
                        : httpHeaders.getContentLength());
                messageMedia.setFormat(mediaFormat);

                return messageMedia;
            } catch (UserException e) {
                throw e;
            } catch (Throwable e) {
                LOGGER.error("Incorrect url({}, {})", url, e.getMessage());
                if (e.getMessage().equals("Unsupported media source")) {
                    throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_UNSUPPORTED_MEDIA_SOURCE, locale));
                } else if (e.getMessage().startsWith("Too big")) {
                    throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_TOO_BIG_REMOTE_VIDEO,
                            new Object[]{MemoryUtils.humanReadableByteCount(MAX_REMOTE_VIDEO_SIZE)}, locale));
                } else {
                    throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_MEDIA_LINK, locale));
                }
            } finally {
                stopWatch.stop();
                LOGGER.debug("Handle url latency({}, {}, {})", userId, stopWatch.getTime(), url);
            }
        }

        return null;
    }

    private boolean containsUnsupportedMediaSource(String url) {
        url = url.toLowerCase();

        return url.contains("youtube") || url.contains("tiktok") || url.contains("instagram") || url.contains("youtu.be");
    }

    private boolean isLinkToMe(String url) {
        return url.contains("t.me/" + botProperties.getName());
    }
}
