package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.AMR;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.OGG;

@Component
@SuppressWarnings("CPD-START")
public class FFmpegAudioStreamConversionHelper {

    private static final int COVER_INDEX = 0;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public FFmpegAudioStreamConversionHelper(FFprobeDevice fFprobeDevice, FFmpegDevice fFmpegDevice) {
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
    }

    public void copyOrConvertAudioCodecsForTelegramVoice(FFmpegCommand commandBuilder, FFprobeDevice.FFProbeStream audioStream) {
        commandBuilder.mapAudio(audioStream.getInput(), 0);
        if (FFmpegCommand.OPUS_CODEC_NAME.equals(audioStream.getCodecName())) {
            commandBuilder.copyAudio();
        } else {
            commandBuilder.audioCodec(FFmpegCommand.LIBOPUS);
        }
    }

    public void convertAudioCodecsForTelegramVoice(FFmpegCommand commandBuilder) {
        commandBuilder.mapAudio(0).audioCodec(FFmpegCommand.LIBOPUS);
    }

    public void addCopyableCoverArtOptions(SmartTempFile in, SmartTempFile out, FFmpegCommand commandBuilder) throws InterruptedException {
        if (hasCopyableCoverArt(in, out, commandBuilder)) {
            commandBuilder.mapVideo(COVER_INDEX).copyVideo(COVER_INDEX);
        }
    }

    private boolean hasCopyableCoverArt(SmartTempFile in, SmartTempFile out, FFmpegCommand commandBuilder) throws InterruptedException {
        List<FFprobeDevice.FFProbeStream> videoStreams = fFprobeDevice.getVideoStreams(in.getAbsolutePath());
        if (!CollectionUtils.isEmpty(videoStreams)) {
            commandBuilder = new FFmpegCommand(commandBuilder)
                    .mapVideo(COVER_INDEX)
                    .copyVideo(COVER_INDEX);

            return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
        }

        return false;
    }

    public void copyOrConvertAudioCodecs(FFmpegCommand commandBuilder, List<FFprobeDevice.FFProbeStream> audioStreams,
                                         Format targetFormat, SmartTempFile result) throws InterruptedException {
        FFmpegCommand baseCommand = new FFmpegCommand(commandBuilder);
        int outCodecIndex = 0;
        for (int audioStreamMapIndex = 0; audioStreamMapIndex < audioStreams.size(); ++audioStreamMapIndex) {
            FFprobeDevice.FFProbeStream audioStream = audioStreams.get(audioStreamMapIndex);

            commandBuilder.mapAudio(audioStream.getInput(), audioStreamMapIndex);
            if (isCopyableAudioCodecs(baseCommand, result, targetFormat, audioStream.getInput(), audioStreamMapIndex)) {
                commandBuilder.copyAudio(outCodecIndex);
            } else {
                addAudioCodecOptions(commandBuilder, outCodecIndex, targetFormat);
            }
            ++outCodecIndex;
        }
    }

    public void convertAudioCodecs(FFmpegCommand commandBuilder, Format targetFormat) {
        commandBuilder.mapAudio(0);
        addAudioCodecOptions(commandBuilder, targetFormat);
    }

    public void addAudioTargetOptions(FFmpegCommand commandBuilder, Format target) {
        addAudioTargetOptions(commandBuilder, target, true);
    }

    public void addAudioTargetOptions(FFmpegCommand commandBuilder, Format target, boolean appendAr) {
        if (target.getAssociatedFormat() == AMR) {
            if (appendAr) {
                commandBuilder.ar("8000");
            }
            commandBuilder.ac("1");
        } else if (target.getAssociatedFormat().canBeSentAsVoice() && appendAr) {
            commandBuilder.ar("48000");
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommand baseCommand, SmartTempFile out,
                                          Format targetFormat, Integer input, int streamMapIndex) throws InterruptedException {
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommand);

        commandBuilder.mapAudio(input, streamMapIndex).copy(FFmpegCommand.AUDIO_STREAM_SPECIFIER);
        addAudioTargetOptions(commandBuilder, targetFormat);
        commandBuilder.out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.buildFullCommand());
    }

    private void addAudioCodecOptions(FFmpegCommand commandBuilder, int streamIndex, Format target) {
        if (target.getAssociatedFormat() == OGG) {
            commandBuilder.audioCodec(streamIndex, FFmpegCommand.LIBOPUS);
        }
    }

    private void addAudioCodecOptions(FFmpegCommand commandBuilder, Format target) {
        if (target.getAssociatedFormat().canBeSentAsVoice()) {
            commandBuilder.audioCodec(FFmpegCommand.LIBOPUS);
        }
    }
}
