package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.caption.CaptionGenerator;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

@Component
public class VavMergeConverter extends BaseAny2AnyConverter {

    public static final String ADD_AUDIO_MODE = "0";

    public static final String REPLACE_AUDIO_MODE = "1";

    public static final String ADD_SUBTITLES_MODE = "2";

    public static final String REPLACE_SUBTITLES_MODE = "3";

    private static final String TAG = "vavmerge";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.VIDEOAUDIO), List.of(Format.MERGE)
    );

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private Jackson jackson;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper;

    private CaptionGenerator captionGenerator;

    private UserService userService;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    @Autowired
    public VavMergeConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                             Jackson jackson, FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                             FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                             FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper,
                             CaptionGenerator captionGenerator, UserService userService,
                             FFmpegProgressCallbackHandlerFactory callbackHandlerFactory) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.jackson = jackson;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegSubtitlesHelper = fFmpegSubtitlesHelper;
        this.captionGenerator = captionGenerator;
        this.userService = userService;
        this.callbackHandlerFactory = callbackHandlerFactory;
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem conversionQueueItem) {
        TgFile videoFile = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.VIDEO)
                .findAny().orElseThrow();
        SmartTempFile video = conversionQueueItem.getDownloadedFileOrThrow(videoFile.getFileId());

        List<TgFile> audioFiles = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.AUDIO)
                .collect(Collectors.toList());

        List<SmartTempFile> audios = audioFiles.stream()
                .map(a -> conversionQueueItem.getDownloadedFileOrThrow(a.getFileId()))
                .collect(Collectors.toList());

        List<TgFile> subtitlesFiles = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.SUBTITLES)
                .collect(Collectors.toList());

        List<SmartTempFile> subtitles = subtitlesFiles.stream()
                .map(s -> conversionQueueItem.getDownloadedFileOrThrow(s.getFileId()))
                .collect(Collectors.toList());

        Format targetFormat = videoFile.getFormat();
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                conversionQueueItem.getFirstFileId(), TAG, targetFormat.getExt());

        try {
            List<FFprobeDevice.FFProbeStream> videoStreamsForConversion = fFprobeDevice.getAllStreams(video.getAbsolutePath());
            videoStreamsForConversion.forEach(s -> s.setInput(0));
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite().input(video.getAbsolutePath());

            if (!audios.isEmpty()) {
                audios.forEach(a -> commandBuilder.input(a.getAbsolutePath()));
            }
            if (!subtitles.isEmpty()) {
                subtitles.forEach(s -> commandBuilder.input(s.getAbsolutePath()));
            }

            if (targetFormat.canBeSentAsVideo()) {
                fFmpegVideoHelper.copyOrConvertVideoCodecsForTelegramVideo(commandBuilder, result, videoStreamsForConversion,
                        targetFormat, conversionQueueItem.getSize());
            } else {
                fFmpegVideoHelper.copyOrConvertVideoCodecs(commandBuilder, videoStreamsForConversion, targetFormat,
                        result);
            }
            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);
            FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);

            SettingsState settingsState = jackson.convertValue(conversionQueueItem.getExtra(), SettingsState.class);
            if (!audios.isEmpty()) {
                List<FFprobeDevice.FFProbeStream> audioStreamsForConversion = new ArrayList<>();

                for (int index = 0; index < audios.size(); index++) {
                    SmartTempFile audio = audios.get(index);
                    List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(audio.getAbsolutePath());
                    for (FFprobeDevice.FFProbeStream stream : allStreams) {
                        stream.setInput(index + 1);
                    }
                    audioStreamsForConversion.addAll(allStreams);
                }
                audioStreamsForConversion.addAll(ADD_AUDIO_MODE.equals(settingsState.getVavMergeAudioMode()) ? videoStreamsForConversion : List.of());
                if (targetFormat.canBeSentAsVideo()) {
                    videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, audioStreamsForConversion);
                } else {
                    videoAudioConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, audioStreamsForConversion, result, targetFormat);
                }
                if (subtitles.isEmpty()) {
                    fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, videoStreamsForConversion, result, targetFormat);
                }
            } else {
                if (targetFormat.canBeSentAsVideo()) {
                    videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, videoStreamsForConversion);
                } else {
                    videoAudioConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, videoStreamsForConversion, result, targetFormat);
                }
            }
            if (!subtitles.isEmpty()) {
                int subtitlesInput = audios.size() + 1;
                List<FFprobeDevice.FFProbeStream> subtitlesStreams = new ArrayList<>();

                for (SmartTempFile subtitle : subtitles) {
                    List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(subtitle.getAbsolutePath());
                    for (FFprobeDevice.FFProbeStream stream : allStreams) {
                        stream.setInput(subtitlesInput);
                    }
                    subtitlesStreams.addAll(allStreams);
                    ++subtitlesInput;
                }
                subtitlesStreams.addAll(ADD_SUBTITLES_MODE.equals(settingsState.getVavMergeSubtitlesMode()) ? videoStreamsForConversion : List.of());
                fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, subtitlesStreams, result, targetFormat);
            }
            if (WEBM.equals(targetFormat)) {
                commandBuilder.vp8QMinQMax();
            }
            commandBuilder.fastConversion();

            long durationInSeconds = fFprobeDevice.getDurationInSeconds(video.getAbsolutePath());
            commandBuilder.t(durationInSeconds);
            commandBuilder.defaultOptions().out(result.getAbsolutePath());

            FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(conversionQueueItem, durationInSeconds,
                    userService.getLocaleOrDefault(conversionQueueItem.getUserId()));
            fFmpegDevice.execute(commandBuilder.buildFullCommand(), callback);

            String fileName = Any2AnyFileNameUtils.getFileName(videoFile.getFileName(),
                    targetFormat.getExt());
            String caption = captionGenerator.generate(conversionQueueItem.getUserId(), videoFile.getSource());
            if (targetFormat.canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

                return new VideoResult(fileName, result, targetFormat, downloadThumb(conversionQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), targetFormat.supportsStreaming(), caption);
            } else {
                return new FileResult(fileName, result, downloadThumb(conversionQueueItem), caption);
            }
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
