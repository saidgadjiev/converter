package ru.gadjini.telegram.converter.service.command;

import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;

public class FFmpegVavMergeCommandBuilder extends BaseFFmpegCommandBuilderChain {

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    private FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper;

    public FFmpegVavMergeCommandBuilder(FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper,
                                        FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper) {
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
        this.subtitlesStreamConversionHelper = subtitlesStreamConversionHelper;
    }

    @Override
    public void prepareCommand(FFmpegCommand command, FFmpegConversionContext conversionContext) throws InterruptedException {
        int audioStreamsCount = conversionContext.getExtra(FFmpegConversionContext.AUDIO_STREAMS_COUNT);
        int subtitleStreamsCount = conversionContext.getExtra(FFmpegConversionContext.SUBTITLE_STREAMS_COUNT);
        FFmpegCommand baseCommand = new FFmpegCommand(command);
        if (audioStreamsCount > 0) {
            audioStreamInVideoFileConversionHelper.copyOrConvertAudioCodecs(command, conversionContext);
            if (subtitleStreamsCount == 0) {
                subtitlesStreamConversionHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, command, conversionContext);
            }
        } else {
            audioStreamInVideoFileConversionHelper.copyOrConvertAudioCodecs(command, conversionContext);
        }
        if (subtitleStreamsCount > 0) {
            subtitlesStreamConversionHelper.copyOrConvertOrIgnoreSubtitlesCodecs(baseCommand, command, conversionContext);
        }

        super.prepareCommand(command, conversionContext);
    }
}
