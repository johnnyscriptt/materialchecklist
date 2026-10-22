package io.github.johnnyscriptt.materialchecklist.net;

/**
 * Size limits on everything the payloads carry, shared by both sides so a well-behaved client
 * can never send what the server would refuse to decode.
 */
public final class NetLimits {
    /** Project ids are UUIDs (36 chars); a little slack for hand-edited files. */
    public static final int PROJECT_ID = 64;
    /** Items a single list may ask about. Litematica lists for big builds run to a few hundred. */
    public static final int ITEMS = 1024;
    public static final int DIMENSION = 128;

    private NetLimits() {
    }
}
