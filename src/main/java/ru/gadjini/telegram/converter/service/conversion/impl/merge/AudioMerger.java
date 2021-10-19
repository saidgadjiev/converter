package ru.gadjini.telegram.converter.service.conversion.impl.merge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.impl.FFmpegAudioFormatsConverter;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

    private FFmpegAudioStreamConversionHelper audioHelper;

    private FFmpegAudioFormatsConverter audioFormatsConverter;

    @Autowired
    public AudioMerger(FFprobeDevice fFprobeDevice, FFmpegDevice fFmpegDevice,
                       FFmpegAudioStreamConversionHelper audioHelper, FFmpegAudioFormatsConverter audioFormatsConverter) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.audioHelper = audioHelper;
        this.audioFormatsConverter = audioFormatsConverter;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        List<SmartTempFile> filesToConcatenate = null;

        try {
            filesToConcatenate = prepareFilesToConcatenate(fileQueueItem);
            SmartTempFile filesList = createFilesListFile(fileQueueItem.getUserId(), filesToConcatenate);

            Format targetFormat = fileQueueItem.getFirstFileFormat();
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), TAG,
                    targetFormat.getExt());
            try {
                FFmpegCommand commandBuilder = new FFmpegCommand().hideBanner().quite()
                        .f(FFmpegCommand.CONCAT).safe("0").input(filesList.getAbsolutePath())
                        .mapAudio(0).copyAudio();

                audioHelper.addAudioTargetOptions(commandBuilder, targetFormat);
                commandBuilder.out(result.getAbsolutePath());

                fFmpegDevice.execute(commandBuilder.toCmd());

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
                            fileQueueItem.getFirstFile().getAudioTitle(), downloadThumb(fileQueueItem), (int) durationInSeconds);
                }

                return new FileResult(fileName, result);
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw new ConvertException(e);
            } finally {
                tempFileService().delete(filesList);
            }
        } catch (InterruptedException e) {
            throw new ConvertException(e);
        } finally {
            if (filesToConcatenate != null) {
                filesToConcatenate.forEach(f -> tempFileService().delete(f));
            }
        }
    }

    private SmartTempFile createFilesListFile(long userId, List<SmartTempFile> files) {
        SmartTempFile filesList = tempFileService().createTempFile(FileTarget.TEMP, userId, TAG, Format.TXT.getExt());
        try (PrintWriter printWriter = new PrintWriter(filesList.getAbsolutePath())) {
            for (SmartTempFile downloadedFile : files) {
                printWriter.println(filesListFileStr(downloadedFile.getAbsolutePath()));
            }

            return filesList;
        } catch (FileNotFoundException e) {
            tempFileService().delete(filesList);
            throw new ConvertException(e);
        }
    }

    private List<SmartTempFile> prepareFilesToConcatenate(ConversionQueueItem fileQueueItem) throws InterruptedException {
        if (isTheSameFormatFiles(fileQueueItem) && isTheSameCodecs(fileQueueItem)) {
            return fileQueueItem.getDownloadedFiles();
        }
        List<SmartTempFile> files = new ArrayList<>();
        for (int i = 1; i < fileQueueItem.getDownloadedFiles().size(); i++) {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
            files.add(result);

            try {
                audioFormatsConverter.doConvertAudio(fileQueueItem.getDownloadedFiles().get(i), result, fileQueueItem.getFirstFileFormat());
            } catch (Throwable e) {
                files.forEach(f -> tempFileService().delete(f));
            }
        }

        return files;
    }

    private String filesListFileStr(String filePath) {
        return "file '" + filePath + "'";
    }

    private boolean isTheSameFormatFiles(ConversionQueueItem queueItem) {
        return queueItem.getFiles().stream().allMatch(s -> s.getFormat().equals(queueItem.getFirstFile().getFormat()));
    }

    private boolean isTheSameCodecs(ConversionQueueItem queueItem) throws InterruptedException {
        SmartTempFile firstFile = queueItem.getDownloadedFiles().get(0);
        List<FFprobeDevice.FFProbeStream> firstAudioStreams = fFprobeDevice.getAudioStreams(firstFile.getAbsolutePath());

        for (int i = 1; i < queueItem.getDownloadedFiles().size(); i++) {
            List<FFprobeDevice.FFProbeStream> audioStreams = fFprobeDevice.getAudioStreams(queueItem.getDownloadedFiles().get(i).getAbsolutePath());

            if (!firstAudioStreams.equals(audioStreams)) {
                return false;
            }
        }

        return true;
    }
}
