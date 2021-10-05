package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FFmpegVideoCommandPreparer {

    private FFmpegSubtitlesStreamConversionHelper subtitlesHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper fFmpegAudioHelper;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    @Autowired
    public FFmpegVideoCommandPreparer(FFmpegSubtitlesStreamConversionHelper subtitlesHelper,
                                      FFmpegAudioStreamInVideoFileConversionHelper fFmpegAudioHelper,
                                      FFmpegVideoStreamConversionHelper fFmpegVideoHelper) {
        this.subtitlesHelper = subtitlesHelper;
        this.fFmpegAudioHelper = fFmpegAudioHelper;
        this.fFmpegVideoHelper = fFmpegVideoHelper;
    }

    public void prepareCommandForVideoScaling(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.FFProbeStream> allStreams,
                                              SmartTempFile result, String scale, String audioCodec, String audioCodecName,
                                              Long audioBitrate,
                                              Format targetFormat, int quality) throws InterruptedException {
        List<FFprobeDevice.FFProbeStream> videoStreams = allStreams.stream().filter(s -> FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE.equals(s.getCodecType())).collect(Collectors.toList());

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        int outCodecIndex = 0;
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); videoStreamMapIndex++) {
            FFprobeDevice.FFProbeStream videoStream = videoStreams.get(videoStreamMapIndex);
            if (fFmpegVideoHelper.isExtraVideoStream(videoStreams, videoStream)) {
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
            commandBuilder.keepVideoBitRate(videoStream.getBitRate(), outCodecIndex, quality);

            ++outCodecIndex;
        }
        fFmpegVideoHelper.addVideoTargetFormatOptions(commandBuilder, targetFormat);
        baseCommand = new FFmpegCommandBuilder(commandBuilder);

        if (StringUtils.isNotBlank(audioCodec)) {
            fFmpegAudioHelper.copyOrConvertToTargetAudioCodecs(commandBuilder, allStreams, audioCodec,
                    audioCodecName, audioBitrate, true);
        } else {
            if (targetFormat.supportsStreaming()) {
                fFmpegAudioHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams, audioBitrate);
            } else {
                fFmpegAudioHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, allStreams, audioBitrate, result, targetFormat);
            }
        }
        subtitlesHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder, allStreams, result, targetFormat);
        commandBuilder.fastConversion();
    }
}
