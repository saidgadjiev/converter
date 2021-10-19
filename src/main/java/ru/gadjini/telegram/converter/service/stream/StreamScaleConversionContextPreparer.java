package ru.gadjini.telegram.converter.service.stream;

import org.apache.commons.lang3.StringUtils;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format._3GP;

public class StreamScaleConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        for (FFprobeDevice.FFProbeStream stream : conversionContext.videoStreams()) {
            if (StringUtils.isBlank(stream.getTargetScale())) {
                String scale = conversionContext.outputFormat() == _3GP ? FFmpegCommand._3GP_SCALE : FFmpegCommand.EVEN_SCALE;
                stream.setTargetScale(scale);
            }
        }

        super.prepare(conversionContext);
    }
}
