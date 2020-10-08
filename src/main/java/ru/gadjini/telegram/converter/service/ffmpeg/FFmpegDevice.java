package ru.gadjini.telegram.converter.service.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class FFmpegDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegDevice.class);

    private ProcessExecutor processExecutor;

    @Value("${ffmpeg.threads:2}")
    private int ffmpegThreads;

    @PostConstruct
    public void init() {
        LOGGER.debug("Ffmpeg threads({})", ffmpegThreads);
    }

    @Autowired
    public FFmpegDevice(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public void convert(String in, String out, String... options) {
        processExecutor.execute(getConvertCommand(in, out, options), Set.of(139));
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
