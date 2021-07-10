package ru.gadjini.telegram.converter.domain;

public class ConversionReport {

    public static final String TYPE = "conversion_report";

    public static final String ID = "id";

    public static final String USER_ID = "user_id";

    public static final String QUEUE_ITEM_ID = "queue_item_id";

    private int id;

    private long userId;

    private int queueItemId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getQueueItemId() {
        return queueItemId;
    }

    public void setQueueItemId(int queueItemId) {
        this.queueItemId = queueItemId;
    }
}
