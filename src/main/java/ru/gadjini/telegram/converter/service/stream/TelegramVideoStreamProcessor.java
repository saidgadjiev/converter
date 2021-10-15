package ru.gadjini.telegram.converter.service.stream;

import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public class TelegramVideoStreamProcessor extends BaseStreamProcessor {

    @Override
    public void process(Format format, List<FFprobeDevice.FFProbeStream> streams, Object... args) {
        if (format.canBeSentAsVideo()) {
            for (FFprobeDevice.FFProbeStream stream : streams) {
                if (stream.getCodecName().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)) {
                    stream.setTargetCodecType(FFmpegAudioStreamInVideoFileConversionHelper.TELEGRAM_VIDEO_AUDIO_CODEC);
                } else if (stream.getCodecName().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)) {
                    stream.setTargetCodecType(FFmpegCommand.H264_CODEC);
                }
            }
        }

        super.process(format, streams, args);
    }
}
