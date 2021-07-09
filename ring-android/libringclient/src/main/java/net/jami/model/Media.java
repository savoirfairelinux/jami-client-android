package net.jami.model;

import java.util.HashMap;
import java.util.Map;

public class Media {

    private static final String SOURCE_KEY = "SOURCE";
    private static final String MEDIA_TYPE_KEY = "MEDIA_TYPE";
    private static final String LABEL_KEY = "LABEL";
    private static final String ENABLED_KEY = "ENABLED";
    private static final String ON_HOLD_KEY = "ON_HOLD";
    private static final String MUTED_KEY = "MUTED";

    public enum MediaType {
        MEDIA_TYPE_AUDIO,
        MEDIA_TYPE_VIDEO;

        static MediaType parseMediaType(String mediaType) {
            for (MediaType mt : values()) {
                if (mt.name().contains(mediaType)) {
                    return mt;
                }
            }
            return null;
        }

        static String getMediaTypeString(MediaType mediaType) {
            if (mediaType == null) return "NULL";
            return mediaType.name();
        }
    }

    private final String source;
    private final MediaType mediaType;
    private final String label;
    private final boolean enabled;
    private final boolean onHold;
    private boolean muted;

    public Media(String source, MediaType mediaType, String label, boolean enabled, boolean onHold, boolean muted) {
        this.source = source;
        this.mediaType = mediaType;
        this.label = label;
        this.enabled = enabled;
        this.onHold = onHold;
        this.muted = muted;
    }

    public Media(Map<String, String> mediaMap) {
        this.source = mediaMap.get(SOURCE_KEY);
        this.mediaType = MediaType.parseMediaType(mediaMap.get(MEDIA_TYPE_KEY));
        this.label = mediaMap.get(LABEL_KEY);
        this.enabled = Boolean.parseBoolean(mediaMap.get(ENABLED_KEY));
        this.onHold = Boolean.parseBoolean(mediaMap.get(ON_HOLD_KEY));
        this.muted = Boolean.parseBoolean(mediaMap.get(MUTED_KEY));
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

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public Map<String, String> toMap() {
        Map<String, String> stringStringMap = new HashMap<>();
        stringStringMap.put(SOURCE_KEY, this.source);
        stringStringMap.put(MEDIA_TYPE_KEY, MediaType.getMediaTypeString(this.mediaType));
        stringStringMap.put(LABEL_KEY, this.label);
        stringStringMap.put(ENABLED_KEY, Boolean.toString(this.enabled));
        stringStringMap.put(ON_HOLD_KEY, Boolean.toString(this.onHold));
        stringStringMap.put(MUTED_KEY, Boolean.toString(this.muted));
        return stringStringMap;
    }
}
