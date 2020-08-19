package ru.gadjini.telegram.converter.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.converter.io.SmartTempFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;

@Service
public class TempFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempFileService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${temp.dir:#{systemProperties['java.io.tmpdir']}}")
    private String tempDir;

    @PostConstruct
    public void init() {
        LOGGER.debug("Temp dir({})", tempDir);
    }

    public String getTempDir() {
        return tempDir;
    }

    public SmartTempFile getTempFile(long chatId, String fileId, String tag, String ext) {
        File file = new File(tempDir, generateName(chatId, fileId, tag, ext));

        LOGGER.debug("Get({})", file.getAbsolutePath());
        return new SmartTempFile(file);
    }

    public SmartTempFile getTempFile(long chatId, String tag, String ext) {
        return getTempFile(chatId, null, tag, ext);
    }

    public SmartTempFile createTempFile(long chatId, String fileId, String tag, String ext) {
        try {
            File file = new File(tempDir, generateName(chatId, fileId, tag, ext));
            Files.createFile(file.toPath());

            LOGGER.debug("Create({}, {}, {})", chatId, fileId, file.getAbsolutePath());
            return new SmartTempFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartTempFile createTempFile(long chatId, String tag, String ext) {
        return createTempFile(chatId, null, tag, ext);
    }

    public SmartTempFile createTempFile(String tag, String ext) {
        return createTempFile(0, null, tag, ext);
    }

    public String generateName(long chatId, String fileId, String tag, String ext) {
        tag = StringUtils.defaultIfBlank(tag, "-");
        ext = StringUtils.defaultIfBlank(ext, "tmp");
        fileId = StringUtils.defaultIfBlank(fileId, "-");
        long n = RANDOM.nextLong();

        return "tag_" + tag + "_chatId_" + chatId + "_fileId_" + fileId + "_time_" + System.nanoTime() + "_salt_" + Long.toUnsignedString(n) + "." + ext;
    }
}
