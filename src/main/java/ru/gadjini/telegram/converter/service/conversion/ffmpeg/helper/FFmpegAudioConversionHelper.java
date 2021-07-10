package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Service
public class FFmpegAudioConversionHelper {

    private static final int COVER_INDEX = 0;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public FFmpegAudioConversionHelper(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    public void convertAudioCodecsForTelegramVoice(FFmpegCommandBuilder commandBuilder, int streamIndex, Format targetFormat) {
        commandBuilder.mapAudio(streamIndex);
        addAudioCodecOptions(commandBuilder, targetFormat);
    }

    public void convertAudioCodecsForTelegramVoice(FFmpegCommandBuilder commandBuilder, Format targetFormat) {
        commandBuilder.mapAudio();
        addAudioCodecOptions(commandBuilder, targetFormat);
    }

    public void copyOrConvertAudioCodecsForTelegramVoice(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> audioStreams) {
        int outCodecIndex = 0;
        for (int audioStreamMapIndex = 0; audioStreamMapIndex < audioStreams.size(); ++audioStreamMapIndex) {
            FFprobeDevice.Stream audioStream = audioStreams.get(audioStreamMapIndex);

            commandBuilder.mapAudio(audioStream.getInput(), audioStreamMapIndex);
            if (FFmpegCommandBuilder.OPUS.equals(audioStream.getCodecName())) {
                commandBuilder.copyAudio(outCodecIndex);
            } else {
                commandBuilder.audioCodec(outCodecIndex, FFmpegCommandBuilder.OPUS);
            }
            ++outCodecIndex;
        }
    }

    public void convertAudioCodecs(FFmpegCommandBuilder commandBuilder, Format targetFormat) {
        commandBuilder.mapAudio();
        addAudioCodecOptions(commandBuilder, targetFormat);
    }

    public void copyOrConvertAudioCodecs(FFmpegCommandBuilder commandBuilder, List<FFprobeDevice.Stream> audioStreams,
                                         Format targetFormat, SmartTempFile result) throws InterruptedException {
        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        int outCodecIndex = 0;
        for (int audioStreamMapIndex = 0; audioStreamMapIndex < audioStreams.size(); ++audioStreamMapIndex) {
            FFprobeDevice.Stream audioStream = audioStreams.get(audioStreamMapIndex);

            commandBuilder.mapAudio(audioStream.getInput(), audioStreamMapIndex);
            if (isCopyableAudioCodecs(baseCommand, result, targetFormat, audioStream.getInput(), audioStreamMapIndex)) {
                commandBuilder.copyAudio(outCodecIndex);
            } else {
                addAudioCodecOptions(commandBuilder, outCodecIndex, targetFormat);
            }
            ++outCodecIndex;
        }
    }

    public void addChannelMapFilter(FFmpegCommandBuilder commandBuilder, SmartTempFile out) throws InterruptedException {
        FFmpegCommandBuilder command = new FFmpegCommandBuilder(commandBuilder);
        command.out(out.getAbsolutePath());

        if (fFmpegDevice.isChannelMapError(command.buildFullCommand())) {
            commandBuilder.filterAudio("channelmap=channel_layout=5.1");
        }
    }

    public void addAudioTargetOptions(FFmpegCommandBuilder commandBuilder, Format target) {
        if (target.getAssociatedFormat() == AMR) {
            commandBuilder.ar("8000").ac("1");
        } else if (target.getAssociatedFormat().canBeSentAsVoice()) {
            commandBuilder.ar("48000");
        }
    }

    private void addAudioCodecOptions(FFmpegCommandBuilder commandBuilder, int streamIndex, Format target) {
        if (target.getAssociatedFormat() == OGG) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommandBuilder.OPUS);
        }
    }

    private void addAudioCodecOptions(FFmpegCommandBuilder commandBuilder, Format target) {
        if (target.getAssociatedFormat().canBeSentAsVoice()) {
            commandBuilder.audioCodec(FFmpegCommandBuilder.OPUS);
        }
    }

    public void addCopyableCoverArtOptions(SmartTempFile in, SmartTempFile out, FFmpegCommandBuilder commandBuilder) throws InterruptedException {
        if (hasCopyableCoverArt(in, out, commandBuilder)) {
            commandBuilder.mapVideo(COVER_INDEX).copyVideo(COVER_INDEX);
        }
    }

    private boolean hasCopyableCoverArt(SmartTempFile in, SmartTempFile out, FFmpegCommandBuilder commandBuilder) throws InterruptedException {
        List<FFprobeDevice.Stream> videoStreams = fFprobeDevice.getVideoStreams(in.getAbsolutePath());
        if (!CollectionUtils.isEmpty(videoStreams)) {
            commandBuilder = new FFmpegCommandBuilder(commandBuilder)
                    .mapVideo(COVER_INDEX)
                    .copyVideo(COVER_INDEX);

            return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
        }

        return false;
    }

    private boolean isCopyableAudioCodecs(FFmpegCommandBuilder baseCommand, SmartTempFile out,
                                          Format targetFormat, Integer input, int streamMapIndex) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder(baseCommand);

        commandBuilder.mapAudio(input, streamMapIndex).copy(FFmpegCommandBuilder.AUDIO_STREAM_SPECIFIER);
        addAudioTargetOptions(commandBuilder, targetFormat);
        commandBuilder.out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

}
