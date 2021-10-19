package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class TelegramVideoConversionContextPreparer extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) {
        if (conversionContext.outputFormat().canBeSentAsVideo()) {
            for (FFprobeDevice.FFProbeStream stream : conversionContext.streams()) {
                if (stream.getCodecName().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)) {
                    stream.setTargetCodecName(FFmpegAudioStreamInVideoFileConversionHelper.TELEGRAM_VIDEO_AUDIO_CODEC);
                } else if (stream.getCodecName().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)) {
                    stream.setTargetCodecName(FFmpegCommand.H264_CODEC);
                }
            }
        }

        super.prepare(conversionContext);
    }
}
