package ru.gadjini.telegram.converter.service.ffmpeg;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.service.ProcessExecutor;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FFmpegDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegDevice.class);

    @Value("${ffmpeg.logging.dir}")
    private String loggingDir;

    @PostConstruct
    public void init() {
        LOGGER.debug("FFmpeg logging dir " + loggingDir);
        try {
            File loggingDirFile = new File(loggingDir);
            if (!loggingDirFile.exists()) {
                Files.createDirectory(loggingDirFile.toPath());
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void convert(String in, String out, String... options) {
        new ProcessExecutor().executeWithRedirectError(getConvertCommand(in, out, options), getLogFile(out));
    }

    private String getLogFile(String out) {
        String logFileName = FilenameUtils.getBaseName(out) + ".txt";
        File logFile = new File(loggingDir, logFileName);
        try {
            Files.createFile(logFile.toPath());

            return logFile.getAbsolutePath();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
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
