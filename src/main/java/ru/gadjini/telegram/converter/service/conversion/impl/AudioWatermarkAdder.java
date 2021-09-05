package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.domain.watermark.audio.AudioWatermark;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.sox.SoxService;
import ru.gadjini.telegram.converter.service.watermark.audio.AudioWatermarkService;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector.GarbageFileCollection;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MP3;

@Component
public class AudioWatermarkAdder extends BaseAudioConverter {

    private static final String TAG = "amark";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.AUDIO), List.of(Format.WATERMARK)
    );

    private static final Format NATIVE_FORMAT = MP3;

    private AudioWatermarkService audioWatermarkService;

    private SoxService soxService;

    private FFmpegAudioFormatsConverter audioFormatsConverter;

    private TempFileGarbageCollector tempFileGarbageCollector;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public AudioWatermarkAdder(AudioWatermarkService audioWatermarkService, SoxService soxService,
                               FFmpegAudioFormatsConverter audioFormatsConverter,
                               TempFileGarbageCollector tempFileGarbageCollector, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.audioWatermarkService = audioWatermarkService;
        this.soxService = soxService;
        this.audioFormatsConverter = audioFormatsConverter;
        this.tempFileGarbageCollector = tempFileGarbageCollector;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        int count = super.createDownloadsWithThumb(conversionQueueItem);
        AudioWatermark watermark = audioWatermarkService.getWatermark(conversionQueueItem.getUserId());
        fileDownloadService().createDownload(watermark.getAudio(), conversionQueueItem.getId(),
                conversionQueueItem.getUserId(), null);

        return count + 1;
    }

    @Override
    protected void doConvertAudio(SmartTempFile in, SmartTempFile out,
                                  ConversionQueueItem fileQueueItem) throws InterruptedException {
        AudioWatermark watermark = audioWatermarkService.getWatermark(fileQueueItem.getUserId());
        GarbageFileCollection garbageFileCollection = tempFileGarbageCollector.getNewCollection();

        try {
            List<SmartTempFile> files = convertToFormatSuitableForSox(watermark, fileQueueItem);
            files = makeChannelsAndSampleRatesTheSame(files);

            garbageFileCollection.addFile(files.get(1));

            Long bitrate = null;
            if (fileQueueItem.getFirstFileFormat() == NATIVE_FORMAT) {
                List<FFprobeDevice.Stream> audioStreams = fFprobeDevice.getAudioStreams(in.getAbsolutePath());
                bitrate = audioStreams.iterator().next().getBitRate();
            }
            SmartTempFile mixResult = mixWithSox(bitrate, files);
            garbageFileCollection.addFile(mixResult);

            audioFormatsConverter.doConvertAudioWithCopy(mixResult, out,
                    fileQueueItem.getFirstFileFormat(), bitrate);
        } finally {
            garbageFileCollection.delete();
        }
    }

    private SmartTempFile mixWithSox(Long bitrate, List<SmartTempFile> files) {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, TAG, MP3.getExt());
        try {
            soxService.mix(files.get(0).getAbsolutePath(), files.get(1).getAbsolutePath(),
                    3, bitrate, result.getAbsolutePath());
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }

        return result;
    }

    private List<SmartTempFile> makeChannelsAndSampleRatesTheSame(List<SmartTempFile> files) throws InterruptedException {
        List<SmartTempFile> result = new ArrayList<>();
        SmartTempFile audio = files.get(0);
        result.add(audio);
        String audioSampleRate = soxService.getSampleRate(audio.getAbsolutePath());
        String audioChannels = soxService.getChannels(audio.getAbsolutePath());

        List<String> options = new ArrayList<>();
        SmartTempFile watermarkFile = files.get(1);
        String watermarkSampleRate = soxService.getSampleRate(watermarkFile.getAbsolutePath());
        if (!Objects.equals(audioSampleRate, watermarkSampleRate)) {
            options.add("-r");
            options.add(audioSampleRate);
        }
        String watermarkChannels = soxService.getChannels(watermarkFile.getAbsolutePath());
        if (!Objects.equals(audioChannels, watermarkChannels)) {
            options.add("-c");
            options.add(audioChannels);
        }
        if (!options.isEmpty()) {
            SmartTempFile r = tempFileService().createTempFile(FileTarget.TEMP, TAG, MP3.getExt());
            try {
                soxService.convert(watermarkFile.getAbsolutePath(), r.getAbsolutePath(), options.toArray(String[]::new));
                result.add(r);
            } catch (Throwable e) {
                tempFileService().delete(r);
                throw new ConvertException(e);
            }
        } else {
            result.add(watermarkFile);
        }

        return result;
    }

    private List<SmartTempFile> convertToFormatSuitableForSox(AudioWatermark watermark, ConversionQueueItem queueItem) {
        SmartTempFile audio = queueItem.getDownloadedFileOrThrow(queueItem.getFirstFileId());
        List<SmartTempFile> deleteOnError = new ArrayList<>();
        try {
            if (queueItem.getFirstFile().getFormat() != MP3) {
                audio = convertToMp3(audio);
                deleteOnError.add(audio);
            }
            SmartTempFile watermarkFile = queueItem.getDownloadedFileOrThrow(watermark.getAudio().getFileId());
            if (watermark.getAudio().getFormat() != MP3) {
                watermarkFile = convertToMp3(watermarkFile);
                deleteOnError.add(watermarkFile);
            }

            return List.of(audio, watermarkFile);
        } catch (Throwable e) {
            deleteOnError.forEach(f -> tempFileService().delete(f));
            throw new ConvertException(e);
        }
    }

    private SmartTempFile convertToMp3(SmartTempFile in) {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, TAG, MP3.getExt());
        try {
            audioFormatsConverter.doConvertAudio(in, result, MP3);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }

        return result;
    }
}
