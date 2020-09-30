package ru.gadjini.telegram.converter.service.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.gadjini.telegram.smart.bot.commons.io.SmartTempFile;
import ru.gadjini.telegram.smart.bot.commons.service.TempFileService;
import ru.gadjini.telegram.smart.bot.commons.service.format.Format;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArchiveService {

    private static final String TAG = "archive";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    private Set<ArchiveDevice> archiveDevices;

    private TempFileService fileService;

    @Autowired
    public ArchiveService(Set<ArchiveDevice> archiveDevices, TempFileService fileService) {
        this.archiveDevices = archiveDevices;
        this.fileService = fileService;
    }

    public SmartTempFile createArchive(int userId, List<File> files, Format archiveFormat) {
        SmartTempFile archive = fileService.getTempFile(userId, TAG, archiveFormat.getExt());
        ArchiveDevice archiveDevice = getCandidate(archiveFormat);
        archiveDevice.zip(files.stream().map(File::getAbsolutePath).collect(Collectors.toList()), archive.getAbsolutePath());

        return archive;
    }

    private ArchiveDevice getCandidate(Format format) {
        for (ArchiveDevice archiveDevice : archiveDevices) {
            if (archiveDevice.accept(format)) {
                return archiveDevice;
            }
        }

        LOGGER.warn("No candidate({})", format);
        throw new IllegalArgumentException("No candidate " + format);
    }
}
