package ru.gadjini.telegram.converter.service.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.property.FontProperties;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegAudioStreamInVideoFileConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegSubtitlesStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper.FFmpegVideoStreamConversionHelper;
import ru.gadjini.telegram.converter.service.conversion.impl.Tgs2GifConverter;
import ru.gadjini.telegram.converter.service.conversion.impl.Video2GifConverter;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.smart.bot.commons.service.LocalisationService;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;

@Component
public class FFmpegCommandBuilderFactory {

    private FFmpegVideoStreamConversionHelper videoStreamConversionHelper;

    private FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper;

    private FFmpegAudioStreamConversionHelper audioStreamConversionHelper;

    private FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper;

    private Tgs2GifConverter tgs2GifConverter;

    private Video2GifConverter video2GifConverter;

    private LocalisationService localisationService;

    private FontProperties fontProperties;

    private FFprobeDevice fFprobeDevice;

    private UserService userService;

    @Autowired
    public FFmpegCommandBuilderFactory(FFmpegVideoStreamConversionHelper videoStreamConversionHelper,
                                       FFmpegAudioStreamInVideoFileConversionHelper audioStreamInVideoFileConversionHelper,
                                       FFmpegAudioStreamConversionHelper audioStreamConversionHelper,
                                       FFmpegSubtitlesStreamConversionHelper subtitlesStreamConversionHelper,
                                       Tgs2GifConverter tgs2GifConverter, Video2GifConverter video2GifConverter,
                                       LocalisationService localisationService, FontProperties fontProperties,
                                       FFprobeDevice fFprobeDevice, UserService userService) {
        this.videoStreamConversionHelper = videoStreamConversionHelper;
        this.audioStreamInVideoFileConversionHelper = audioStreamInVideoFileConversionHelper;
        this.audioStreamConversionHelper = audioStreamConversionHelper;
        this.subtitlesStreamConversionHelper = subtitlesStreamConversionHelper;
        this.tgs2GifConverter = tgs2GifConverter;
        this.video2GifConverter = video2GifConverter;
        this.localisationService = localisationService;
        this.fontProperties = fontProperties;
        this.fFprobeDevice = fFprobeDevice;
        this.userService = userService;
    }

    public FFmpegCommandBuilderChain enableExperimentalFeatures() {
        return new FFmpegEnableExperimentalFeaturesCommandBuilder();
    }

    public FFmpegCommandBuilderChain fastVideoConversion() {
        return new FFmpegFastVideoConversionCommandBuilder();
    }

    public FFmpegCommandBuilderChain input() {
        return new FFmpegInputCommandBuilder();
    }

    public FFmpegCommandBuilderChain output() {
        return new FFmpegOutputCommandBuilder();
    }

    public FFmpegCommandBuilderChain subtitlesConversion() {
        return new FFmpegSubtitleConvertCommandBuilder(subtitlesStreamConversionHelper);
    }

    public FFmpegCommandBuilderChain videoConversion() {
        return new FFmpegVideoConvertCommandBuilder(videoStreamConversionHelper);
    }

    public FFmpegCommandBuilderChain telegramVideoConversion() {
        return new FFmpegTelegramVideoCommandBuilder();
    }

    public FFmpegCommandBuilderChain synchronizeVideoTimestamp() {
        return new FFmpegSynchronizeVideoTimestampsForVbrCommandBuilder();
    }

    public FFmpegCommandBuilderChain maxMuxingQueueSize() {
        return new FFmpegVideoConversionMaxMuxingQueueSizeCommandBuilder();
    }

    public FFmpegCommandBuilderChain audioInVideoConversion() {
        return new FFmpegAudioInVideoConvertCommandBuilder(audioStreamInVideoFileConversionHelper);
    }

    public FFmpegCommandBuilderChain videoEditor() {
        return new FFmpegVideoEditorCommandBuilder();
    }

    public FFmpegCommandBuilderChain webmQuality() {
        return new FFmpegWebmQualityCommandBuilder();
    }

    public FFmpegCommandBuilderChain audioChannelMapFilter() {
        return new FFmpegVideoConversionAudioChannelMapFilterCommandBuilder(audioStreamInVideoFileConversionHelper);
    }

    public FFmpegCommandBuilderChain videoWatermarkInput() {
        return new FFmpegVideoWatermarkInputCommandBuilder(tgs2GifConverter, video2GifConverter, localisationService, userService);
    }

    public FFmpegCommandBuilderChain videoWatermark() {
        return new FFmpegVideoWatermarkCommandBuilder(fontProperties);
    }

    public FFmpegAudioCodecsConvertCommandBuilder audioConversion() {
        return new FFmpegAudioCodecsConvertCommandBuilder(audioStreamInVideoFileConversionHelper);
    }

    public FFmpegAudioCoverCommandBuilder audioCover() {
        return new FFmpegAudioCoverCommandBuilder(audioStreamConversionHelper);
    }

    public FFmpegCommandBuilderChain hideBannerQuite() {
        return new FFmpegHideBannerQuiteCommandBuilder();
    }

    public FFmpegCommandBuilderChain cutStartPoint() {
        return new FFmpegCutStartPointCommandBuilder();
    }

    public FFmpegCommandBuilderChain cutEndPoint() {
        return new FFmpegCutEndPointCommandBuilder();
    }

    public FFmpegCommandBuilderChain audioBassBoost() {
        return new FFmpegAudioBassBoostCommandBuilder();
    }

    public FFmpegCommandBuilderChain singleLoop() {
        return new FFmpegSingleLoopCommandBuilder();
    }

    public FFmpegCommandBuilderChain vaiMake() {
        return new FFmpegVaiMakeCommandBuilder(fFprobeDevice);
    }

    public FFmpegCommandBuilderChain singleFramerate() {
        return new FFmpegSingleFramerateCommandBuilder();
    }
}
