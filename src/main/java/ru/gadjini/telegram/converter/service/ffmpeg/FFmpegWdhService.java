package ru.gadjini.telegram.converter.service.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.command.FFmpegCommand;
import ru.gadjini.telegram.smart.bot.commons.service.Jackson;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.List;

@Component
public class FFmpegWdhService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegWdhService.class);

    private ProcessExecutor processExecutor;

    private Jackson jsonMapper;

    @Autowired
    public FFmpegWdhService(ProcessExecutor processExecutor, Jackson jsonMapper) {
        this.processExecutor = processExecutor;
        this.jsonMapper = jsonMapper;
    }

    public FFprobeDevice.WHD getWHD(String in, int index) throws InterruptedException {
        return getWHD(in, index, false);
    }

    public FFprobeDevice.WHD getWHD(String in, int index, boolean throwEx) throws InterruptedException {
        FFprobeDevice.WHD whd = new FFprobeDevice.WHD();
        try {
            FFprobeDevice.FFprobeResult probeVideoStream = probeVideoStream(in, index);
            if (probeVideoStream != null) {
                FFprobeDevice.FFProbeStream videoStream = probeVideoStream.getFirstStream();
                if (videoStream != null) {
                    whd.setWidth(videoStream.getWidth());
                    whd.setHeight(videoStream.getHeight());
                }
                FFprobeDevice.FFprobeFormat fFprobeFormat = probeVideoStream.getFormat();
                if (fFprobeFormat != null) {
                    Long duration = probeVideoStream.getFormat().getDuration();
                    whd.setDuration(duration);
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            if (throwEx) {
                throw e;
            } else {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return whd;
    }

    private FFprobeDevice.FFprobeResult probeVideoStream(String in, int index) throws InterruptedException {
        String result = processExecutor.executeWithResult(getProbeVideoStreamsCommand(in, index));
        return jsonMapper.readValue(result, FFprobeDevice.FFprobeResult.class);
    }

    private String[] getProbeVideoStreamsCommand(String in, Integer streamIndex) {
        List<String> command = new ArrayList<>();
        command.add("ffprobe");
        command.add("-v");
        command.add("error");
        command.add("-select_streams");
        command.add(FFmpegCommand.VIDEO_STREAM_SPECIFIER + ":" + streamIndex);
        command.add("-show_entries");
        command.add("stream=index,codec_name,codec_type,width,height:stream_tags=language,mimetype,filename:format=duration");
        command.add("-of");
        command.add("json");
        command.add(in);

        return command.toArray(String[]::new);
    }
}
