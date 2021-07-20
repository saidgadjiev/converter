package ru.gadjini.telegram.converter.service;

import org.apache.commons.lang3.StringUtils;
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
import ru.gadjini.telegram.smart.bot.commons.common.TgConstants;
import ru.gadjini.telegram.smart.bot.commons.domain.FileSource;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
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

    private RestTemplate restTemplate;

    private FormatService formatService;

    private LocalisationService localisationService;

    private ApplicationProperties applicationProperties;

    private UserService userService;

    @Autowired
    public ConverterMediaUrlExtractor(RestTemplate restTemplate, FormatService formatService,
                                      LocalisationService localisationService, ApplicationProperties applicationProperties,
                                      UserService userService) {
        this.restTemplate = restTemplate;
        this.formatService = formatService;
        this.localisationService = localisationService;
        this.applicationProperties = applicationProperties;
        this.userService = userService;
    }

    @Override
    public MessageMedia extractMedia(long userId, String url) {
        if (applicationProperties.is(FormatsConfiguration.VIDEO_CONVERTER)) {
            Locale locale = userService.getLocaleOrDefault(userId);

            try {
                if (containsUnsupportedMediaSource(url)) {
                    throw new IllegalArgumentException("Unsupported media source");
                }
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
                if (httpHeaders.getContentLength() <= 0
                        || httpHeaders.getContentLength() > TgConstants.LARGE_FILE_SIZE) {
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
                messageMedia.setFileSize(httpHeaders.getContentLength());
                messageMedia.setFormat(mediaFormat);

                return messageMedia;
            } catch (UserException e) {
                throw e;
            } catch (Throwable e) {
                LOGGER.error("Incorrect url({}, {})", url, e.getMessage());
                throw new UserException(localisationService.getMessage(ConverterMessagesProperties.MESSAGE_INCORRECT_MEDIA_LINK, locale));
            }
        }

        return null;
    }

    private boolean containsUnsupportedMediaSource(String url) {
        return url.toLowerCase().contains("youtube") || url.contains("tiktok") || url.contains("instagram");
    }
}
