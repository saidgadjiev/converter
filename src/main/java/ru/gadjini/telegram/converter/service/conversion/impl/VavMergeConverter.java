package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

//ffmpeg -y -i pp.mp4 -i bb.opus -map 0:v:0 -map 1:a:0  -t 245 -preset veryfast -crf 26 bb.mp4
@Component
public class VavMergeConverter extends BaseAny2AnyConverter {

    private static final String TAG = "vavmerge";

    public static final int AUDIO_FILE_INDEX = 1;

    public static final int VIDEO_FILE_INDEX = 0;

    public static final int SUBTITLES_FILE_INDEX = 2;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.VIDEOAUDIO), List.of(Format.MERGE)
    );

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper;

    @Autowired
    public VavMergeConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                             FFmpegAudioStreamInVideoFileConversionHelper videoAudioConversionHelper,
                             FFmpegVideoStreamConversionHelper fFmpegVideoHelper, FFmpegSubtitlesStreamConversionHelper fFmpegSubtitlesHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.videoAudioConversionHelper = videoAudioConversionHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegSubtitlesHelper = fFmpegSubtitlesHelper;
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
        TgFile audioFile = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.AUDIO)
                .findAny().orElse(null);

        SmartTempFile audio = null;
        if (audioFile != null) {
            audio = conversionQueueItem.getDownloadedFileOrThrow(audioFile.getFileId());
        }

        TgFile subtitlesFile = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.SUBTITLES)
                .findAny().orElse(null);

        SmartTempFile subtitles = null;
        if (subtitlesFile != null) {
            subtitles = conversionQueueItem.getDownloadedFileOrThrow(subtitlesFile.getFileId());
        }

        Format targetFormat = videoFile.getFormat();
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                conversionQueueItem.getFirstFileId(), TAG, targetFormat.getExt());

        try {
            List<FFprobeDevice.Stream> videoStreamsForConversion = fFprobeDevice.getAllStreams(video.getAbsolutePath());
            videoStreamsForConversion.forEach(s -> s.setInput(0));
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite().input(video.getAbsolutePath());

            if (audio != null) {
                commandBuilder.input(audio.getAbsolutePath());
            }
            if (subtitles != null) {
                commandBuilder.input(subtitles.getAbsolutePath());
            }

            if (targetFormat.canBeSentAsVideo()) {
                fFmpegVideoHelper.copyOrConvertVideoCodecsForTelegramVideo(commandBuilder, videoStreamsForConversion, targetFormat);
            } else {
                fFmpegVideoHelper.copyOrConvertVideoCodecs(commandBuilder, videoStreamsForConversion, targetFormat, result);
            }
            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);
            FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
            if (audio != null) {
                List<FFprobeDevice.Stream> audioStreamsForConversion = fFprobeDevice.getAllStreams(audio.getAbsolutePath());
                audioStreamsForConversion.forEach(s -> s.setInput(1));
                baseCommand = new FFmpegCommandBuilder(commandBuilder);
                if (targetFormat.canBeSentAsVideo()) {
                    videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, audioStreamsForConversion);
                } else {
                    videoAudioConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, audioStreamsForConversion, result, targetFormat);
                }
                if (subtitles == null) {
                    fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, videoStreamsForConversion, result, targetFormat);
                }
            } else {
                if (targetFormat.canBeSentAsVideo()) {
                    videoAudioConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, videoStreamsForConversion);
                } else {
                    videoAudioConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, videoStreamsForConversion, result, targetFormat);
                }
            }
            if (subtitles != null) {
                int subtitlesInput = audio == null ? 1 : 2;
                List<FFprobeDevice.Stream> subtitlesStreams = fFprobeDevice.getAllStreams(subtitles.getAbsolutePath());
                subtitlesStreams.forEach(f -> f.setInput(subtitlesInput));
                fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, subtitlesStreams, result, targetFormat);
            }
            if (WEBM.equals(targetFormat)) {
                commandBuilder.vp8QualityOptions();
            }
            commandBuilder.fastConversion();

            long durationInSeconds = fFprobeDevice.getDurationInSeconds(video.getAbsolutePath());
            commandBuilder.t(durationInSeconds);
            commandBuilder.defaultOptions().out(result.getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(videoFile.getFileName(),
                    targetFormat.getExt());
            if (targetFormat.canBeSentAsVideo()) {
                FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);

                return new VideoResult(fileName, result, targetFormat, downloadThumb(conversionQueueItem), whd.getWidth(), whd.getHeight(),
                        whd.getDuration(), targetFormat.supportsStreaming());
            } else {
                return new FileResult(fileName, result, downloadThumb(conversionQueueItem));
            }
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
