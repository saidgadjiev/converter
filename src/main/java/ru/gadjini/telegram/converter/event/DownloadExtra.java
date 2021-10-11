package ru.gadjini.telegram.converter.event;

import ru.gadjini.telegram.smart.bot.commons.domain.TgFile;

import java.util.List;

public class DownloadExtra {

    private List<TgFile> files;

    private int currentFileIndex;

    public DownloadExtra() {

    }

    public DownloadExtra(List<TgFile> files, int currentFileIndex) {
        this.files = files;
        this.currentFileIndex = currentFileIndex;
    }

    public List<TgFile> getFiles() {
        return files;
    }

    public int getCurrentFileIndex() {
        return currentFileIndex;
    }
}
