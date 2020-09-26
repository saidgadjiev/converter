package ru.gadjini.telegram.converter.service.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class FileLg implements Lg {

    private PrintWriter printWriter;

    public FileLg(File file) {
        try {
            printWriter = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void log(String log, Object... args) {
        printWriter.println(String.format(log, args));
    }

    @Override
    public void close() {
        printWriter.close();
    }
}
