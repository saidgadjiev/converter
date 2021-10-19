package ru.gadjini.telegram.converter.service.command;

import org.joda.time.Period;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

import static ru.gadjini.telegram.converter.service.conversion.impl.VideoCutter.PERIOD_FORMATTER;

public class FFmpegCutStartPointCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        Period startPoint = conversionContext.getExtra(FFmpegConversionContext.CUT_START_POINT);
        String startPointStr = PERIOD_FORMATTER.print(startPoint);

        command.ss(startPointStr);

        super.prepareCommand(command, conversionContext);
    }
}
