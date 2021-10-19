package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoAudioChannelLayoutState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVideoEditorCommandBuilder extends BaseFFmpegCommandBuilderChain {

    @Override
    public void prepareCommand(FFmpegCommand command,
                               FFmpegConversionContext conversionContext) throws InterruptedException {
        SettingsState settingsState = (SettingsState) args[0];

        if (EditVideoAudioChannelLayoutState.MONO.equals(settingsState.getAudioChannelLayout())) {
            command.ac("1");
        } else if (EditVideoAudioChannelLayoutState.STEREO.equals(settingsState.getAudioChannelLayout())) {
            command.ac("2");
        }

        super.prepareCommand(command, conversionContext);
    }
}
