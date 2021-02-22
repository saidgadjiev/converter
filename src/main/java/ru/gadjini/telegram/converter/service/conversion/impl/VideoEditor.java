package ru.gadjini.telegram.converter.service.conversion.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.command.keyboard.start.SettingsState;
import ru.gadjini.telegram.converter.domain.ConversionQueueItem;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilder;
import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.FileResult;
import ru.gadjini.telegram.converter.service.conversion.common.FFmpegHelper;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.utils.Any2AnyFileNameUtils;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;

import java.util.*;
import java.util.stream.Collectors;

import static ru.gadjini.telegram.smart.bot.commons.service.format.Format.*;

@Component
public class VideoEditor extends BaseAny2AnyConverter {

    private static final String TAG = "vedit";

    private static final Map<List<Format>, List<Format>> MAP = Map.of(
            Format.filter(FormatCategory.VIDEO), List.of(EDIT_VIDEO)
    );

    private FFprobeDevice fFprobeDevice;

    private FFmpegDevice fFmpegDevice;

    private FFmpegHelper fFmpegHelper;

    private Gson gson;

    @Autowired
    public VideoEditor(FFprobeDevice fFprobeDevice, FFmpegDevice fFmpegDevice, FFmpegHelper fFmpegHelper, Gson gson) {
        super(MAP);
        this.fFprobeDevice = fFprobeDevice;
        this.fFmpegDevice = fFmpegDevice;
        this.fFmpegHelper = fFmpegHelper;
        this.gson = gson;
    }

    @Override
    protected ConvertResult doConvert(ConversionQueueItem fileQueueItem) {
        SmartTempFile file = fileQueueItem.getDownloadedFile(fileQueueItem.getFirstFileId());

        SmartTempFile out = getFileService().createTempFile(fileQueueItem.getUserId(), fileQueueItem.getFirstFileId(), TAG, fileQueueItem.getFirstFileFormat().getExt());
        try {
            List<FFprobeDevice.Stream> allStreams = fFprobeDevice.getAllStreams(file.getAbsolutePath());
            FFmpegHelper.removeExtraVideoStreams(allStreams);
            SettingsState settingsState = gson.fromJson((JsonElement) fileQueueItem.getExtra(), SettingsState.class);

            FFmpegCommandBuilder commandBuilder = new FFmpegCommandBuilder();

            List<FFprobeDevice.Stream> videoStreams = allStreams.stream()
                    .filter(s -> FFprobeDevice.Stream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                    .collect(Collectors.toList());

            String height = settingsState.getResolution().replace("p", "");
            String scale = "scale=-2:" + height;
            for (int videoStreamIndex = 0; videoStreamIndex < videoStreams.size(); videoStreamIndex++) {
                commandBuilder.mapVideo(videoStreamIndex);
                fFmpegHelper.addFastestVideoCodecOptions(commandBuilder, file, out, videoStreams.get(videoStreamIndex), videoStreamIndex, scale);
                commandBuilder.filterVideo(videoStreamIndex, scale);
            }
            if (allStreams.stream().anyMatch(stream -> FFprobeDevice.Stream.AUDIO_CODEC_TYPE.equals(stream.getCodecType()))) {
                commandBuilder.mapAudio().copyAudio();
            }
            if (allStreams.stream().anyMatch(s -> FFprobeDevice.Stream.SUBTITLE_CODEC_TYPE.equals(s.getCodecType()))) {
                if (fFmpegHelper.isSubtitlesCopyable(file, out)) {
                    commandBuilder.mapSubtitles().copySubtitles();
                } else if (FFmpegHelper.isSubtitlesSupported(fileQueueItem.getFirstFileFormat())) {
                    commandBuilder.mapSubtitles();
                    FFmpegHelper.addSubtitlesCodec(commandBuilder, fileQueueItem.getFirstFileFormat());
                }
            }
            commandBuilder.preset(FFmpegCommandBuilder.PRESET_VERY_FAST);
            commandBuilder.deadline(FFmpegCommandBuilder.DEADLINE_REALTIME);
            fFmpegHelper.addTargetFormatOptions(commandBuilder, fileQueueItem.getFirstFileFormat());

            fFmpegDevice.convert(file.getAbsolutePath(), out.getAbsolutePath(), commandBuilder.build());

            String fileName = Any2AnyFileNameUtils.getFileName(fileQueueItem.getFirstFileName(), fileQueueItem.getFirstFileFormat().getExt());

            return new FileResult(fileName, out, downloadThumb(fileQueueItem));
        } catch (Throwable e) {
            out.smartDelete();
            throw e;
        }
    }
}
