package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatCategory;
import ru.gadjini.telegram.smart.bot.commons.service.format.FormatService;
import ru.gadjini.telegram.smart.bot.commons.utils.NumberUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FFmpegVideoStreamConversionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegVideoStreamConversionHelper.class);

    private static final Set<String> IMAGE_CODECS = Set.of(FFmpegCommand.BMP, FFmpegCommand.PNG, FFmpegCommand.MJPEG);

    private FFmpegDevice fFmpegDevice;

    private FormatService formatService;

    private FFmpegCommandBuilderFactory commandBuilderFactory;

    @Autowired
    public FFmpegVideoStreamConversionHelper(FFmpegDevice fFmpegDevice, FormatService formatService,
                                             FFmpegCommandBuilderFactory commandBuilderFactory) {
        this.fFmpegDevice = fFmpegDevice;
        this.formatService = formatService;

        this.commandBuilderFactory = commandBuilderFactory;
    }

    public boolean isVideoStreamsValidForTelegramVideo(List<FFprobeDevice.FFProbeStream> allStreams) {
        List<FFprobeDevice.FFProbeStream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        return videoStreams.stream().allMatch(v -> FFmpegCommand.H264_CODEC.equals(v.getCodecName()));
    }

    public void copyOrConvertVideoCodecs(FFmpegCommand commandBuilder, FFmpegConversionContext conversionContext) throws InterruptedException {
        List<FFprobeDevice.FFProbeStream> videoStreams = conversionContext.videoStreams();

        FFmpegCommand baseCommand = new FFmpegCommand(commandBuilder);
        int outCodecIndex = 0;
        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.FFProbeStream videoStream = videoStreams.get(videoStreamMapIndex);
            if (isExtraVideoStream(videoStreams, videoStream)) {
                continue;
            }
            commandBuilder.mapVideo(videoStream.getInput(), videoStreamMapIndex);
            boolean copied = false;
            if (StringUtils.isNotBlank(videoStream.getTargetCodecName())
                    && !Objects.equals(videoStream.getTargetCodecName(), videoStream.getCodecName())) {
                commandBuilder.videoCodec(outCodecIndex, videoStream.getTargetCodecName())
                        .filterVideo(outCodecIndex, videoStream.getTargetScale());
            } else {
                if (isCopyableVideoCodecs(baseCommand, conversionContext.output(), videoStream.getInput(), videoStreamMapIndex)) {
                    commandBuilder.copyVideo(outCodecIndex);
                    copied = true;
                } else {
                    boolean convertibleToH264 = isConvertibleToH264(baseCommand, conversionContext.output(),
                            videoStream.getInput(), videoStreamMapIndex, videoStream.getTargetScale());
                    if (!addFastestVideoCodec(commandBuilder, videoStream, outCodecIndex,
                            convertibleToH264, videoStream.getTargetScale())) {
                        if (StringUtils.isNotBlank(videoStream.getTargetCodecName())) {
                            commandBuilder.videoCodec(outCodecIndex, videoStream.getTargetCodecName());
                        }
                        commandBuilder.filterVideo(videoStream.getTargetScale());
                    }
                }
            }
            if (!copied) {
                commandBuilder.keepVideoBitRate(videoStream.getBitRate(), outCodecIndex);
            }
            ++outCodecIndex;
        }
    }

    private boolean isCopyableVideoCodecs(FFmpegCommand baseCommand, SmartTempFile out,
                                          Integer input, int videoStreamMapIndex) throws InterruptedException {
        FFmpegCommand command = new FFmpegCommand(baseCommand);

        FFmpegConversionContext conversionContext = new FFmpegConversionContext().output(out);
        command.mapVideo(input, videoStreamMapIndex).copy(FFmpegCommand.VIDEO_STREAM_SPECIFIER);
        commandBuilderFactory.fastVideoConversionAndDefaultOptions().prepareCommand(command, conversionContext);
        commandBuilderFactory.output().prepareCommand(command, conversionContext);

        return fFmpegDevice.isExecutable(command.toCmd());
    }

    public boolean isConvertibleToH264(FFmpegCommand baseCommand, SmartTempFile out,
                                       Integer input, int videoStreamMapIndex, String scale) throws InterruptedException {
        FFmpegCommand command = new FFmpegCommand(baseCommand);

        command.mapVideo(input, videoStreamMapIndex).videoCodec(FFmpegCommand.H264_CODEC).filterVideo(scale);
        FFmpegConversionContext conversionContext = new FFmpegConversionContext().output(out);
        commandBuilderFactory.output().prepareCommand(command, conversionContext);

        return fFmpegDevice.isExecutable(command.toCmd());
    }

    public boolean addFastestVideoCodec(FFmpegCommand commandBuilder, FFprobeDevice.FFProbeStream videoStream,
                                        int videoStreamIndex, boolean convertibleToH264, String h264Scale) {
        if (StringUtils.isBlank(videoStream.getCodecName())) {
            return false;
        }
        if (convertibleToH264) {
            commandBuilder.videoCodec(videoStreamIndex, FFmpegCommand.H264_CODEC);
            addScaleFilterForH264(commandBuilder, videoStream, videoStreamIndex, h264Scale);
            return true;
        } else if (FFmpegCommand.VP9_CODEC.equals(videoStream.getCodecName())) {
            commandBuilder.videoCodec(videoStreamIndex, FFmpegCommand.VP8_CODEC);
            return true;
        } else if (FFmpegCommand.AV1_CODEC.equals(videoStream.getCodecName())) {
            commandBuilder.videoCodec(videoStreamIndex, FFmpegCommand.VP8_CODEC);
            return true;
        } else {
            return false;
        }
    }

    public Integer getOverallBitrate(List<FFprobeDevice.FFProbeStream> streams) {
        Integer overallBitrate = 0;
        for (FFprobeDevice.FFProbeStream stream : streams) {
            if (isImageStream(stream)) {
                continue;
            }
            overallBitrate += stream.getBitRate();
        }

        return overallBitrate;
    }

    public boolean isExtraVideoStream(List<FFprobeDevice.FFProbeStream> videoStreams, FFprobeDevice.FFProbeStream videoStream) {
        if (videoStreams.size() == 1 && isImageStream(videoStream)) {
            return false;
        }
        if (videoStreams.stream().allMatch(this::isImageStream)) {
            return false;
        }

        return isImageStream(videoStream);
    }

    public int getFirstVideoStreamIndex(List<FFprobeDevice.FFProbeStream> allStreams) {
        List<FFprobeDevice.FFProbeStream> videoStreams = allStreams.stream()
                .filter(s -> FFprobeDevice.FFProbeStream.VIDEO_CODEC_TYPE.equals(s.getCodecType()))
                .collect(Collectors.toList());

        for (int videoStreamMapIndex = 0; videoStreamMapIndex < videoStreams.size(); ++videoStreamMapIndex) {
            FFprobeDevice.FFProbeStream videoStream = videoStreams.get(videoStreamMapIndex);
            if (!isExtraVideoStream(videoStreams, videoStream)) {
                return videoStreamMapIndex;
            }
        }

        return 0;
    }

    private void addScaleFilterForH264(FFmpegCommand commandBuilder, FFprobeDevice.FFProbeStream stream,
                                       int codecIndex, String scale) {
        if (StringUtils.isNotBlank(scale)
                && (!FFmpegCommand.EVEN_SCALE.equals(scale)
                || !NumberUtils.isEvent(stream.getWidth()) || !NumberUtils.isEvent(stream.getHeight()))) {
            if (commandBuilder.useFilterComplex()) {
                commandBuilder.complexFilter("[v:" + codecIndex + "]" + scale + "[sv] ");
            } else {
                commandBuilder.filterVideo(codecIndex, scale);
            }
        }
    }

    private boolean isImageStream(FFprobeDevice.FFProbeStream stream) {
        if (StringUtils.isNotBlank(stream.getCodecName()) && IMAGE_CODECS.contains(stream.getCodecName())) {
            return true;
        }
        try {
            Format format = formatService.getFormat(stream.getFileName(), stream.getMimeType());

            if (format != null && format.getCategory() == FormatCategory.IMAGES) {
                return true;
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage());
        }

        return false;
    }
}
