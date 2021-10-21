package ru.gadjini.telegram.converter.service.stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioBitrateState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioCodecState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class VideoEditorConversionContextProcessor extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        SettingsState settingsState = conversionContext.getExtra(FFmpegConversionContext.SETTINGS_STATE);
        for (FFprobeDevice.FFProbeStream stream : conversionContext.streams()) {
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
                    stream.setTargetCodecName(stream.getCodecType());
                } else {
                    stream.setTargetCodecName(settingsState.getAudioCodec());
                }
            }
        }

        super.prepare(conversionContext);
    }
}
