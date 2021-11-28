package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.*;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.EmptyConversionResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.model.MessageMedia;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.command.CommandStateService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class VideoEditor extends BaseAny2AnyConverter {

    private static final String TAG = "vedit";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            filter(FormatCategory.VIDEO), List.of(EDIT, PREPARE_VIDEO_EDITING)
    );

    private ConversionMessageBuilder messageBuilder;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    private Jackson jackson;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private EditVideoSettingsWelcomeState welcomeState;

    private CommandStateService commandStateService;

    private FFmpegConversionContextPreparerChain streamProcessor;

    private FFmpegCommandBuilderChain commandBuilderChain;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public VideoEditor(ConversionMessageBuilder messageBuilder,
                       UserService userService, FFprobeDevice fFprobeDevice,
                       FFmpegDevice fFmpegDevice, Jackson jackson,
                       FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                       FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                       CommandStateService commandStateService,
                       FFmpegConversionContextPreparerChainFactory streamProcessorFactory,
                       FFmpegCommandBuilderFactory commandBuilderChainFactory,
                       VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.messageBuilder = messageBuilder;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.jackson = jackson;
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.commandStateService = commandStateService;
        this.videoResultBuilder = videoResultBuilder;

        this.commandBuilderChain = commandBuilderChainFactory.quiteInput();
        commandBuilderChain.setNext(commandBuilderChainFactory.simpleVideoStreamsConversionWithWebmQuality())
                .setNext(commandBuilderChainFactory.videoEditor())
                .setNext(commandBuilderChainFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderChainFactory.output());

        this.streamProcessor = streamProcessorFactory.videoConversionContextPreparer();
        streamProcessor.setNext(streamProcessorFactory.videoEditorContextPreparer());
    }

    @Autowired
    public void setWelcomeState(EditVideoSettingsWelcomeState welcomeState) {
        this.welcomeState = welcomeState;
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        if (conversionQueueItem.getTargetFormat() == PREPARE_VIDEO_EDITING) {
            return super.createDownloads(conversionQueueItem);
        } else {
            EditVideoState editVideoState = commandStateService.getState(conversionQueueItem.getUserId(),
                    ConverterCommandNames.EDIT_VIDEO, true, EditVideoState.class);

            conversionQueueItem.getFirstFile().setFilePath(editVideoState.getDownloadedFilePath());
            getFileDownloadService().createCompletedDownloads(
                    conversionQueueItem.getFiles(), conversionQueueItem.getId(), conversionQueueItem.getUserId(), null
            );

            return conversionQueueItem.getFiles().size() + createThumbDownload(conversionQueueItem);
        }
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        if (fileQueueItem.getTargetFormat() == PREPARE_VIDEO_EDITING) {
            return prepareVideoEditing(fileQueueItem);
        } else {
            return doEdit(fileQueueItem);
        }
    }

    private ConversionResult prepareVideoEditing(ConversionQueueItem fileQueueItem) {
        EditVideoState state = createState(fileQueueItem, userService.getLocaleOrDefault(fileQueueItem.getUserId()));
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());
        state.setDownloadedFilePath(file.getAbsolutePath());

        try {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath(), FormatCategory.VIDEO);
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(file.getAbsolutePath(), videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            state.setCurrentVideoResolution(whd.getHeight());
            state.setCurrentOverallBitrate(videoStreamConversionHelper.getOverallBitrate(allStreams));
            state.setCurrentAudioBitrate(allStreams.stream().filter(s -> s.getCodecType().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE))
                    .map(FFprobeDevice.FFProbeStream::getBitRate).collect(Collectors.toList()));

            FFprobeDevice.FFProbeStream firstVideoStream = allStreams.get(videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            state.setCurrentVideoBitrate(firstVideoStream.getBitRate());
            state.getSettings().setVideoBitrate(state.getCurrentVideoBitrate());
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        }

        welcomeState.enter(fileQueueItem.getUserId(), state);

        return new EmptyConversionResult(false);
    }

    private ConversionResult doEdit(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath(), FormatCategory.VIDEO);

            FFmpegConversionContext conversionContext = FFmpegConversionContext.from(file, result, fileQueueItem.getFirstFileFormat(), allStreams)
                    .putExtra(FFmpegConversionContext.SETTINGS_STATE, settingsState);
            streamProcessor.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);

            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(),
                    videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = callbackHandlerFactory
                    .createCallback(fileQueueItem, srcWhd.getDuration(),
                            userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            fFmpegDevice.execute(command.toCmd(), callback);

            FFprobeDevice.WHD targetWhd = fFprobeDevice.getWHD(result.getAbsolutePath(),
                    videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            String resolutionChangedInfo = messageBuilder.getVideoEditedInfoMessage(fileQueueItem.getSize(),
                    result.length(), srcWhd.getHeight(),
                    targetWhd.getHeight(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));

            return videoResultBuilder.build(fileQueueItem, fileQueueItem.getFirstFileFormat(), resolutionChangedInfo, result);
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private EditVideoState createState(ConversionQueueItem conversionQueueItem, Locale locale) {
        EditVideoState editVideoState = new EditVideoState();
        editVideoState.setState(new ConvertState());
        editVideoState.setStateName(EditVideoSettingsStateName.WELCOME);
        ConvertState convertState = editVideoState.getState();
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());

        List<MessageMedia> files = conversionQueueItem.getFiles().stream().map(MessageMedia::fromTgFile).collect(Collectors.toList());

        files.forEach(convertState::addMedia);

        convertState.setMessageId(conversionQueueItem.getReplyToMessageId());
        convertState.getSettings().setResolution(EditVideoResolutionState.AUTO);
        convertState.getSettings().setCompressBy(EditVideoQualityState.AUTO);
        convertState.getSettings().setAudioCodec(EditVideoAudioCodecState.AUTO);
        convertState.getSettings().setAudioBitrate(EditVideoAudioBitrateState.AUTO);
        convertState.getSettings().setAudioChannelLayout(EditVideoAudioChannelLayoutState.AUTO);

        return editVideoState;
    }
}
