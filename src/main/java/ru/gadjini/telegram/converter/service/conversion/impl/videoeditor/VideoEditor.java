package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.*;
import ru.gadjini.telegram.converter.command.keyboard.start.ConvertState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.common.ConverterCommandNames;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.MessageResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.impl.extraction.ExtractionByLanguageState;
import ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.state.StandardVideoEditorState;
import ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.state.VideoEditorState;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.queue.ConversionMessageBuilder;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class VideoEditor extends BaseAny2AnyConverter {

    private static final String TAG = "vedit";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(EDIT, PREPARE_VIDEO_EDITING)
    );

    private ConversionMessageBuilder messageBuilder;

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    private UserService userService;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    private Jackson jackson;

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private CaptionGenerator captionGenerator;

    private StandardVideoEditorState standardVideoEditorState;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private Set<EditVideoSettingsState> editVideoSettingsStateSet;

    private CommandStateService commandStateService;

    @Autowired
    public VideoEditor(ConversionMessageBuilder messageBuilder,
                       FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper,
                       UserService userService, FFprobeDevice fFprobeDevice,
                       FFmpegDevice fFmpegDevice, Jackson jackson,
                       FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                       CaptionGenerator captionGenerator, StandardVideoEditorState standardVideoEditorState,
                       FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,CommandStateService commandStateService) {
        super(MAP);
        this.messageBuilder = messageBuilder;
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
        this.userService = userService;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.jackson = jackson;
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.captionGenerator = captionGenerator;
        this.standardVideoEditorState = standardVideoEditorState;
        this.callbackHandlerFactory = callbackHandlerFactory;
        this.commandStateService = commandStateService;
    }

    @Autowired
    public void setEditVideoSettingsStateSet(Set<EditVideoSettingsState> editVideoSettingsStateSet) {
        this.editVideoSettingsStateSet = editVideoSettingsStateSet;
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
            ExtractionByLanguageState audioExtractionState = commandStateService.getState(conversionQueueItem.getUserId(),
                    ConverterCommandNames.EXTRACT_MEDIA_BY_LANGUAGE, true, ExtractionByLanguageState.class);

            conversionQueueItem.getFirstFile().setFilePath(audioExtractionState.getFilePath());
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
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(file.getAbsolutePath(), videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            state.setCurrentVideoResolution(whd.getHeight());
            FFprobeDevice.FFProbeStream firstVideoStream = allStreams.get(videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));
            state.setCurrentVideoBitrate(firstVideoStream.getBitRate());
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        }

        commandStateService.setState(fileQueueItem.getUserId(), ConverterCommandNames.EDIT_VIDEO, state);
        return new MessageResult(getState(state.getStateName()).enter(fileQueueItem.getUserId(), state), false);
    }

    private ConversionResult doEdit(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);

            int quality = settingsState.getQuality();
            settingsState.setCrf(String.valueOf(100 - quality));

            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(),
                    videoStreamConversionHelper.getFirstVideoStreamIndex(allStreams));

            String height = settingsState.getResolution().replace("p", "");
            String scale = EditVideoResolutionState.AUTO.equals(settingsState.getResolution()) ? null
                    : "scale=-2:" + (NumberUtils.isDigits(height) ? height : "ceil(ih" + height + "/2)*2");

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
            commandBuilder.hideBanner().quite().input(file.getAbsolutePath());

            String preferableAudioCodec = EditVideoAudioCodecState.AUTO.equalsIgnoreCase(settingsState.getAudioCodec())
                    ? null
                    : getAudioCodec(settingsState.getAudioCodec());
            String preferableAudioCodecName = StringUtils.isNotBlank(preferableAudioCodec) ? settingsState.getAudioCodec() : null;
            Long preferableAudioBitrate = EditVideoAudioBitrateState.AUTO.equalsIgnoreCase(settingsState.getAudioBitrate())
                    ? null
                    : Long.parseLong(settingsState.getAudioBitrate());

            AtomicReference<VideoEditorState> videoEditorStateAtomicReference = new AtomicReference<>(standardVideoEditorState);

            if (StringUtils.isNotBlank(scale)) {
                videoEditorStateAtomicReference.get().scale(scale, videoEditorStateAtomicReference);
            }
            if (StringUtils.isNotBlank(preferableAudioCodec)) {
                videoEditorStateAtomicReference.get().audioCodec(preferableAudioCodec, videoEditorStateAtomicReference);
            }
            videoEditorStateAtomicReference.get().prepareCommand(commandBuilder, fileQueueItem, allStreams, settingsState,
                    scale, preferableAudioCodec, preferableAudioCodecName, preferableAudioBitrate, result);

            if (fileQueueItem.getFirstFileFormat() == WEBM) {
                commandBuilder.vp8QMinQMax();
            }

            String monoStereo = EditVideoAudioChannelLayoutState.AUTO.equalsIgnoreCase(settingsState.getAudioChannelLayout())
                    ? null
                    : EditVideoAudioChannelLayoutState.MONO.equalsIgnoreCase(settingsState.getAudioChannelLayout())
                    ? "1"
                    : "2";

            if (StringUtils.isNotBlank(monoStereo) && !commandBuilder.hasAc()) {
                commandBuilder.ac(monoStereo);
            }

            audioStreamInVideoFileConversionHelper.addChannelMapFilter(commandBuilder, result);
            commandBuilder.defaultOptions().out(result.getAbsolutePath());
            FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(fileQueueItem, srcWhd.getDuration(),
                    userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            fFmpegDevice.execute(commandBuilder.buildFullCommand(), callback);

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            FFprobeDevice.WHD targetWhd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
            String resolutionChangedInfo = messageBuilder.getVideoEditedInfoMessage(fileQueueItem.getSize(),
                    result.length(), srcWhd.getHeight(),
                    targetWhd.getHeight(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));

            String caption = captionGenerator.generate(fileQueueItem.getUserId(), fileQueueItem.getFirstFile().getSource(), resolutionChangedInfo);
            if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem),
                        targetWhd.getWidth(), targetWhd.getHeight(),
                        targetWhd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming(), caption);
            } else {
                return new FileResult(fileName, result, downloadThumb(fileQueueItem), caption);
            }
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private String getAudioCodec(String audioCodec) {
        switch (audioCodec) {
            case "aac":
                return FFmpegCommandBuilder.AAC_CODEC;
            case "opus":
                return FFmpegCommandBuilder.OPUS;
            case "vorbis":
                return FFmpegCommandBuilder.LIBVORBIS;
            default:
                throw new UnsupportedOperationException("Not implemented for " + audioCodec);
        }
    }

    private EditVideoState createState(ConversionQueueItem conversionQueueItem, Locale locale) {
        EditVideoState editVideoState = new EditVideoState();
        editVideoState.setState(new ConvertState());
        editVideoState.setStateName(EditVideoSettingsStateName.WELCOME);
        ConvertState convertState = editVideoState.getState();
        convertState.setUserLanguage(locale.getLanguage());
        convertState.setSettings(new SettingsState());

        MessageMedia messageMedia = new MessageMedia();
        messageMedia.setFormat(conversionQueueItem.getFirstFileFormat());
        messageMedia.setFileSize(conversionQueueItem.getSize());
        convertState.setMedia(messageMedia);

        convertState.getSettings().setResolution(EditVideoResolutionState.AUTO);
        convertState.getSettings().setCrf(EditVideoQualityState.DEFAULT_QUALITY);
        convertState.getSettings().setAudioCodec(EditVideoAudioCodecState.AUTO);
        convertState.getSettings().setAudioBitrate(EditVideoAudioBitrateState.AUTO);
        convertState.getSettings().setAudioChannelLayout(EditVideoAudioChannelLayoutState.AUTO);

        return editVideoState;
    }

    private EditVideoSettingsState getState(EditVideoSettingsStateName settingsStateName) {
        return editVideoSettingsStateSet.stream().filter(s -> settingsStateName.equals(s.getName())).findFirst().orElseThrow();
    }
}
