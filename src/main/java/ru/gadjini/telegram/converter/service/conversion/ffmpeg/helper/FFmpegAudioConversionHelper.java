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

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.AMR;
import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.OGG;

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

    public void addAudioOptions(Format target, FFmpegCommandBuilder commandBuilder) {
        if (target == AMR) {
            commandBuilder.ar("8000").ac("1");
        } else if (target == OGG) {
            commandBuilder.audioCodec(FFmpegCommandBuilder.VORBIS).qa("4");
        }
    }

    public void addCopyableCoverArtOptions(SmartTempFile in, SmartTempFile out, FFmpegCommandBuilder commandBuilder) throws InterruptedException {
        if (hasCopyableCoverArt(in, out, commandBuilder)) {
            commandBuilder.mapVideo(COVER_INDEX).copyVideo(COVER_INDEX);
        }
    }

    public boolean hasCopyableCoverArt(SmartTempFile in, SmartTempFile out, FFmpegCommandBuilder commandBuilder) throws InterruptedException {
        List<FFprobeDevice.Stream> videoStreams = fFprobeDevice.getVideoStreams(in.getAbsolutePath());
        if (!CollectionUtils.isEmpty(videoStreams)) {
            commandBuilder = new FFmpegCommandBuilder(commandBuilder)
                    .mapVideo(COVER_INDEX)
                    .copyVideo(COVER_INDEX);

            return fFmpegDevice.isConvertable(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
        }

        return false;
    }
}
