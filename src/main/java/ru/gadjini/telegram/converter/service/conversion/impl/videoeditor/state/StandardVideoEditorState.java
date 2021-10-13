package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.state;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class StandardVideoEditorState implements VideoEditorState {

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    private FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper;

    private ScaleVideoEditorState scaleVideoEditorState;

    private StandardAudioCodecVideoEditorState standardAudioCodecVideoEditorState;

    @Autowired
    public StandardVideoEditorState(FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                                    FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper,
                                    FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper) {
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
        this.subtitlesStreamConversionHelper = subtitlesStreamConversionHelper;
    }

    @Autowired
    public void setStandardAudioCodecVideoEditorState(StandardAudioCodecVideoEditorState standardAudioCodecVideoEditorState) {
        this.standardAudioCodecVideoEditorState = standardAudioCodecVideoEditorState;
    }

    @Autowired
    public void setScaleVideoEditorState(ScaleVideoEditorState scaleVideoEditorState) {
        this.scaleVideoEditorState = scaleVideoEditorState;
    }

    @Override
    public void scale(String scale, AtomicReference<VideoEditorState> currentState) {
        if (StringUtils.isNotBlank(scale)) {
            currentState.set(scaleVideoEditorState);
        }
    }

    @Override
    public void audioCodec(String audioCodec, AtomicReference<VideoEditorState> currentState) {
        if (StringUtils.isNotBlank(audioCodec)) {
            currentState.set(standardAudioCodecVideoEditorState);
        }
    }

    @Override
    public void prepareCommand(FFmpegCommandBuilder commandBuilder, ConversionQueueItem fileQueueItem,
                               List<FFprobeDevice.FFProbeStream> allStreams, SettingsState settingsState,
                               String scale, String audioCodec, String audioCodecName,
                               Long audioBitrate, SmartTempFile result) throws InterruptedException {
        if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
            videoStreamConversionHelper.convertVideoCodecsForTelegramVideo(commandBuilder,
                    allStreams, fileQueueItem.getFirstFileFormat(), fileQueueItem.getSize(), 0);
        } else {
            videoStreamConversionHelper.convertVideoCodecs(commandBuilder, allStreams,
                    fileQueueItem.getFirstFileFormat(), result, 0);
        }
        videoStreamConversionHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
            audioStreamInVideoFileConversionHelper.copyOrConvertAudioCodecsForTelegramVideo(commandBuilder, allStreams, audioBitrate);
        } else {
            audioStreamInVideoFileConversionHelper.copyOrConvertAudioCodecs(baseCommand, commandBuilder, allStreams,
                    audioBitrate, result, fileQueueItem.getFirstFileFormat());
        }
        subtitlesStreamConversionHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder,
                allStreams, result, fileQueueItem.getFirstFileFormat());

        commandBuilder.fastConversion();
    }
}
