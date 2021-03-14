package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FFmpegVideoStreamsChangeHelper {

    private FFmpegVideoConversionHelper videoConversionHelper;

    private FFmpegHelper fFmpegHelper;

    public FFmpegVideoStreamsChangeHelper(FFmpegVideoConversionHelper videoConversionHelper, FFmpegHelper fFmpegHelper) {
        this.videoConversionHelper = videoConversionHelper;
        this.fFmpegHelper = fFmpegHelper;
    }

    public void prepareCommandForVideoScaling(FFmpegCommandBuilder commandBuilder, SmartTempFile file,
                                              SmartTempFile result, String scale, ConversionQueueItem fileQueueItem) throws InterruptedException {
        List<FFprobeDevice.Stream> allStreams = videoConversionHelper.getStreamsForConversion(file);

        List<FFprobeDevice.Stream> videoStreams = allStreams.stream().filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())).collect(Collectors.toList());
        for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); videoStreamIndex++) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamIndex);
            commandBuilder.mapVideo(videoStreamIndex);
            boolean convertibleToH264 = videoConversionHelper.isConvertibleToH264(file, result, videoStream, videoStreamIndex, scale);
            if (!videoConversionHelper.addFastestVideoCodec(commandBuilder, videoStream, videoStreamIndex,
                    convertibleToH264, scale)) {
                commandBuilder.videoCodec(videoStreamIndex, videoStream.getCodecName());
            }
            if (!convertibleToH264) {
                commandBuilder.filterVideo(videoStreamIndex, scale);
            }
        }
        videoConversionHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getTargetFormat());
        videoConversionHelper.copyOrConvertAudioCodecs(commandBuilder, allStreams, file, result, fileQueueItem);
        fFmpegHelper.copyOrConvertSubtitlesCodecs(commandBuilder, allStreams, file, result, fileQueueItem);
        commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
        commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);
    }
}
