package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.state;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.bot.edit.video.state.EditVideoCrfState;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoCommandPreparer;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ScaleVideoEditorState implements VideoEditorState {

    private FFmpegVideoCommandPreparer videoStreamsChangeHelper;

    private ScaleAudioCodecVideoEditorState scaleAudioCodecVideoEditorState;

    @Autowired
    public ScaleVideoEditorState(FFmpegVideoCommandPreparer videoStreamsChangeHelper) {
        this.videoStreamsChangeHelper = videoStreamsChangeHelper;
    }

    @Autowired
    public void setScaleAudioCodecVideoEditorState(ScaleAudioCodecVideoEditorState scaleAudioCodecVideoEditorState) {
        this.scaleAudioCodecVideoEditorState = scaleAudioCodecVideoEditorState;
    }

    @Override
    public void audioCodec(String audioCodec, AtomicReference<VideoEditorState> currentState) {
        if (StringUtils.isNotBlank(audioCodec)) {
            currentState.set(scaleAudioCodecVideoEditorState);
        }
    }

    @Override
    public void prepareCommand(FFmpegCommandBuilder commandBuilder, ConversionQueueItem fileQueueItem,
                               List<FFprobeDevice.Stream> allStreams, SettingsState settingsState, String scale,
                               String audioCodec, String audioCodecName, SmartTempFile result) throws InterruptedException {
        videoStreamsChangeHelper.prepareCommandForVideoScaling(commandBuilder, allStreams, result, scale,
                fileQueueItem.getFirstFileFormat(), EditVideoCrfState.DONT_CHANGE.equals(settingsState.getCrf()),
                fileQueueItem.getSize());
    }
}
