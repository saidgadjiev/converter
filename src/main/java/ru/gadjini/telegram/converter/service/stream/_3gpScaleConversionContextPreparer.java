package ru.gadjini.telegram.converter.service.stream;

import org.apache.commons.lang3.StringUtils;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

public class _3gpScaleConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            if (conversionContext.outputFormat() == Format._3GP
                    && StringUtils.isBlank(stream.getTargetScale())) {
                stream.setTargetScale(FFmpegCommand._3GP_SCALE);
            }
        }

        super.prepare(conversionContext);
    }
}
