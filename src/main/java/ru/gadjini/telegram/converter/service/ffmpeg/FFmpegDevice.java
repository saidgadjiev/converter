package ru.gadjini.telegram.converter.service.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.process.FFmpegProcessExecutor;
import ru.gadjini.telegram.smart.bot.commons.service.process.FFmpegProgressCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FFmpegDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegDevice.class);

    private static final Pattern DECODER_NOT_FOUND_PATTERN = Pattern.compile(".*Decoder \\(.*\\) not found for input stream.*", Pattern.DOTALL);

    private static final Pattern BITRATE_PATTERN = Pattern.compile("(?<=bitrate: )[\\d:.]*");

    private ProcessExecutor processExecutor;

    private FFmpegProcessExecutor fFmpegProcessExecutor;

    @Autowired
    public FFmpegDevice(ProcessExecutor processExecutor, FFmpegProcessExecutor fFmpegProcessExecutor) {
        this.processExecutor = processExecutor;
        this.fFmpegProcessExecutor = fFmpegProcessExecutor;
    }

    public Integer getOverallBitrate(String in) throws InterruptedException {
        String result = processExecutor.tryExecute(getBitrateCommand(in), 3);

        Matcher matcher = BITRATE_PATTERN.matcher(result);
        if (matcher.find()) {
            try {
                return Integer.valueOf(result.substring(matcher.start(), matcher.end()));
            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }
        }

        return null;
    }

    public void convert(String in, String out, String... options) throws InterruptedException {
        processExecutor.execute(getConvertCommand(in, out, options), Set.of(139));
    }

    public void execute(String... command) throws InterruptedException {
        processExecutor.execute(command, Set.of(139));
    }

    public void execute(String[] command, FFmpegProgressCallback progressCallback) throws InterruptedException {
        if (progressCallback == null) {
            execute(command);
        } else {
            fFmpegProcessExecutor.execute(command, progressCallback);
        }
    }

    public boolean isChannelMapError(String... command) throws InterruptedException {
        String result = processExecutor.tryExecute(command, 6);

        return result.contains("Invalid channel layout 5.1(side)");
    }

    public boolean isExecutable(String... command) throws InterruptedException {
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

    private String[] getBitrateCommand(String in) {
        return new String[] {
                "ffmpeg", "-y", "-i", in
        };
    }

    private boolean isOkay(String result) {
        return !result.contains("Conversion failed!")
                && !result.contains("Timestamps are unset in a packet for stream")
                && !result.contains("Non-monotonous DTS in output stream")
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
