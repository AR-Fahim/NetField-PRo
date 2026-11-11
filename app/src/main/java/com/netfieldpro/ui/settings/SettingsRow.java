package com.netfieldpro.ui.settings;

public class SettingsRow {

    private final String id;
    private final String title;
    private final String subtitle;
    private final Runnable onClick;
    private final boolean visible;

    public SettingsRow(String id, String title, String subtitle, Runnable onClick, boolean visible) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.onClick = onClick;
        this.visible = visible;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Runnable getOnClick() {
        return onClick;
    }

    public boolean isVisible() {
        return visible;
    }
}

