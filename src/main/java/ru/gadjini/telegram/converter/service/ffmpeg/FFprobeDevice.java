package ru.gadjini.telegram.converter.service.ffmpeg;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

@Service
public class FFprobeDevice {

    public String getVideoCodec(String in) {
        return new ProcessExecutor().executeWithResult(getCommand(in));
    }

    private String[] getCommand(String in) {
        return new String[] {
                "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=codec_name", "-of", "default=noprint_wrappers=1:nokey=1", in
        };
    }
}

