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

    public void addCoverArtOptions(SmartTempFile in, SmartTempFile out, FFmpegCommand commandBuilder) throws InterruptedException {
        List<FFprobeDevice.FFProbeStream> videoStreams = fFprobeDevice.getVideoStreams(in.getAbsolutePath());
        if (!CollectionUtils.isEmpty(videoStreams)) {
            if (isCopyableCoverArt(out, commandBuilder)) {
                commandBuilder.mapVideo(COVER_INDEX).copyVideo(COVER_INDEX);
            } else if (isConvertableCoverArt(out, commandBuilder)) {
                commandBuilder.mapVideo(COVER_INDEX);
            }
        }
    }

    private boolean isCopyableCoverArt(SmartTempFile out, FFmpegCommand commandBuilder) throws InterruptedException {
        commandBuilder = new FFmpegCommand(commandBuilder)
                .mapVideo(COVER_INDEX)
                .copyVideo(COVER_INDEX)
                .out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }

    private boolean isConvertableCoverArt(SmartTempFile out, FFmpegCommand commandBuilder) throws InterruptedException {
        commandBuilder = new FFmpegCommand(commandBuilder)
                .mapVideo(COVER_INDEX)
                .out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }

    public void copyOrConvertAudioCodecs(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        FFmpegCommand baseCommand = new FFmpegCommand(command);
        int audioStreamIndex = 0;
        List<FFprobeDevice.FFProbeStream> audioStreams = conversionContext.audioStreams();
        for (int audioStreamMapIndex = 0; audioStreamMapIndex < audioStreams.size(); ++audioStreamMapIndex) {
            boolean copied = false;
            command.mapAudio(audioStreamMapIndex);
            FFprobeDevice.FFProbeStream audioStream = audioStreams.get(audioStreamMapIndex);
            if (StringUtils.isNotBlank(audioStream.getTargetCodecName())
                    && !Objects.equals(audioStream.getTargetCodecName(), audioStream.getCodecName())) {
                command.audioCodec(audioStreamIndex, audioStream.getTargetCodec());
            } else {
                if ((audioStream.getTargetBitrate() == null ||
                        Objects.equals(audioStream.getBitRate(), audioStream.getTargetBitrate()))
                        && !conversionContext.isUseStaticAudioFilter()
                        && !audioStream.isDontCopy()
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
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommand);

        commandBuilder.mapAudio(streamMapIndex).copy(FFmpegCommand.AUDIO_STREAM_SPECIFIER);
        commandBuilder.out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }
}
