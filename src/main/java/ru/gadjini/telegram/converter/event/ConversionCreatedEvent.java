package ru.gadjini.telegram.converter.event;

public class ConversionCreatedEvent {

    public ConversionCreatedEvent(int queueItemId) {
        this.queueItemId = queueItemId;
    }

    private int queueItemId;

    public int getQueueItemId() {
        return queueItemId;
    }

    public void setQueueItemId(int queueItemId) {
        this.queueItemId = queueItemId;
    }
}
