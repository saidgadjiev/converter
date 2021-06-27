package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class FFmpegAudioFormatsConverter extends BaseAudioConverter {

    private static final Map<List<Format>, List<Format>> MAP = new HashMap<>() {{
        put(List.of(AAC), List.of(AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(RM), List.of(AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, AAC));
        put(List.of(AMR), List.of(AAC, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(AIFF), List.of(AMR, AAC, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(FLAC), List.of(AMR, AAC, AIFF, MP3, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(MP3), List.of(AMR, AAC, AIFF, FLAC, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(OGG), List.of(AMR, AAC, AIFF, FLAC, MP3, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(WAV), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(WMA), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WAV, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(OPUS), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, M4A, VOICE, RA, RM));
        put(List.of(SPX), List.of(AMR, AAC, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A, VOICE, RA, RM));
        put(List.of(M4A), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, VOICE, RA, RM));
        put(List.of(M4B), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, SPX, OPUS, M4A, VOICE, RA, RM));
        put(List.of(RA), List.of(AAC, AMR, AIFF, FLAC, MP3, OGG, WAV, WMA, OPUS, SPX, M4A, VOICE, RM));
    }};

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private FFmpegAudioConversionHelper audioConversionHelper;

    @Autowired
    public FFmpegAudioFormatsConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                                       FFmpegAudioConversionHelper audioConversionHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.audioConversionHelper = audioConversionHelper;
    }

    @Override
    public void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem fileQueueItem) {
        try {
            doConvertAudioWithCopy(in, out, fileQueueItem.getTargetFormat());
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        }
    }

    private void doConvertAudioWithCopy(SmartTempFile in, SmartTempFile out, Format targetFormat) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite()
                .input(in.getAbsolutePath());

        List<FFprobeDevice.Stream> audioStreams = fFprobeDevice.getAudioStreams(in.getAbsolutePath());
        audioConversionHelper.addCopyableCoverArtOptions(in, out, commandBuilder);
        if (targetFormat.canBeSentAsVoice()) {
            audioConversionHelper.copyOrConvertAudioCodecsForTelegramVoice(commandBuilder, audioStreams);
        } else {
            audioConversionHelper.copyOrConvertAudioCodecs(commandBuilder, audioStreams, targetFormat, out);
        }
        audioConversionHelper.addAudioTargetOptions(commandBuilder, targetFormat);

        fFmpegDevice.execute(commandBuilder.buildFullCommand());
    }

    void doConvertAudio(SmartTempFile in, SmartTempFile out, Format targetFormat) throws InterruptedException {
        FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite()
                .input(in.getAbsolutePath()).mapAudio();
        audioConversionHelper.addCopyableCoverArtOptions(in, out, commandBuilder);
        if (targetFormat.canBeSentAsVoice()) {
            audioConversionHelper.convertAudioCodecsForTelegramVoice(commandBuilder, targetFormat);
        } else {
            audioConversionHelper.convertAudioCodecs(commandBuilder, targetFormat);
        }
        audioConversionHelper.addAudioTargetOptions(commandBuilder, targetFormat);
        commandBuilder.out(out.getAbsolutePath());

        fFmpegDevice.execute(commandBuilder.buildFullCommand());
    }
}
