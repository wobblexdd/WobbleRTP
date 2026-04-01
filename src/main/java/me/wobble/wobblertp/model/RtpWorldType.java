package me.wobble.wobblertp.model;

public enum RtpWorldType {
    OVERWORLD("Overworld"),
    NETHER("Nether"),
    THE_END("The End");

    private final String displayName;

    RtpWorldType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
