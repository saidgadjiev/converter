package ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.state;

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

@Component
public class StandardAudioCodecVideoEditorState implements VideoEditorState {

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    private FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper;

    @Autowired
    public StandardAudioCodecVideoEditorState(FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                                              FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper,
                                              FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper) {
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
        this.subtitlesStreamConversionHelper = subtitlesStreamConversionHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommandBuilder commandBuilder, ConversionQueueItem fileQueueItem,
                               List<FFprobeDevice.Stream> allStreams, SettingsState settingsState, String scale,
                               String audioCodec, String audioCodecName, Long audioBitrate,
                               SmartTempFile result) throws InterruptedException {
        if (fileQueueItem.getFirstFileFormat().canBeSentAsVideo()) {
            videoStreamConversionHelper.convertVideoCodecsForTelegramVideo(commandBuilder,
                    allStreams, fileQueueItem.getFirstFileFormat(), fileQueueItem.getSize());
        } else {
            videoStreamConversionHelper.convertVideoCodecs(commandBuilder, allStreams,
                    fileQueueItem.getFirstFileFormat(), result, fileQueueItem.getSize());
        }
        videoStreamConversionHelper.addVideoTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());

        FFmpegCommandBuilder baseCommand = new FFmpegCommandBuilder(commandBuilder);
        audioStreamInVideoFileConversionHelper.copyOrConvertToTargetAudioCodecs(commandBuilder, allStreams,
                audioCodec, audioCodecName, audioBitrate, true);
        subtitlesStreamConversionHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, commandBuilder,
                allStreams, result, fileQueueItem.getFirstFileFormat());

        commandBuilder.fastConversion();
    }
}
