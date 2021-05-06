package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.WEBM;

//ffmpeg -y -i pp.mp4 -i bb.opus -map 0:v:0 -map 1:a:0  -t 245 -preset veryfast -crf 26 bb.mp4
@Component
public class VavMergeConverter extends BaseAny2AnyConverter {

    private static final String TAG = "vavmerge";

    public static final int AUDIO_FILE_INDEX = 1;

    public static final int VIDEO_FILE_INDEX = 0;

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.VIDEOAUDIO), List.of(Format.VMAKE)
    );

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioHelper fFmpegAudioHelper;

    private FFmpegVideoConversionHelper videoConversionHelper;

    private FFmpegVideoHelper fFmpegVideoHelper;

    private FFmpegSubtitlesHelper fFmpegSubtitlesHelper;

    @Autowired
    public VavMergeConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                             FFmpegAudioHelper fFmpegAudioHelper,
                             FFmpegVideoConversionHelper videoConversionHelper,
                             FFmpegVideoHelper fFmpegVideoHelper, FFmpegSubtitlesHelper fFmpegSubtitlesHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegAudioHelper = fFmpegAudioHelper;
        this.videoConversionHelper = videoConversionHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegSubtitlesHelper = fFmpegSubtitlesHelper;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem conversionQueueItem) {
        SmartTempFile video = conversionQueueItem.getDownloadedFiles().get(VIDEO_FILE_INDEX);
        SmartTempFile audio = conversionQueueItem.getDownloadedFiles().get(AUDIO_FILE_INDEX);

        Format targetFormat = conversionQueueItem.getFiles().get(VIDEO_FILE_INDEX).getFormat();
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                conversionQueueItem.getFirstFileId(), TAG, targetFormat.getExt());

        try {
            List<FFprobeDevice.Stream> videoStreamsForConversion = videoConversionHelper.getStreamsForConversion(video);
            videoStreamsForConversion.forEach(s -> s.setInput(0));
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite().input(video.getAbsolutePath())
                    .input(audio.getAbsolutePath());

            if (targetFormat.canBeSentAsVideo()) {
                fFmpegVideoHelper.copyOrConvertVideoCodecsForTelegramVideo(commandBuilder, videoStreamsForConversion, targetFormat);
            } else {
                fFmpegVideoHelper.copyOrConvertVideoCodecs(commandBuilder, videoStreamsForConversion, targetFormat, result);
            }
            fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);
            if (WEBM.equals(targetFormat)) {
                commandBuilder.crf("10");
            }
            List<FFprobeDevice.Stream> audioStreamsForConversion = fFprobeDevice.getAllStreams(audio.getAbsolutePath());
            audioStreamsForConversion.forEach(s -> s.setInput(1));
            if (targetFormat.canBeSentAsVideo()) {
                fFmpegAudioHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, audioStreamsForConversion);
            } else {
                fFmpegAudioHelper.copyOrConvertAudioCodecs(commandBuilder, audioStreamsForConversion, result, targetFormat);
            }
            fFmpegSubtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(commandBuilder, videoStreamsForConversion, video, result, targetFormat);
            commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
            commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);

            long durationInSeconds = fFprobeDevice.getDurationInSeconds(video.getAbsolutePath());
            commandBuilder.shortest().t(durationInSeconds);
            commandBuilder.out(result.getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(conversionQueueItem.getFiles().get(VIDEO_FILE_INDEX).getFileName(),
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
