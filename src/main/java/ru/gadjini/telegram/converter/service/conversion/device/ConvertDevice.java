package ru.gadjini.telegram.converter.service.conversion.device;

public interface ConvertDevice {

    void convert(String in, String out, String... options) throws InterruptedException;
}
