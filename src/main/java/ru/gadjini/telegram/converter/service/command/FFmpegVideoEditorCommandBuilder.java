package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioChannelLayoutState;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoQualityState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVideoEditorCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command,
                               FFmpegConversionContext conversionContext) throws InterruptedException {
        SettingsState settingsState = conversionContext.getExtra(FFmpegConversionContext.SETTINGS_STATE);

        if (EditVideoAudioChannelLayoutState.MONO.equals(settingsState.getAudioChannelLayout())) {
            command.ac("1");
        } else if (EditVideoAudioChannelLayoutState.STEREO.equals(settingsState.getAudioChannelLayout())) {
            command.ac("2");
        }
        if (!EditVideoQualityState.AUTO.equals(settingsState.getCompressBy())) {
            command.crf(settingsState.getCompressBy());
        }

        super.prepareCommand(command, conversionContext);
    }
}
