package ru.gadjini.telegram.converter.service.telegram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.gadjini.telegram.converter.exception.DownloadCanceledException;
import ru.gadjini.telegram.converter.exception.botapi.TelegramApiException;
import ru.gadjini.telegram.converter.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.converter.io.SmartTempFile;
import ru.gadjini.telegram.converter.model.ApiResponse;
import ru.gadjini.telegram.converter.model.bot.api.method.IsChatMember;
import ru.gadjini.telegram.converter.model.bot.api.method.send.*;
import ru.gadjini.telegram.converter.model.bot.api.method.updatemessages.*;
import ru.gadjini.telegram.converter.model.bot.api.object.AnswerCallbackQuery;
import ru.gadjini.telegram.converter.model.bot.api.object.GetFile;
import ru.gadjini.telegram.converter.model.bot.api.object.Message;
import ru.gadjini.telegram.converter.model.bot.api.object.Progress;
import ru.gadjini.telegram.converter.property.BotApiProperties;
import ru.gadjini.telegram.converter.utils.MemoryUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class TelegramBotApiService implements TelegramMediaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotApiService.class);

    private final BotApiProperties botApiProperties;

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @Autowired
    public TelegramBotApiService(BotApiProperties botApiProperties, ObjectMapper objectMapper) {
        this.botApiProperties = botApiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();

        LOGGER.debug("Bot api: " + botApiProperties.getEndpoint());
    }


    public Boolean isChatMember(IsChatMember isChatMember) {
        try {
            HttpEntity<IsChatMember> request = new HttpEntity<>(isChatMember);
            String result = restTemplate.postForObject(getUrl(IsChatMember.METHOD), request, String.class);
            try {
                ApiResponse<Boolean> apiResponse = objectMapper.readValue(result, new TypeReference<>() {
                });

                if (apiResponse.getOk()) {
                    return apiResponse.getResult();
                } else {
                    throw new TelegramApiRequestException(String.valueOf(isChatMember.getUserId()), "Error is chat member", apiResponse);
                }
            } catch (IOException e) {
                throw new TelegramApiException("Unable to deserialize response(" + result + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(String.valueOf(isChatMember.getUserId()), e.getMessage(), e);
        }
    }

    public Boolean sendAnswerCallbackQuery(AnswerCallbackQuery answerCallbackQuery) {
        try {
            HttpEntity<AnswerCallbackQuery> request = new HttpEntity<>(answerCallbackQuery);
            String response = restTemplate.postForObject(getUrl(AnswerCallbackQuery.METHOD), request, String.class);
            try {
                ApiResponse<Boolean> result = objectMapper.readValue(response, new TypeReference<>() {
                });

                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(null, "Error answering callback query", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(null, "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiException(e);
        }
    }

    public Message sendMessage(SendMessage sendMessage) {
        try {
            HttpEntity<SendMessage> request = new HttpEntity<>(sendMessage);
            String response = restTemplate.postForObject(getUrl(HtmlMessage.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendMessage.getChatId(), "Error sending message", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendMessage.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendMessage.getChatId(), e.getMessage(), e);
        }
    }

    public void editReplyMarkup(EditMessageReplyMarkup editMessageReplyMarkup) {
        try {
            HttpEntity<EditMessageReplyMarkup> request = new HttpEntity<>(editMessageReplyMarkup);
            String response = restTemplate.postForObject(getUrl(EditMessageReplyMarkup.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (!result.getOk()) {
                    throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Error editing message reply markup", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageReplyMarkup.getChatId(), e.getMessage(), e);
        }
    }

    public void editMessageText(EditMessageText editMessageText) {
        try {
            HttpEntity<EditMessageText> request = new HttpEntity<>(editMessageText);
            String response = restTemplate.postForObject(getUrl(EditMessageText.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (!result.getOk()) {
                    throw new TelegramApiRequestException(editMessageText.getChatId(), "Error editing message text", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageText.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageText.getChatId(), e.getMessage(), e);
        }
    }

    public void editMessageCaption(EditMessageCaption editMessageCaption) {
        try {
            HttpEntity<EditMessageCaption> request = new HttpEntity<>(editMessageCaption);
            String response = restTemplate.postForObject(getUrl(EditMessageCaption.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (!result.getOk()) {
                    throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Error editing message caption", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageCaption.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageCaption.getChatId(), e.getMessage(), e);
        }
    }

    public Boolean deleteMessage(DeleteMessage deleteMessage) {
        try {
            HttpEntity<DeleteMessage> request = new HttpEntity<>(deleteMessage);
            String response = restTemplate.postForObject(getUrl(DeleteMessage.METHOD), request, String.class);
            try {
                ApiResponse<Boolean> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(deleteMessage.getChatId(), "Error deleting message", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(deleteMessage.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(deleteMessage.getChatId(), e.getMessage(), e);
        }
    }


    @Override
    public Message editMessageMedia(EditMessageMedia editMessageMedia) {
        try {
            HttpEntity<EditMessageMedia> request = new HttpEntity<>(editMessageMedia);
            String response = restTemplate.postForObject(getUrl(EditMessageMedia.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(editMessageMedia.getChatId(), "Error editing message media", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(editMessageMedia.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(editMessageMedia.getChatId(), e.getMessage(), e);
        }
    }

    @Override
    public Message sendSticker(SendSticker sendSticker) {
        try {
            HttpEntity<SendSticker> request = new HttpEntity<>(sendSticker);
            String response = restTemplate.postForObject(getUrl(SendSticker.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendSticker.getChatId(), "Error sending sticker", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendSticker.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendSticker.getChatId(), e.getMessage(), e);
        }
    }

    @Override
    public Message sendDocument(SendDocument sendDocument) {
        try {
            HttpEntity<SendDocument> request = new HttpEntity<>(sendDocument);
            String response = restTemplate.postForObject(getUrl(SendDocument.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendDocument.getChatId(), "Error sending document", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendDocument.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendDocument.getChatId(), e.getMessage(), e);
        }
    }

    @Override
    public Message sendVideo(SendVideo sendVideo) {
        try {
            HttpEntity<SendVideo> request = new HttpEntity<>(sendVideo);
            String response = restTemplate.postForObject(getUrl(SendVideo.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendVideo.getChatId(), "Error sending document", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendVideo.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendVideo.getChatId(), e.getMessage(), e);
        }
    }

    @Override
    public Message sendAudio(SendAudio sendAudio) {
        try {
            HttpEntity<SendAudio> request = new HttpEntity<>(sendAudio);
            String response = restTemplate.postForObject(getUrl(SendAudio.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendAudio.getChatId(), "Error sending document", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendAudio.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendAudio.getChatId(), e.getMessage(), e);
        }
    }

    @Override
    public Message sendPhoto(SendPhoto sendPhoto) {
        try {
            HttpEntity<SendPhoto> request = new HttpEntity<>(sendPhoto);
            String response = restTemplate.postForObject(getUrl(SendPhoto.METHOD), request, String.class);
            try {
                ApiResponse<Message> result = objectMapper.readValue(response, new TypeReference<>() {
                });
                if (result.getOk()) {
                    return result.getResult();
                } else {
                    throw new TelegramApiRequestException(sendPhoto.getChatId(), "Error sending photo", result);
                }
            } catch (IOException e) {
                throw new TelegramApiRequestException(sendPhoto.getChatId(), "Unable to deserialize response(" + response + ")\n" + e.getMessage(), e);
            }
        } catch (RestClientException e) {
            throw new TelegramApiRequestException(sendPhoto.getChatId(), e.getMessage(), e);
        }
    }

    @Override
    public void downloadFileByFileId(String fileId, long fileSize, SmartTempFile outputFile) {
        downloadFileByFileId(fileId, fileSize, null, outputFile);
    }

    @Override
    public void downloadFileByFileId(String fileId, long fileSize, Progress progress, SmartTempFile outputFile) {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            LOGGER.debug("Start downloadFileByFileId({})", fileId);

            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            getFile.setFileSize(fileSize);
            getFile.setPath(outputFile.getAbsolutePath());
            getFile.setRemoveParentDirOnCancel(false);
            HttpEntity<GetFile> request = new HttpEntity<>(getFile);
            String result = restTemplate.postForObject(getUrl(GetFile.METHOD), request, String.class);
            try {
                ApiResponse<Void> apiResponse = objectMapper.readValue(result, new TypeReference<>() {
                });

                if (!apiResponse.getOk()) {
                    throw new DownloadCanceledException();
                }
            } catch (IOException e) {
                throw new TelegramApiException("Unable to deserialize response(" + result + ", " + fileId + ")\n" + e.getMessage(), e);
            }

            stopWatch.stop();
            LOGGER.debug("Finish downloadFileByFileId({}, {}, {})", fileId, MemoryUtils.humanReadableByteCount(outputFile.length()), stopWatch.getTime(TimeUnit.SECONDS));
        } catch (RestClientException e) {
            throw new TelegramApiException(e);
        }
    }

    @Override
    public boolean cancelUploading(String filePath) {
        return false;
    }

    @Override
    public boolean cancelDownloading(String fileId) {
        return false;
    }

    @Override
    public void cancelDownloads() {

    }

    private String getUrl(String method) {
        return botApiProperties.getEndpoint() + method;
    }
}
