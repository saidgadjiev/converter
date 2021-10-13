package ru.gadjini.telegram.converter.service.stream;

import org.springframework.stereotype.Component;
import ru.gadjini.telegram.converter.service.conversion.impl.videoeditor.VideoEditorStreamProcessor;

@Component
public class StreamProcessorFactory {

    public StreamProcessor telegramVideoProcessor() {
        return new TelegramVideoStreamProcessor();
    }

    public StreamProcessor videoEditorProcessor() {
        return new VideoEditorStreamProcessor();
    }
}
