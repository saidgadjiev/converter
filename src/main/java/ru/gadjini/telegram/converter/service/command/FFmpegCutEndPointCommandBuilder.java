package ru.gadjini.telegram.converter.service.command;

import org.joda.time.Period;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegCutEndPointCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        Period cutEndPoint = conversionContext.getExtra(FFmpegConversionContext.CUT_END_POINT);
        Period cutStartPoint = conversionContext.getExtra(FFmpegConversionContext.CUT_START_POINT);
        String duration = String.valueOf(cutEndPoint.minus(cutStartPoint).toStandardDuration().getStandardSeconds());

        command.t(duration);

        super.prepareCommand(command, conversionContext);
    }
}
