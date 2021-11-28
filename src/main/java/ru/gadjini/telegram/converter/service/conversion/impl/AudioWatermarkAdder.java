package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.domain.watermark.audio.AudioWatermark;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.sox.SoxService;
import ru.gadjini.telegram.converter.service.watermark.audio.AudioWatermarkService;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.TempFileGarbageCollector.GarbageFileCollection;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MP3;

@Component
public class AudioWatermarkAdder extends BaseAudioConverter {

    private static final String TAG = "amark";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.AUDIO), List.of(Format.WATERMARK)
    );

    private AudioWatermarkService audioWatermarkService;

    private SoxService soxService;

    private FFmpegAudioFormatsConverter audioFormatsConverter;

    private TempFileGarbageCollector tempFileGarbageCollector;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public AudioWatermarkAdder(AudioWatermarkService audioWatermarkService, SoxService soxService,
                               FFmpegAudioFormatsConverter audioFormatsConverter,
                               TempFileGarbageCollector tempFileGarbageCollector, FFprobeDevice fFprobeDevice, FFmpegDevice fFmpegDevice) {
        super(MAP);
        this.audioWatermarkService = audioWatermarkService;
        this.soxService = soxService;
        this.audioFormatsConverter = audioFormatsConverter;
        this.tempFileGarbageCollector = tempFileGarbageCollector;
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
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
                                  ConversionQueueItem fileQueueItem, Format targetFormat) throws InterruptedException {
        AudioWatermark watermark = audioWatermarkService.getWatermark(fileQueueItem.getUserId());
        GarbageFileCollection finallyGarbageFileCollection = tempFileGarbageCollector.getNewCollection();

        try {
            List<FFprobeDevice.FFProbeStream> audioStreams = fFprobeDevice.getAudioStreams(in.getAbsolutePath(), FormatCategory.AUDIO);
            Integer bitrate = audioStreams.iterator().next().getBitRate();

            SmartTempFile audio = makeWatermarkPartVolumeLower(watermark, fileQueueItem, bitrate, finallyGarbageFileCollection);
            List<SmartTempFile> files = convertToFormatSuitableForSox(watermark, audio, fileQueueItem, finallyGarbageFileCollection);

            files = makeChannelsAndSampleRatesTheSame(files, finallyGarbageFileCollection);
            SmartTempFile mixResult = mixWithSox(bitrate, files, finallyGarbageFileCollection);

            audioFormatsConverter.doConvert(mixResult, out,
                    fileQueueItem.getFirstFileFormat());
        } finally {
            finallyGarbageFileCollection.delete();
        }
    }

    private SmartTempFile mixWithSox(Integer bitrate, List<SmartTempFile> files,
                                     GarbageFileCollection garbageFileCollection) throws InterruptedException {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, TAG, MP3.getExt());
        garbageFileCollection.addFile(result);

        soxService.mix(files.get(0).getAbsolutePath(), files.get(1).getAbsolutePath(),
                3, bitrate, result.getAbsolutePath());

        return result;
    }

    private List<SmartTempFile> makeChannelsAndSampleRatesTheSame(List<SmartTempFile> files,
                                                                  GarbageFileCollection garbageFileCollection) throws InterruptedException {
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
            garbageFileCollection.addFile(r);

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

    private List<SmartTempFile> convertToFormatSuitableForSox(AudioWatermark watermark, SmartTempFile audio,
                                                              ConversionQueueItem queueItem, GarbageFileCollection garbageFileCollection) {

        if (queueItem.getFirstFile().getFormat() != MP3) {
            audio = convertToMp3(audio);
            garbageFileCollection.addFile(audio);
        }
        SmartTempFile watermarkFile = queueItem.getDownloadedFileOrThrow(watermark.getAudio().getFileId());
        if (watermark.getAudio().getFormat() != MP3) {
            watermarkFile = convertToMp3(watermarkFile);
            garbageFileCollection.addFile(watermarkFile);
        }

        return List.of(audio, watermarkFile);
    }

    private SmartTempFile convertToMp3(SmartTempFile in) {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, TAG, MP3.getExt());
        try {
            audioFormatsConverter.doConvert(in, result, MP3);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }

        return result;
    }

    private SmartTempFile makeWatermarkPartVolumeLower(AudioWatermark watermark, ConversionQueueItem queueItem,
                                                       Integer bitrate, GarbageFileCollection garbageFileCollection) throws InterruptedException {
        SmartTempFile in = queueItem.getDownloadedFileOrThrow(queueItem.getFirstFileId());

        SmartTempFile watermarkFile = queueItem.getDownloadedFileOrThrow(watermark.getAudio().getFileId());
        long watermarkDuration = fFprobeDevice.getDurationInSeconds(watermarkFile.getAbsolutePath());

        List<File> files = split(in, queueItem, watermarkDuration, garbageFileCollection);
        SmartTempFile volumeDecreased = decreaseVolume(files.get(1), queueItem, bitrate, garbageFileCollection);
        files.set(1, volumeDecreased.getFile());

        SmartTempFile filesList = createFilesListFile(queueItem.getUserId(), files);
        garbageFileCollection.addFile(filesList);

        return mergeAudioFiles(filesList, queueItem, garbageFileCollection);
    }

    private String filesListFileStr(String filePath) {
        return "file '" + filePath + "'";
    }

    private SmartTempFile mergeAudioFiles(SmartTempFile filesList, ConversionQueueItem fileQueueItem,
                                          GarbageFileCollection garbageFileCollection) throws InterruptedException {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        garbageFileCollection.addFile(result);

        FFmpegCommand mergeCommandBuilder = new FFmpegCommand().hideBanner().quite()
                .f(FFmpegCommand.CONCAT).safe("0").input(filesList.getAbsolutePath())
                .mapAudio(0).copyAudio();

        mergeCommandBuilder.out(result.getAbsolutePath());

        fFmpegDevice.execute(mergeCommandBuilder.toCmd());

        return result;
    }

    private SmartTempFile decreaseVolume(File in, ConversionQueueItem fileQueueItem, Integer bitrate,
                                         GarbageFileCollection garbageFileCollection) throws InterruptedException {
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        garbageFileCollection.addFile(result);

        FFmpegCommand commandBuilder = new FFmpegCommand().hideBanner().quite().input(in.getAbsolutePath());

        commandBuilder.filterAudio("volume=0.1");
        commandBuilder.keepAudioBitRate(bitrate);
        if (fileQueueItem.getFirstFileFormat().canBeSentAsVoice()) {
            commandBuilder.audioCodec(FFmpegCommand.OPUS_CODEC_NAME);
        }
        commandBuilder.strict("-2");
        commandBuilder.out(result.getAbsolutePath());

        fFmpegDevice.execute(commandBuilder.toCmd());

        return result;
    }

    private List<File> split(SmartTempFile in, ConversionQueueItem queueItem, long watermarkDuration,
                             GarbageFileCollection garbageFileCollection) {
        FFmpegCommand commandBuilder = new FFmpegCommand().hideBanner().quite().input(in.getAbsolutePath());

        SmartTempFile tempDir = tempFileService().createTempDir(FileTarget.TEMP, queueItem.getUserId(), TAG);
        garbageFileCollection.addFile(tempDir);

        try {
            commandBuilder.segmentTimes(List.of(3L, watermarkDuration + 3)).copyCodecs();

            String fileName = Any2AnyFileNameUtils.getFileName(tempDir.getName(), "%01d", queueItem.getFirstFileFormat().getExt());
            commandBuilder.out(new File(tempDir.getFile(), fileName).getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.toCmd());
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        }

        return new ArrayList<>(FileUtils.listFiles(tempDir.getFile(), null, false));
    }

    private SmartTempFile createFilesListFile(long userId, Collection<File> files) {
        SmartTempFile filesList = tempFileService().createTempFile(FileTarget.TEMP, userId, TAG, Format.TXT.getExt());
        try (PrintWriter printWriter = new PrintWriter(filesList.getAbsolutePath())) {
            for (File downloadedFile : files) {
                printWriter.println(filesListFileStr(downloadedFile.getAbsolutePath()));
            }

            return filesList;
        } catch (FileNotFoundException e) {
            tempFileService().delete(filesList);
            throw new ConvertException(e);
        }
    }
}
