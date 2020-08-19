package ru.gadjini.telegram.converter.io;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class SmartTempFile {

    private File file;

    public SmartTempFile(File file) {
        this.file = file;
    }

    public String getName() {
        return file.getName();
    }

    public String getParent() {
        return file.getParent();
    }

    public File getParentFile() {
        return file.getParentFile();
    }

    public String getPath() {
        return file.getPath();
    }

    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    public File getAbsoluteFile() {
        return file.getAbsoluteFile();
    }

    public String getCanonicalPath() throws IOException {
        return file.getCanonicalPath();
    }

    public File getCanonicalFile() throws IOException {
        return file.getCanonicalFile();
    }

    @Deprecated
    public URL toURL() throws MalformedURLException {
        return file.toURL();
    }

    public URI toURI() {
        return file.toURI();
    }

    public boolean canRead() {
        return file.canRead();
    }

    public boolean canWrite() {
        return file.canWrite();
    }

    public boolean exists() {
        return file.exists();
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public boolean isFile() {
        return file.isFile();
    }

    public boolean isHidden() {
        return file.isHidden();
    }

    public long lastModified() {
        return file.lastModified();
    }

    public long length() {
        return file.length();
    }

    public boolean createNewFile() throws IOException {
        return file.createNewFile();
    }

    public boolean delete() {
        return file.delete();
    }

    public void deleteOnExit() {
        file.deleteOnExit();
    }

    public String[] list() {
        return file.list();
    }

    public String[] list(FilenameFilter filter) {
        return file.list(filter);
    }

    public File[] listFiles() {
        return file.listFiles();
    }

    public File[] listFiles(FilenameFilter filter) {
        return file.listFiles(filter);
    }

    public File[] listFiles(FileFilter filter) {
        return file.listFiles(filter);
    }

    public boolean mkdir() {
        return file.mkdir();
    }

    public boolean mkdirs() {
        return file.mkdirs();
    }

    public boolean renameTo(File dest) {
        return file.renameTo(dest);
    }

    public boolean setLastModified(long time) {
        return file.setLastModified(time);
    }

    public boolean setReadOnly() {
        return file.setReadOnly();
    }

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return file.setWritable(writable, ownerOnly);
    }

    public boolean setWritable(boolean writable) {
        return file.setWritable(writable);
    }

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return file.setReadable(readable, ownerOnly);
    }

    public boolean setReadable(boolean readable) {
        return file.setReadable(readable);
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return file.setExecutable(executable, ownerOnly);
    }

    public boolean setExecutable(boolean executable) {
        return file.setExecutable(executable);
    }

    public boolean canExecute() {
        return file.canExecute();
    }

    public static File[] listRoots() {
        return File.listRoots();
    }

    public long getTotalSpace() {
        return file.getTotalSpace();
    }

    public long getFreeSpace() {
        return file.getFreeSpace();
    }

    public long getUsableSpace() {
        return file.getUsableSpace();
    }

    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        return File.createTempFile(prefix, suffix, directory);
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(prefix, suffix);
    }

    public int compareTo(File pathname) {
        return file.compareTo(pathname);
    }

    public Path toPath() {
        return file.toPath();
    }

    public File getFile() {
        return file;
    }

    public void smartDelete() {
        if (file != null && file.exists()) {
            FileUtils.deleteQuietly(file);
        }
    }
}
