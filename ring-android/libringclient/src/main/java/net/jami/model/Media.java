package net.jami.model;

import java.util.Map;

public class Media {

    enum MediaType {
        AUDIO,
        VIDEO
    }

    private final String source;
    private final MediaType mediaType;
    private final String label;
    private final boolean enabled;
    private final boolean onHold;
    private final boolean muted;

    public Media(Map<String, String> mediaMap) {
        this.source = mediaMap.get("SOURCE");
        this.mediaType = parseMediaType(mediaMap.get("MEDIA_TYPE"));
        this.label = mediaMap.get("LABEL");
        this.enabled = Boolean.parseBoolean(mediaMap.get("ENABLED"));
        this.onHold = Boolean.parseBoolean(mediaMap.get("ON_HOLD"));
        this.muted = Boolean.parseBoolean(mediaMap.get("MUTED"));
    }

    private MediaType parseMediaType(String mediaType) {
        if (mediaType.contains("MEDIA_TYPE_VIDEO"))
            return MediaType.VIDEO;
        return MediaType.AUDIO;
    }

    public String getSource() {
        return source;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isOnHold() {
        return onHold;
    }

    public boolean isMuted() {
        return muted;
    }
}
