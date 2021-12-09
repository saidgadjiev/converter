package ru.gadjini.telegram.converter.service.stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioBitrateState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioCodecState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoQualityState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoResolutionState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.service.conversion.codec.AudioCodecHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;

public class VideoEditorConversionContextProcessor extends BaseFFmpegConversionContextPreparerChain {

    @Override
    public void prepare(FFmpegConversionContext conversionContext) throws InterruptedException {
        SettingsState settingsState = conversionContext.getExtra(FFmpegConversionContext.SETTINGS_STATE);
        for (FFprobeDevice.FFProbeStream stream : conversionContext.streams()) {
            if (stream.getCodecType().equals(FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE)) {
                if (!EditVideoResolutionState.AUTO.equals(settingsState.getResolution())) {
                    String height = settingsState.getResolution().replace("p", "");
                    String scale = "scale=-2:" + (NumberUtils.isDigits(height) ? height : "ceil(ih" + height + "/2)*2");
                    stream.setTargetScale(scale);
                }
            } else if (stream.getCodecType().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE)) {
                if (!StringUtils.isBlank(settingsState.getAudioBitrate())
                        && !EditVideoAudioBitrateState.AUTO.equals(settingsState.getAudioBitrate())) {
                    stream.setTargetBitrate(settingsState.getParsedAudioBitrate());
                }
                if (!StringUtils.isBlank(settingsState.getAudioCodec())
                        && !EditVideoAudioCodecState.AUTO.equals(settingsState.getAudioCodec())) {
                    stream.setTargetCodec(settingsState.getAudioCodec(), AudioCodecHelper.getCodec(settingsState.getAudioCodec()));
                } else {
                    stream.setTargetCodec(stream.getCodecName(), AudioCodecHelper.getCodec(stream.getCodecName()));
                }
            }
        }
        if (!EditVideoQualityState.AUTO.equals(settingsState.getCompressBy())) {
            conversionContext.setUseCrf(true);
        }

        super.prepare(conversionContext);
    }
}
