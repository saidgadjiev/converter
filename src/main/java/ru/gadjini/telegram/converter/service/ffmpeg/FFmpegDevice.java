package ru.gadjini.telegram.converter.service.ffmpeg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class FFmpegDevice {

    private ProcessExecutor processExecutor;

    @Autowired
    public FFmpegDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public void convert(String in, String out, String... options) {
        processExecutor.execute(getConvertCommand(in, out, options), Set.of(139));
    }

    public boolean isConvertable(String in, String out, String... options) {
        String result = processExecutor.tryExecute(getConvertCommand(in, out, options), 3);

        return !result.contains("Conversion failed!")
                && !result.contains("Unsupported audio codec")
                && !result.contains("Could not find tag for codec")
                && !result.contains("incompatible with output codec")
                && !result.contains("Error initializing output stream")
                && !result.contains("Error selecting an encoder for stream");
    }

    private String[] getConvertCommand(String in, String out, String... options) {
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
