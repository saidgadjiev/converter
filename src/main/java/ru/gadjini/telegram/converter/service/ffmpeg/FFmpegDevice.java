package ru.gadjini.telegram.converter.service.ffmpeg;

import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FFmpegDevice {

    public void convert(String in, String out, String ... options) {
        new ProcessExecutor().execute(getConvertCommand(in, out, options));
    }

    private String[] getConvertCommand(String in, String out, String ... options) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-hide_banner");
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(in);
        cmd.addAll(Arrays.asList(options));
        cmd.add(out);

        return cmd.toArray(String[]::new);
    }
}
