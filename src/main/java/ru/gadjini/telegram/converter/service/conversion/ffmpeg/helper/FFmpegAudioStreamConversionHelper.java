package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;

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

            return fFmpegDevice.isExecutable(commandBuilder.toCmd());
        }

        return false;
    }

    public void copyOrConvertAudioCodecs(FFmpegCommand commandBuilder, List<FFprobeDevice.FFProbeStream> audioStreams,
                                         SmartTempFile result) throws InterruptedException {
        FFmpegCommand baseCommand = new FFmpegCommand(commandBuilder);
        int outCodecIndex = 0;
        for (int audioStreamMapIndex = 0; audioStreamMapIndex < audioStreams.size(); ++audioStreamMapIndex) {
            FFprobeDevice.FFProbeStream audioStream = audioStreams.get(audioStreamMapIndex);

            commandBuilder.mapAudio(audioStream.getInput(), audioStreamMapIndex);
            if (isCopyableAudioCodecs(baseCommand, result, audioStream.getInput(), audioStreamMapIndex)) {
                commandBuilder.copyAudio(outCodecIndex);
            }
            ++outCodecIndex;
        }
    }

    private boolean isCopyableAudioCodecs(FFmpegCommand baseCommand, SmartTempFile out,
                                          Integer input, int streamMapIndex) throws InterruptedException {
        FFmpegCommand commandBuilder = new FFmpegCommand(baseCommand);

        commandBuilder.mapAudio(input, streamMapIndex).copy(FFmpegCommand.AUDIO_STREAM_SPECIFIER);
        commandBuilder.out(out.getAbsolutePath());

        return fFmpegDevice.isExecutable(commandBuilder.toCmd());
    }
}
