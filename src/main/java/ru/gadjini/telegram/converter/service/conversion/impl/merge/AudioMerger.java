package ru.gadjini.telegram.converter.service.conversion.impl.merge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.AudioResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
import ru.gadjini.telegram.converter.service.conversion.impl.FFmpegAudioFormatsConverter;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AudioMerger extends BaseAny2AnyConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioMerger.class);

    private static final String TAG = "amerger";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.AUDIO), List.of(Format.MERGE)
    );

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    private FilesListCreator filesListCreator;

    private FFmpegAudioFormatsConverter audioFormatsConverter;

    @Autowired
    public AudioMerger(FFprobeDevice fFprobeDevice, FFmpegDevice fFmpegDevice,
                       FFmpegAudioFormatsConverter audioFormatsConverter,
                       FFmpegCommandBuilderFactory commandBuilderFactory,
                       FilesListCreator filesListCreator) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.audioFormatsConverter = audioFormatsConverter;

        this.commandBuilderChain = commandBuilderFactory.quite();
        this.filesListCreator = filesListCreator;
        commandBuilderChain.setNext(commandBuilderFactory.concat())
                .setNext(commandBuilderFactory.input())
                .setNext(commandBuilderFactory.audioMerge())
                .setNext(commandBuilderFactory.audioConversionDefaultOptions())
                .setNext(commandBuilderFactory.output());
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
            SmartTempFile filesList = filesListCreator.createFilesList(fileQueueItem.getUserId(), filesToConcatenate);

            Format targetFormat = fileQueueItem.getFirstFileFormat();
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), TAG,
                    targetFormat.getExt());
            try {
                FFmpegConversionContext conversionContext = FFmpegConversionContext.from(filesList, result, targetFormat);
                FFmpegCommand command = new FFmpegCommand();
                commandBuilderChain.prepareCommand(command, conversionContext);
                fFmpegDevice.execute(command.toCmd());

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

    private List<SmartTempFile> prepareFilesToConcatenate(ConversionQueueItem fileQueueItem) throws InterruptedException {
        if (isTheSameFormatFiles(fileQueueItem) && isTheSameCodecs(fileQueueItem)) {
            return fileQueueItem.getDownloadedFiles();
        }
        List<SmartTempFile> files = new ArrayList<>();
        for (int i = 0; i < fileQueueItem.getDownloadedFiles().size(); i++) {
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                    fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
            files.add(result);

            try {
                audioFormatsConverter.doConvert(fileQueueItem.getDownloadedFiles().get(i), result, fileQueueItem.getFirstFileFormat());
            } catch (Throwable e) {
                files.forEach(f -> tempFileService().delete(f));
            }
        }

        return files;
    }

    private boolean isTheSameFormatFiles(ConversionQueueItem queueItem) {
        return queueItem.getFiles().stream().allMatch(s -> s.getFormat().equals(queueItem.getFirstFile().getFormat()));
    }

    private boolean isTheSameCodecs(ConversionQueueItem queueItem) throws InterruptedException {
        SmartTempFile firstFile = queueItem.getDownloadedFiles().get(0);
        List<FFprobeDevice.FFProbeStream> firstAudioStreams = fFprobeDevice.getAudioStreams(firstFile.getAbsolutePath()
        );

        for (int i = 1; i < queueItem.getDownloadedFiles().size(); i++) {
            List<FFprobeDevice.FFProbeStream> audioStreams = fFprobeDevice.getAudioStreams(
                    queueItem.getDownloadedFiles().get(i).getAbsolutePath());

            if (!equalAudioStreams(firstAudioStreams, audioStreams)) {
                return false;
            }
        }

        return true;
    }

    private boolean equalAudioStreams(List<FFprobeDevice.FFProbeStream> src, List<FFprobeDevice.FFProbeStream> target) {
        if (src.size() != target.size()) {
            return false;
        }
        for (int i = 0; i < src.size(); ++i) {
            if (!equalStreams(src.get(i), target.get(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean equalStreams(FFprobeDevice.FFProbeStream src, FFprobeDevice.FFProbeStream target) {
        return Objects.equals(src.getCodecName(), target.getCodecName());
    }
}
