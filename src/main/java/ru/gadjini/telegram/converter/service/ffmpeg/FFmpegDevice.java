package ru.gadjini.telegram.converter.service.ffmpeg;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

@Service
public class FFmpegDevice {

    public void convert(String in, String out) {
        new ProcessExecutor().execute(getConvertCommand(in, out));
    }

    private String[] getConvertCommand(String in, String out) {
        return new String[]{"ffmpeg", "-hide_banner", "-loglevel", "panic", "-y", "-i", in, out};
    }
}
