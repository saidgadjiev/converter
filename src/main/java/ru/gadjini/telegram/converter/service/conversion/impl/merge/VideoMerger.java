package ru.gadjini.telegram.converter.service.conversion.impl.merge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.VideoResult;
import ru.gadjini.telegram.converter.service.conversion.impl.BaseAny2AnyConverter;
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
@SuppressWarnings("CPD-START")
public class VideoMerger extends BaseAny2AnyConverter {

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.MERGE)
    );

    private static final String TAG = "vmerger";

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    @Autowired
    public VideoMerger(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        List<SmartTempFile> filesToConcatenate = fileQueueItem.getDownloadedFilesWithoutThumb();

        try {
            SmartTempFile filesList = createFilesListFile(fileQueueItem.getUserId(), filesToConcatenate);

            Format targetFormat = fileQueueItem.getFirstFileFormat();
            SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(), TAG,
                    targetFormat.getExt());
            try {
                List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(filesToConcatenate.get(0).getAbsolutePath());
                FFmpegCommand commandBuilder = new FFmpegCommand().hideBanner().quite()
                        .f(FFmpegCommand.CONCAT).safe("0").input(filesList.getAbsolutePath())
                        .mapVideo().copyVideo();
                if (allStreams.stream().anyMatch(a -> FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE.equals(a.getCodecType()))) {
                    commandBuilder.mapAudio().copyAudio();
                }
                if (allStreams.stream().anyMatch(a -> FFprobeDevice.FFProbeStream.SUBTITLE_CODEC_TYPE.equals(a.getCodecType()))) {
                    commandBuilder.mapSubtitles().copySubtitles();
                }
                commandBuilder.out(result.getAbsolutePath());

                fFmpegDevice.execute(commandBuilder.toCmd());

                String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), targetFormat.getExt());

                if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
                    FFprobeDevice.WHD whd = fFprobeDevice.getWHD(result.getAbsolutePath(), 0);
                    return new VideoResult(fileName, result, fileQueueItem.getFirstFileFormat(), downloadThumb(fileQueueItem), whd.getWidth(), whd.getHeight(),
                            whd.getDuration(), fileQueueItem.getFirstFileFormat().supportsStreaming());
                } else {
                    return new FileResult(fileName, result, downloadThumb(fileQueueItem));
                }
            } catch (Throwable e) {
                tempFileService().delete(result);
                throw new ConvertException(e);
            } finally {
                tempFileService().delete(filesList);
            }
        } finally {
            filesToConcatenate.forEach(f -> tempFileService().delete(f));
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

    private String filesListFileStr(String filePath) {
        return "file '" + filePath + "'";
    }
}
