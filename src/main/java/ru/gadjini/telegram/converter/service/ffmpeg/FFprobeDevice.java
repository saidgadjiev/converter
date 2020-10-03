package ru.gadjini.telegram.converter.service.ffmpeg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

@Service
public class FFprobeDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public FFprobeDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public String getVideoCodec(String in) {
        return processExecutor.executeWithResult(getCommand(in));
    }

    public long getDurationInSeconds(String in) {
        String duration = new ProcessExecutor().executeWithResult(getDurationCommand(in));

        return Math.round(Double.parseDouble(duration));
    }

    private String[] getDurationCommand(String in) {
        return new String[]{"ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", in};
    }

    private String[] getCommand(String in) {
        return new String[] {
                "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=codec_name", "-of", "default=noprint_wrappers=1:nokey=1", in
        };
    }
}

