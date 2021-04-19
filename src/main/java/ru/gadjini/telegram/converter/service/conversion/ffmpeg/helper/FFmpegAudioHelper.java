package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MTS;

@Component
public class FFmpegAudioHelper {

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioHelper(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> allStreams,
                                         SmartTempFile file, SmartTempFile out, ConversionQueueItem fileQueueItem) throws InterruptedException {
        if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
            FFmpegCommandBuilder baseCommandToCheckCopyable = new FFmpegCommandBuilder(commandBuilder);

            List<FFprobeDevice.Stream> audioStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());
            List<Integer> copyAudiosIndexes = new ArrayList<>();
            commandBuilder.mapAudio();
            for (int audioStreamIndex = 0; audioStreamIndex < audioStreams.size(); ++audioStreamIndex) {
                if (isCopyableAudioCodecs(baseCommandToCheckCopyable, file, out, audioStreamIndex)) {
                    copyAudiosIndexes.add(audioStreamIndex);
                } else {
                    addAudioCodecByTargetFormat(commandBuilder, fileQueueItem.getTargetFormat(), audioStreamIndex);
                }
            }
            if (copyAudiosIndexes.size() == audioStreams.size()) {
                commandBuilder.copyAudio();
            } else {
                copyAudiosIndexes.forEach(commandBuilder::copyAudio);
            }
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommandBuilder baseCommandBuilder, SmartTempFile in, SmartTempFile out,
                                          int streamIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommandBuilder);

        commandBuilder.map(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER, streamIndex).copy(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER);

        return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
    }

    private void addAudioCodecByTargetFormat(FFmpegCommandBuilder commandBuilder, Format target, int streamIndex) {
        if (target == MTS) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.AC3_CODEC);
        }
    }
}
