package ru.gadjini.telegram.converter.service.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.exception.ConvertException;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderChain;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory;
import ru.gadjini.telegram.converter.service.conversion.progress.FFmpegProgressCallbackHandlerFactory.FFmpegProgressCallbackHandler;
import ru.gadjini.telegram.converter.service.conversion.result.VideoResultBuilder;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChain;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContextPreparerChainFactory;
import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.UserService;
import ru.gadjini.telegram.smart.bot.commons.service.file.temp.FileTarget;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class VavMergeConverter extends BaseAny2AnyConverter {

    public static final String ADD_AUDIO_MODE = "0";

    public static final String REPLACE_AUDIO_MODE = "1";

    public static final String ADD_SUBTITLES_MODE = "2";

    public static final String REPLACE_SUBTITLES_MODE = "3";

    private static final String TAG = "vavmerge";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            List.of(Format.VIDEOAUDIO), List.of(Format.MERGE)
    );

    private FFmpegDevice fFmpegDevice;

    private FFprobeDevice fFprobeDevice;

    private Jackson jackson;

    private UserService userService;

    private FFmpegProgressCallbackHandlerFactory callbackHandlerFactory;

    private FFmpegCommandBuilderChain commandBuilderChain;

    private FFmpegConversionContextPreparerChain contextPreparerChain;

    private VideoResultBuilder videoResultBuilder;

    @Autowired
    public VavMergeConverter(FFmpegDevice fFmpegDevice, FFprobeDevice fFprobeDevice,
                             Jackson jackson, UserService userService,
                             FFmpegProgressCallbackHandlerFactory callbackHandlerFactory,
                             FFmpegCommandBuilderFactory commandBuilderFactory,
                             FFmpegConversionContextPreparerChainFactory contextPreparerChainFactory,
                             VideoResultBuilder videoResultBuilder) {
        super(MAP);
        this.fFmpegDevice = fFmpegDevice;
        this.fFprobeDevice = fFprobeDevice;
        this.jackson = jackson;
        this.userService = userService;
        this.callbackHandlerFactory = callbackHandlerFactory;

        this.commandBuilderChain = commandBuilderFactory.quiteInput();
        this.videoResultBuilder = videoResultBuilder;
        commandBuilderChain.setNext(commandBuilderFactory.videoConversion())
                .setNext(commandBuilderFactory.vavMerge())
                .setNext(commandBuilderFactory.streamDuration())
                .setNext(commandBuilderFactory.webmQuality())
                .setNext(commandBuilderFactory.fastVideoConversionAndDefaultOptions())
                .setNext(commandBuilderFactory.output());

        this.contextPreparerChain = contextPreparerChainFactory.videoConversionContextPreparer();
    }

    @Override
    public boolean supportsProgress() {
        return true;
    }

    @Override
    public int createDownloads(ConversionQueueItem conversionQueueItem) {
        return super.createDownloadsWithThumb(conversionQueueItem);
    }

    @Override
    protected ConversionResult doConvert(ConversionQueueItem conversionQueueItem) {
        TgFile videoFile = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.VIDEO)
                .findAny().orElseThrow();
        SmartTempFile video = conversionQueueItem.getDownloadedFileOrThrow(videoFile.getFileId());

        List<TgFile> audioFiles = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.AUDIO)
                .collect(Collectors.toList());

        List<SmartTempFile> audios = audioFiles.stream()
                .map(a -> conversionQueueItem.getDownloadedFileOrThrow(a.getFileId()))
                .collect(Collectors.toList());

        List<TgFile> subtitlesFiles = conversionQueueItem.getFiles().stream()
                .filter(f -> f.getFormat().getCategory() == FormatCategory.SUBTITLES)
                .collect(Collectors.toList());

        List<SmartTempFile> subtitles = subtitlesFiles.stream()
                .map(s -> conversionQueueItem.getDownloadedFileOrThrow(s.getFileId()))
                .collect(Collectors.toList());

        Format targetFormat = videoFile.getFormat();
        SmartTempFile result = tempFileService().createTempFile(FileTarget.UPLOAD, conversionQueueItem.getUserId(),
                conversionQueueItem.getFirstFileId(), TAG, targetFormat.getExt());

        try {
            List<FFprobeDevice.FFProbeStream> streams = fFprobeDevice.getAllStreams(video.getAbsolutePath(), FormatCategory.VIDEO);
            streams.forEach(s -> s.setInput(0));

            SettingsState settingsState = jackson.convertValue(conversionQueueItem.getExtra(), SettingsState.class);
            if (!audios.isEmpty()) {
                List<FFprobeDevice.FFProbeStream> audioStreamsForConversion = new ArrayList<>();

                for (int index = 0; index < audios.size(); index++) {
                    SmartTempFile audio = audios.get(index);
                    List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(audio.getAbsolutePath(), FormatCategory.AUDIO);
                    for (FFprobeDevice.FFProbeStream stream : allStreams) {
                        stream.setInput(index + 1);
                    }
                    audioStreamsForConversion.addAll(allStreams);
                }
                if (REPLACE_AUDIO_MODE.equals(settingsState.getVavMergeAudioMode())) {
                    streams.removeIf(f -> f.getCodecType().equals(FFprobeDevice.FFProbeStream.AUDIO_CODEC_TYPE));
                }
                streams.addAll(audioStreamsForConversion);
            }
            if (!subtitles.isEmpty()) {
                int subtitlesInput = audios.size() + 1;
                List<FFprobeDevice.FFProbeStream> subtitlesStreams = new ArrayList<>();

                for (SmartTempFile subtitle : subtitles) {
                    List<FFprobeDevice.FFProbeStream> allStreams = fFprobeDevice.getAllStreams(subtitle.getAbsolutePath(), FormatCategory.SUBTITLES);
                    for (FFprobeDevice.FFProbeStream stream : allStreams) {
                        stream.setInput(subtitlesInput);
                    }
                    subtitlesStreams.addAll(allStreams);
                    ++subtitlesInput;
                }
                if (REPLACE_SUBTITLES_MODE.equals(settingsState.getVavMergeSubtitlesMode())) {
                    streams.removeIf(f -> f.getCodecType().equals(FFprobeDevice.FFProbeStream.SUBTITLE_CODEC_TYPE));
                }
                streams.addAll(subtitlesStreams);
            }

            long durationInSeconds = fFprobeDevice.getDurationInSeconds(video.getAbsolutePath());
            FFmpegConversionContext conversionContext = FFmpegConversionContext.from(video, result, targetFormat, streams)
                    .putExtra(FFmpegConversionContext.AUDIO_STREAMS_COUNT, audios.size())
                    .putExtra(FFmpegConversionContext.SUBTITLE_STREAMS_COUNT, subtitles.size())
                    .putExtra(FFmpegConversionContext.STREAM_DURATION, durationInSeconds);
            if (!audios.isEmpty()) {
                audios.forEach(conversionContext::input);
            }
            if (!subtitles.isEmpty()) {
                subtitles.forEach(conversionContext::input);
            }
            contextPreparerChain.prepare(conversionContext);

            FFmpegCommand command = new FFmpegCommand();
            commandBuilderChain.prepareCommand(command, conversionContext);

            FFmpegProgressCallbackHandler callback = callbackHandlerFactory.createCallback(conversionQueueItem, durationInSeconds,
                    userService.getLocaleOrDefault(conversionQueueItem.getUserId()));
            fFmpegDevice.execute(command.toCmd(), callback);

            return videoResultBuilder.build(conversionQueueItem, conversionQueueItem.getFirstFileFormat(), result);
        } catch (Throwable e) {
            tempFileService().delete(result);
            throw new ConvertException(e);
        }
    }
}
