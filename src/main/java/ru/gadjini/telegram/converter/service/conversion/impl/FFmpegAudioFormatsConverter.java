package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.smart.bot.commons.exception.ProcessException;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegAudioCompressConverter.class);

    private FFmpegDevice fFmpegDevice;

    private FFmpegAudioConversionHelper audioConversionHelper;

    @Autowired
    public FFmpegAudioFormatsConverter(FFmpegDevice fFmpegDevice, FFmpegAudioConversionHelper audioConversionHelper) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.audioConversionHelper = audioConversionHelper;
    }

    @Override
    public void doConvertAudio(SmartTempFile in, SmartTempFile out, ConversionQueueItem fileQueueItem) {
        try {
            doConvertAudio(in, out, fileQueueItem.getTargetFormat());
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        }
    }

    public void doConvertAudio(SmartTempFile in, SmartTempFile out, Format targetFormat) throws InterruptedException {
        try {
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().mapAudio().copyAudio();
            audioConversionHelper.addCopyableCoverArtOptions(in, out, commandBuilder);
            fFmpegDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
        } catch (ProcessException e) {
            LOGGER.error("Error copy codecs");
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().mapAudio();
            audioConversionHelper.addAudioOptions(targetFormat, commandBuilder);
            audioConversionHelper.addCopyableCoverArtOptions(in, out, commandBuilder);
            fFmpegDevice.convert(in.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());
        }
    }
}
