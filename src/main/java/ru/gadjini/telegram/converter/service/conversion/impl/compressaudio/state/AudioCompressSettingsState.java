package ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.state;

import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.AudioCompressionMode;

public interface AudioCompressSettingsState {

    void mode(AudioCompressionMode audioCompressionMode);

    void bitrate(String bitrate);

    void goBack();
}
