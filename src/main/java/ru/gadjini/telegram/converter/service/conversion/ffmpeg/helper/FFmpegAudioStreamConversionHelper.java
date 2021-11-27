package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;
import java.util.Objects;

@Component
public class FFmpegAudioStreamConversionHelper {

    private static final int COVER_INDEX = 0;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioStreamConversionHelper(FFprobeDevice fFprobeDevice, FFmpegDevice fFmpegDevice) {
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
    }

    public void addCopyableCoverArtOptions(SmartTempFile in, FFmpegCommand commandBuilder) throws InterruptedException {
        if (hasCopyableCoverArt(in, commandBuilder)) {
            commandBuilder.mapVideo(COVER_INDEX).copyVideo(COVER_INDEX);
        }
    }

    private boolean hasCopyableCoverArt(SmartTempFile in, FFmpegCommand commandBuilder) throws InterruptedException {
        List<FFprobeDevice.FFProbeStream> videoStreams = fFprobeDevice.getVideoStreams(in.getAbsolutePath());
        if (!CollectionUtils.isEmpty(videoStreams)) {
            commandBuilder = new FFmpegCommand(commandBuilder)
                    .mapVideo(COVER_INDEX)
                    .copyVideo(COVER_INDEX);

            return fFmpegDevice.isExecutable(commandBuilder.toCmd());
        }

        return false;
    }

    public void copyOrConvertAudioCodecs(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        FFmpegCommand baseCommand = new FFmpegCommand(command);
        int audioStreamIndex = 0;
        List<FFprobeDevice.FFProbeStream> audioStreams = conversionContext.audioStreams();
        command.mapAudio();
        for (FFprobeDevice.FFProbeStream audioStream : audioStreams) {
            boolean copied = false;
            if (StringUtils.isNotBlank(audioStream.getTargetCodecName())
                    && !Objects.equals(audioStream.getTargetCodecName(), audioStream.getCodecName())) {
                command.audioCodec(audioStreamIndex, audioStream.getTargetCodecName());
            } else {
                if ((audioStream.getTargetBitrate() == null ||
                        Objects.equals(audioStream.getBitRate(), audioStream.getTargetBitrate()))
                        && isCopyableAudioCodecs(baseCommand, conversionContext.output(), audioStreamIndex)) {
                    command.copyAudio(audioStreamIndex);
                    copied = true;
                }
            }

            if (!copied) {
                command.keepAudioBitRate(audioStreamIndex, audioStream.getTargetBitrate() != null ? audioStream.getTargetBitrate()
                        : audioStream.getBitRate());
            }
            ++audioStreamIndex;
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommand baseCommand, SmartTempFile out, int streamMapIndex) throws InterruptedException {
        if (baseCommand.hasAudioFilter()) {
            return false;
        }
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommand);

        commandBuilder.mapAudio(streamMapIndex).copy(FFmpegCommand.AUDIO_STREAM_SPECIFIER);
        commandBuilder.out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }
}
