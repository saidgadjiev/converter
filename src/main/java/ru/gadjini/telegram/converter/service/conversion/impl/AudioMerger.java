package ru.gadjini.telegram.converter.service.conversion.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Component
public class AudioMerger extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioMerger.class);

    private static final String TAG = "amerger";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.AUDIO), List.of(Format.MERGE)
    );

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    @Autowired
    public AudioMerger(FFprobeDevice fFprobeDevice, FFmpegDevice fFmpegDevice) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile filesList = createFilesListFile(fileQueueItem);

        Format targetFormat = fileQueueItem.getFirstFileFormat();
        SmartTempFile result = tempFileService().createTempFile(FileTarget.TEMP, fileQueueItem.getUserId(), TAG,
                targetFormat.getExt());
        try {
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder().hideBanner().quite()
                    .f(FFmpegCommandBuilder.CONCAT).safe("0").input(filesList.getAbsolutePath()).copyCodecs()
                    .out(result.getAbsolutePath());

            fFmpegDevice.execute(commandBuilder.buildFullCommand());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), targetFormat.getExt());
            if (targetFormat.canBeSentAsAudio()) {
                long durationInSeconds = 0;
                try {
                    durationInSeconds = fFprobeDevice.getDurationInSeconds(result.getAbsolutePath());
                    fileQueueItem.getFirstFile().setDuration((int) durationInSeconds);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }

                return new AudioResult(fileName, result, fileQueueItem.getFirstFile().getAudioPerformer(),
                        fileQueueItem.getFirstFile().getAudioTitle(), (int) durationInSeconds);
            }

            return new FileResult(fileName, result);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        } finally {
            tempFileService().delete(filesList);
        }
    }

    private SmartTempFile createFilesListFile(ConversionQueueItem conversionQueueItem) {
        SmartTempFile filesList = tempFileService().createTempFile(FileTarget.TEMP, conversionQueueItem.getUserId(), TAG, Format.TXT.getExt());
        try (PrintWriter printWriter = new PrintWriter(filesList.getAbsolutePath())) {
            for (SmartTempFile downloadedFile : conversionQueueItem.getDownloadedFiles()) {
                printWriter.println(filesListFileStr(downloadedFile.getAbsolutePath()));
            }

            return filesList;
        } catch (FileNotFoundException e) {
            tempFileService().delete(filesList);
            throw new ConvertException(e);
        }
    }

    private String filesListFileStr(String filePath) {
        return "file '" + filePath + "'";
    }
}