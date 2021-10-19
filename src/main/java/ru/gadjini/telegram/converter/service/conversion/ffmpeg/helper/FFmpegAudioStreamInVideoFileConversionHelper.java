package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("CPD-START")
public class FFmpegAudioStreamInVideoFileConversionHelper {

    public static final String TELEGRAM_VIDEO_AUDIO_CODEC = FFmpegCommand.AAC_CODEC;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioStreamInVideoFileConversionHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public boolean isAudioStreamsValidForTelegramVideo(List<FFprobeDevice.FFProbeStream> allStreams) {
        List<FFprobeDevice.FFProbeStream> audioStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        return audioStreams.stream().allMatch(a -> TELEGRAM_VIDEO_AUDIO_CODEC.equals(a.getCodecName()));
    }

    public void copyOrConvertAudioCodecs(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        List<FFprobeDevice.FFProbeStream> audioStreams = conversionContext.audioStreams();
        if (audioStreams.size() > 0) {
            List<Integer> inputs = audioStreams.stream().map(FFprobeDevice.FFProbeStream::getInput).distinct().collect(Collectors.toList());

            int audioStreamIndex = 0;
            for (Integer input : inputs) {
                command.mapAudioInput(input);

                List<FFprobeDevice.FFProbeStream> byInput = audioStreams.stream()
                        .filter(s -> input.equals(s.getInput()))
                        .collect(Collectors.toList());

                FFmpegCommand baseCommand = new FFmpegCommand(command);
                for (FFprobeDevice.FFProbeStream audioStream : byInput) {
                    boolean copied = false;
                    if (StringUtils.isNotBlank(audioStream.getTargetCodecName())
                            && !Objects.equals(audioStream.getTargetCodecName(), audioStream.getCodecName())) {
                        command.audioCodec(audioStreamIndex, audioStream.getTargetCodecName());
                    } else {
                        if (isCopyableAudioCodecs(baseCommand, conversionContext.output(), input, audioStreamIndex)) {
                            command.copyAudio(audioStreamIndex);
                            copied = true;
                        }
                    }

                    if (!copied) {
                        command.keepVideoBitRate(audioStream.getBitRate(), audioStreamIndex);
                    }
                    ++audioStreamIndex;
                }
            }
        }
    }

    public void addChannelMapFilter(FFmpegCommand commandBuilder, SmartTempFile out) throws InterruptedException {
        FFmpegCommand command = new FFmpegCommand(commandBuilder);
        command.out(out.getAbsolutePath());

        if (fFmpegDevice.isChannelMapError(command.toCmd())) {
            commandBuilder.filterAudio("channelmap=channel_layout=5.1");
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommand baseCommandBuilder, SmartTempFile out, Integer input, int streamIndex) throws InterruptedException {
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommandBuilder);

        commandBuilder.mapAudio(input, streamIndex).copy(FFmpegCommand.AUDIO_STREAM_SPECIFIER);
        commandBuilder.fastConversion().defaultOptions().out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }
}
