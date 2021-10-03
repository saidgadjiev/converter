package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.state;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoCommandPreparer;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;

@Component
public class ScaleVideoEditorState implements VideoEditorState {

    private FFmpegVideoCommandPreparer videoStreamsChangeHelper;

    @Autowired
    public ScaleVideoEditorState(FFmpegVideoCommandPreparer videoStreamsChangeHelper) {
        this.videoStreamsChangeHelper = videoStreamsChangeHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommandBuilder commandBuilder, ConversionQueueItem fileQueueItem,
                               List<FFprobeDevice.FFProbeStream> allStreams, SettingsState settingsState, String scale,
                               String audioCodec, String audioCodecName, Long audioBitrate, SmartTempFile result) throws InterruptedException {
        videoStreamsChangeHelper.prepareCommandForVideoScaling(commandBuilder, allStreams, result, scale,
                audioCodec, audioCodecName, audioBitrate,
                fileQueueItem.getFirstFileFormat(),
                Integer.parseInt(settingsState.getCrf()),
                fileQueueItem.getSize());
    }
}
