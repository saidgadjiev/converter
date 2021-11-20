package ru.gadjini.telegram.converter.service.conversion.ffmpeg.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.converter.service.command.FFmpegCommandBuilderFactory;
import ru.gadjini.telegram.converter.service.ffmpeg.FFmpegDevice;
import ru.gadjini.telegram.converter.service.ffmpeg.FFprobeDevice;
import ru.gadjini.telegram.converter.service.stream.FFmpegConversionContext;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.utils.NumberUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FFmpegVideoStreamConversionHelper {

    private FFmpegDevice fFmpegDevice;

    private FFmpegCommandBuilderFactory commandBuilderFactory;

    private FFmpegImageStreamDetector imageStreamDetector;

    private FFmpegVideoStreamDetector videoStreamDetector;

    @Autowired
    public FFmpegVideoStreamConversionHelper(FFmpegDevice fFmpegDevice,
                                             FFmpegImageStreamDetector imageStreamDetector, FFmpegVideoStreamDetector videoStreamDetector) {
        this.fFmpegDevice = fFmpegDevice;
        this.imageStreamDetector = imageStreamDetector;
        this.videoStreamDetector = videoStreamDetector;
    }

    @Autowired
    public void setCommandBuilderFactory(FFmpegCommandBuilderFactory commandBuilderFactory) {
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
                if (!videoStream.isDontCopy()
                        && (videoStream.getTargetBitrate() == null || Objects.equals(videoStream.getBitRate(), videoStream.getTargetBitrate()))
                        && isCopyableVideoCodecs(baseCommand, conversionContext.output(), videoStream.getInput(), videoStreamMapIndex)) {
                    commandBuilder.copyVideo(outCodecIndex);
                    copied = true;
                } else {
                    String h264Scale = StringUtils.defaultIfBlank(videoStream.getTargetScale(), FFmpegCommand.EVEN_SCALE);
                    boolean convertibleToH264 = isConvertibleToH264(baseCommand, conversionContext.output(),
                            videoStream.getInput(), videoStreamMapIndex, h264Scale);
                    if (!addFastestVideoCodec(commandBuilder, videoStream, outCodecIndex,
                            convertibleToH264, h264Scale)) {
                        if (StringUtils.isNotBlank(videoStream.getTargetCodecName())) {
                            commandBuilder.videoCodec(outCodecIndex, videoStream.getTargetCodecName());
                        } //TODO: Если надо сохранять текущий кодек видео
                        commandBuilder.filterVideo(videoStream.getTargetScale());
                    }
                }
            }
            if (!copied) {
                if (videoStream.getTargetBitrate() != null) {
                    commandBuilder.keepVideoBitRate(outCodecIndex, videoStream.getTargetBitrate());
                } else {
                    commandBuilder.keepVideoBitRate(outCodecIndex, videoStream.getBitRate());
                }
            }
            ++outCodecIndex;
        }
    }

    private boolean isCopyableVideoCodecs(FFmpegCommand baseCommand, SmartTempFile out,
                                          Integer input, int videoStreamMapIndex) throws InterruptedException {
        FFmpegCommand command = new FFmpegCommand(baseCommand);

        FFmpegConversionContext conversionContext = FFmpegConversionContext.from(out);
        command.mapVideo(input, videoStreamMapIndex).copy(FFmpegCommand.VIDEO_STREAM_SPECIFIER);
        commandBuilderFactory.fastVideoConversionAndDefaultOptions().prepareCommand(command, conversionContext);
        commandBuilderFactory.output().prepareCommand(command, conversionContext);

        return fFmpegDevice.isExecutable(command.toCmd());
    }

    public boolean isConvertibleToH264(FFmpegCommand baseCommand, SmartTempFile out,
                                       Integer input, int videoStreamMapIndex, String scale) throws InterruptedException {
        FFmpegCommand command = new FFmpegCommand(baseCommand);

        command.mapVideo(input, videoStreamMapIndex).videoCodec(FFmpegCommand.H264_CODEC).filterVideo(scale);
        FFmpegConversionContext conversionContext = FFmpegConversionContext.from(out);
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
            if (imageStreamDetector.isImageStream(stream)) {
                continue;
            }
            overallBitrate += stream.getBitRate();
        }

        return overallBitrate;
    }

    public boolean isExtraVideoStream(List<FFprobeDevice.FFProbeStream> videoStreams, FFprobeDevice.FFProbeStream videoStream) {
        return videoStreamDetector.isExtraVideoStream(videoStreams, videoStream);
    }

    public int getFirstVideoStreamIndex(List<FFprobeDevice.FFProbeStream> allStreams) {
        return videoStreamDetector.getFirstVideoStreamIndex(allStreams);
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
}
