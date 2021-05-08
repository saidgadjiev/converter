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

    private FFmpegSubtitlesHelper fFmpegHelper;

    private FFmpegAudioHelper fFmpegAudioHelper;

    private FFmpegVideoHelper fFmpegVideoHelper;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegVideoStreamsChangeHelper(FFmpegSubtitlesHelper fFmpegHelper,
                                          FFmpegAudioHelper fFmpegAudioHelper,
                                          FFmpegVideoHelper fFmpegVideoHelper, FFprobeDevice fFprobeDevice) {
        this.fFmpegHelper = fFmpegHelper;
        this.fFmpegAudioHelper = fFmpegAudioHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFprobeDevice = fFprobeDevice;
    }

    public void prepareCommandForVideoScaling(FFmpegCommandBuilder commandBuilder, SmartTempFile file,
                                              SmartTempFile result, String scale, Format targetFormat) throws InterruptedException {
        List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
        List<FFprobeDevice.Stream> videoStreams = allStreams.stream().filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType())).collect(Collectors.toList());

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        int outCodecIndex = 0;
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); videoStreamMapIndex++) {
            FFprobeDevice.Stream videoStream = videoStreams.get(videoStreamMapIndex);
            if (FFmpegVideoConversionHelper.isExtraVideoStream(videoStreams, videoStream)) {
                continue;
            }
            commandBuilder.mapVideo(videoStreamMapIndex);
            boolean convertibleToH264 = targetFormat.supportsStreaming()
                    || fFmpegVideoHelper.isConvertibleToH264(baseCommand, result,
                    videoStream.getInput(), videoStreamMapIndex, scale);
            if (!fFmpegVideoHelper.addFastestVideoCodec(commandBuilder, videoStream, outCodecIndex,
                    convertibleToH264, scale)) {
                commandBuilder.videoCodec(outCodecIndex, videoStream.getCodecName());
            }
            if (!convertibleToH264) {
                commandBuilder.filterVideo(outCodecIndex, scale);
            }
            ++outCodecIndex;
        }
        fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);
        fFmpegHelper.copyOrConvertOrIgnoreSubtitlesCodecs(commandBuilder, allStreams, result, targetFormat);
        if (targetFormat.supportsStreaming()) {
            fFmpegAudioHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams);
        } else {
            fFmpegAudioHelper.copyOrConvertAudioCodecs(commandBuilder, allStreams, result, targetFormat);
        }
        commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
        commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);
    }
}
