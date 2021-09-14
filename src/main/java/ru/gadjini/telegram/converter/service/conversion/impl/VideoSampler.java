package ru.gadjini.telegram.converter.service.conversion.impl;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.exception.CorruptedVideoException;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.exception.UserException;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.List;
import java.util.Map;

@Component
@SuppressWarnings("CPD-START")
public class VideoSampler extends BaseAny2AnyConverter {

    public static final String TAG = "vsampler";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(Format.SAMPLE)
    );

    public static final Period AT_START = Period.ZERO;

    private FFmpegVideoStreamConversionHelper fFmpegVideoHelper;

    private FFprobeDevice fFprobeDevice;

    private Jackson jackson;

    private VideoCutter videoCutter;

    private Video2StreamingConverter video2StreamingConverter;

    @Autowired
    public VideoSampler(FFmpegVideoStreamConversionHelper fFmpegVideoHelper,
                        FFprobeDevice fFprobeDevice,
                        Jackson jackson,
                        VideoCutter videoCutter,
                        Video2StreamingConverter video2StreamingConverter) {
        super(MAP);
        this.fFmpegVideoHelper = fFmpegVideoHelper;
        this.fFprobeDevice = fFprobeDevice;
        this.jackson = jackson;
        this.videoCutter = videoCutter;
        this.video2StreamingConverter = video2StreamingConverter;
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFileOrThrow(fileQueueItem.getFirstFileId());

        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, fileQueueItem.getUserId(),
                fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFprobeDevice.WHD srcWhd = fFprobeDevice.getWHD(file.getAbsolutePath(), fFmpegVideoHelper.getFirstVideoStreamIndex(allStreams));

            SettingsState settingsState = jackson.convertValue(fileQueueItem.getExtra(), SettingsState.class);
            Period sp = getStartPoint(srcWhd, settingsState);

            videoCutter.doCut(file, result, sp, sp.plusSeconds(30), srcWhd.getDuration(), fileQueueItem);

            return video2StreamingConverter.doConvert(result, fileQueueItem);
        } catch (UserException | CorruptedVideoException e) {
            tempFileService().delete(result);
            throw e;
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }

    private Period getStartPoint(FFprobeDevice.WHD whd, SettingsState settingsState) {
        if (settingsState.getCutStartPoint() != null) {
            return settingsState.getCutStartPoint();
        }
        if (whd.getDuration() == null) {
            return Period.seconds(0);
        }

        if (whd.getDuration() < 30) {
            return Period.seconds(0);
        }

        return Period.seconds(new RandomDataGenerator().nextInt(30, whd.getDuration().intValue() - 30));
    }
}
