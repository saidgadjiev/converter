package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;

public class FFmpegExtractAudioContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    private FFmpegDevice fFmpegDevice;

    public FFmpegExtractAudioContextPreparer(FFmpegDevice fFmpegDevice) {
        this.fFmpegDevice = fFmpegDevice;
    }

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        FFmpegCommand command = new FFmpegCommand().hideBanner().quite();
        command.input(conversionContext.getInput().getAbsolutePath());
        int index = conversionContext.getExtra(FFmpegConversionContext.EXTRACT_AUDIO_INDEX);
        command.mapAudio(index);
        command.out(conversionContext.output().getAbsolutePath());

        if (fFmpegDevice.isChannelMapError(command.toCmd())) {
            conversionContext.useStaticAudioFilter();
        }

        super.prepare(conversionContext);
    }
}
