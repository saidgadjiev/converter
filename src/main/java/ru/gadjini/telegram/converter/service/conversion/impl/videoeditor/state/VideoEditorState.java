package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.state;

import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface VideoEditorState {

    default void scale(String scale, AtomicReference<VideoEditorState> currentState) {

    }

    default void audioCodec(String audioCodec, AtomicReference<VideoEditorState> currentState) {

    }

    void prepareCommand(FFmpegCommandBuilder commandBuilder, ConversionQueueItem fileQueueItem,
                        List<FFprobeDevice.Stream> allStreams, SettingsState settingsState,
                        String scale, String audioCodec, String audioCodecName, SmartTempFile result) throws InterruptedException;
}
