package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

public class FFmpegTelegramVoiceContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        if (conversionContext.outputFormat().getCategory() == FormatCategory.AUDIO
                && conversionContext.outputFormat().canBeSentAsVoice()) {
            for (FFprobeDevice.FFProbeStream stream : conversionContext.audioStreams()) {
                stream.setTargetCodecName(FFmpegCommand.LIBOPUS);
            }
        }

        super.prepare(conversionContext);
    }
}
