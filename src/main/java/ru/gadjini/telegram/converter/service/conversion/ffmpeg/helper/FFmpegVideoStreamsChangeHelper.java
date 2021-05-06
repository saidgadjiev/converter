package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FFmpegVideoStreamsChangeHelper {

    private FFmpegVideoConversionHelper videoConversionHelper;

    private FFmpegSubtitlesHelper fFmpegHelper;

    private FFmpegVideoHelper fFmpegVideoHelper;

    private FFmpegAudioHelper fFmpegAudioHelper;

    @Autowired
    public FFmpegVideoStreamsChangeHelper(FFmpegVideoConversionHelper videoConversionHelper,
                                          FFmpegSubtitlesHelper fFmpegHelper, FFmpegVideoHelper fFmpegVideoHelper,
                                          FFmpegAudioHelper fFmpegAudioHelper) {
        this.videoConversionHelper = videoConversionHelper;
        this.fFmpegHelper = fFmpegHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegAudioHelper = fFmpegAudioHelper;
    }

    public void prepareCommandForVideoScaling(FFmpegCommandBuilder commandBuilder, SmartTempFile file,
                                              SmartTempFile result, String scale, Format targetFormat) throws InterruptedException {
        List<FFprobeDevice.Stream> allStreams = videoConversionHelper.getStreamsForConversion(file);
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream().filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())).collect(Collectors.toList());

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); videoStreamIndex++) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamIndex);
            commandBuilder.mapVideo(videoStreamIndex);
            boolean convertibleToH264 = targetFormat.supportsStreaming()
                    || fFmpegVideoHelper.isConvertibleToH264(baseCommand, result, videoStream.getInput(), videoStreamIndex, scale);
            if (!fFmpegVideoHelper.addFastestVideoCodec(commandBuilder, videoStream, videoStreamIndex,
                    convertibleToH264, scale)) {
                commandBuilder.videoCodec(videoStreamIndex, videoStream.getCodecName());
            }
            if (!convertibleToH264) {
                commandBuilder.filterVideo(videoStreamIndex, scale);
            }
        }
        fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);
        if (targetFormat.supportsStreaming()) {
            fFmpegAudioHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
        } else {
            fFmpegAudioHelper.copyOrConvertAudioCodecs(commandBuilder, allStreams, result, targetFormat);
        }
        fFmpegHelper.copyOrConvertOrIgnoreSubtitlesCodecs(commandBuilder, allStreams, file, result, targetFormat);
        commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
        commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);
    }
}
