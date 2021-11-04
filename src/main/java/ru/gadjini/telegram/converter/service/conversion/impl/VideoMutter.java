package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.MUTE;

@Component
@SuppressWarnings("PMD")
public class VideoMutter extends BaseAny2AnyConverter {

    private static final String TAG = "vmute";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(MUTE)
    );

    private final FFmpegCommandBuilderChain commandBuilderChain;

    private final FFmpegConversionContextPreparerChain conversionContextPreparerChain;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public VideoMutter(FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                       FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                       UserService userService, FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                       FFmpegCommandBuilderFactory commandBuilderFactory,
                       FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory, VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;
        this.callbackHandlerFactory = callbackHandlerFactory;

        this.commandBuilderChain = commandBuilderFactory.quiteInput();
        this.videoResultBuilder = videoResultBuilder;
        commandBuilderChain.setNext(commandBuilderFactory.videoConversion())
                .setNext(commandBuilderFactory.subtitlesConversion())
                .setNext(commandBuilderFactory.webmQuality())
                .setNext(commandBuilderFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderFactory.output());

        this.conversionContextPreparerChain = contextPreparerChainFactory.telegramVideoContextPreparer();
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegConversionContext conversionContext = new FFmpegConversionContext()
                    .streams(allStreams)
                    .input(file)
                    .output(result)
                    .outputFormat(fileQueueItem.getFirstFileFormat());
            conversionContextPreparerChain.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);

            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(file.getAbsolutePath(), fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams));
            FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(fileQueueItem,
                    whd.getDuration(), userService.getLocaleOrDefault(fileQueueItem.getUserId()));
            fFmpegDevice.execute(command.toCmd(), callback);

            return videoResultBuilder.build(fileQueueItem, fileQueueItem.getFirstFileFormat(), result);
        } catch (CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
