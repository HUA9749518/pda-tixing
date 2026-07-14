package com.pda.alertrelay.model;

public class AlertRecord {

    public final long timestamp;
    public final String title;
    public final String text;

    public AlertRecord(long timestamp, String title, String text) {
        this.timestamp = timestamp;
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
    }
}
