package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.common.ConverterMessagesProperties;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.CommandPipeLine;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

@Component
public class Video2GifConverter extends BaseAny2AnyConverter {

    private static final String TAG = "v2gif";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.GIF)
    );

    private FFprobeDevice fFprobeDevice;

    private LocalisationService localisationService;

    private FFmpegDevice fFmpegDevice;

    private UserService userService;

    @Autowired
    public Video2GifConverter(FFprobeDevice fFprobeDevice,
                              LocalisationService localisationService, FFmpegDevice fFmpegDevice, UserService userService) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.localisationService = localisationService;
        this.fFmpegDevice = fFmpegDevice;
        this.userService = userService;
    }

    public SmartTempFile doConvert2Gif(String fileId, ConversionQueueItem fileQueueItem) {
        SmartTempFile in = fileQueueItem.getDownloadedFileOrThrow(fileId);

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileId, TAG, Format.GIF.getExt());
        try {
            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();
            commandBuilder.hideBanner().quite().input(in.getAbsolutePath());

            FFprobeDevice.WHD whd = fFprobeDevice.getWHD(in.getAbsolutePath(), 0);
            if (whd.getDuration() == null || whd.getDuration() > 300) {
                throw new UserException(localisationService.getMessage(
                        ConverterMessagesProperties.MESSAGE_VIDEO_2_GIF_MAX_LENGTH, userService.getLocaleOrDefault(fileQueueItem.getUserId())
                ));
            }
            commandBuilder.vf("fps=10,scale=320:-1:flags=lanczos");
            commandBuilder.videoCodec(FFmpegCommandBuilder.PAM_CODEC);
            commandBuilder.f("image2pipe").streamOut();

            CommandPipeLine commandPipeLine = new CommandPipeLine();
            commandPipeLine.addCommand(commandBuilder.buildFullCommand());
            commandPipeLine.addCommand(new String[]{
                    "convert", "-delay", "10", "-", "-loop 0", "-layers", "optimize", "-alpha", "set", "-dispose previous",
                    result.getAbsolutePath()
            });
            fFmpegDevice.execute(commandPipeLine.build());

            return result;
        } catch (UserException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile result = doConvert2Gif(fileQueueItem.getFirstFileId(), fileQueueItem);
        String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), Format.GIF.getExt());

        return new FileResult(fileName, result);
    }
}
