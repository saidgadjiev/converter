package ru.gadjini.telegram.converter.service.ffmpeg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class FFmpegDevice {

    private static final Pattern DECODER_NOT_FOUND_PATTERN = Pattern.compile(".*Decoder \\(.*\\) not found for input stream.*", Pattern.DOTALL);

    private ProcessExecutor processExecutor;

    @Autowired
    public FFmpegDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public void convert(String in, String out, String... options) throws InterruptedException {
        processExecutor.execute(getConvertCommand(in, out, options), Set.of(139));
    }

    public void execute(String ... command) throws InterruptedException {
        processExecutor.execute(command, Set.of(139));
    }

    public boolean isChannelMapError(String ... command) throws InterruptedException {
        String result = processExecutor.tryExecute(command, 6);

        return result.contains("Invalid channel layout 5.1(side)");
    }

    public boolean isExecutable(String ... command) throws InterruptedException {
        String result = processExecutor.tryExecute(command, 6);

        return isOkay(result);
    }

    public boolean isConvertable(String in, String out, String... options) throws InterruptedException {
        String result = processExecutor.tryExecute(getConvertCommand(in, out, options), 6);

        return isOkay(result);
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

    private boolean isOkay(String result) {
        return !result.contains("Conversion failed!")
                && !result.contains("Unsupported audio codec")
                && !result.contains("Could not find tag for codec")
                && !result.contains("Could not write header for output file")
                && !result.contains("incompatible with output codec")
                && !result.contains("Error while opening encoder for output stream")
                && !result.contains("Error initializing output stream")
                && !result.contains("Error selecting an encoder for stream")
                && !result.contains("Invalid data found when processing input")
                && !result.contains("Invalid UTF-8 in decoded subtitles text")
                && !DECODER_NOT_FOUND_PATTERN.matcher(result).matches();
    }
}
