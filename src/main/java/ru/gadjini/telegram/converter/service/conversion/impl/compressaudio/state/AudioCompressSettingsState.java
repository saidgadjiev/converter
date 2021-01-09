package ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.state;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.gadjini.telegram.converter.service.conversion.impl.compressaudio.AudioCompressionMode;

public interface AudioCompressSettingsState {

    AudioCompressSettingsStateName getName();

    default void mode(CallbackQuery callbackQuery, AudioCompressionMode audioCompressionMode) {

    }

    default void bitrate(long chatId, String bitrate) {

    }

    default void enter(long chatId) {

    }

    default void goBack(long chatId) {

    }
}
