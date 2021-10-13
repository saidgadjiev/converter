package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioBitrateState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioCodecState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.BaseStreamProcessor;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.util.List;

public class VideoEditorStreamProcessor extends BaseStreamProcessor {

    @Override
    public void process(Format format, List<FFprobeDevice.FFProbeStream> allStreams, Object... args) {
        SettingsState settingsState = (SettingsState) args[0];
        for (FFprobeDevice.FFProbeStream stream : allStreams) {
            if (stream.getCodecName().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)) {
                stream.setTargetBitrate(settingsState.getVideoBitrate());

                String height = settingsState.getResolution().replace("p", "");
                String scale = EditVideoResolutionState.AUTO.equals(settingsState.getResolution()) ? null
                        : "scale=-2:" + (NumberUtils.isDigits(height) ? height : "ceil(ih" + height + "/2)*2");
                stream.setTargetScale(scale);
            } else if (stream.getCodecName().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)) {
                if (StringUtils.isBlank(settingsState.getAudioBitrate())
                        || EditVideoAudioBitrateState.AUTO.equals(settingsState.getAudioBitrate())) {
                    stream.setTargetBitrate(stream.getBitRate());
                } else {
                    stream.setTargetBitrate(settingsState.getParsedAudioBitrate());
                }
                if (StringUtils.isBlank(settingsState.getAudioCodec())
                        || EditVideoAudioCodecState.AUTO.equals(settingsState.getAudioCodec())) {
                    stream.setTargetCodecType(stream.getCodecType());
                } else {
                    stream.setTargetCodecType(settingsState.getAudioCodec());
                }
            }
        }

        super.process(format, allStreams, args);
    }
}
