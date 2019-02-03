package com.example.android.bluetoothlegatt.starcom;

import java.util.UUID;

public enum StarcomUUID {
    SERVICE("UART_SERVICE_UUID","SERVICE",UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")),
    READ("UART_CHAR_TX_UUID","READ", UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")),
    WRITE("UART_CHAR_RX_UUID", "WRITE",UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));

    private String mLabel;
    private String mNickname;
    private UUID mUUID;

    private static final String TAG = StarcomUUID.class.getSimpleName();

    StarcomUUID(String mLabel, String mNickname,UUID mUUID ) {
        this.mLabel = mLabel;
        this.mUUID = mUUID;
        this.mNickname = mNickname;
    }

    public static StarcomUUID getStarcomUUIDFromLabel(String mLabel){
        StarcomUUID[] starcomUUIDS = StarcomUUID.values();
        for (StarcomUUID starcomUUID : starcomUUIDS) {
            if (starcomUUID != null) {
                if (starcomUUID.getmLabel().equals(mLabel)) {
                    return starcomUUID;
                }
            }
        }
        return null;
    }

    public static String getStarcomUUIDFromUUID(UUID mUUID){
        StarcomUUID[] starcomUUIDS = StarcomUUID.values();
        for (StarcomUUID starcomUUID : starcomUUIDS) {
            if (starcomUUID != null) {
                if (starcomUUID.getmUUID().equals(mUUID)) {
                    return starcomUUID.toString();
                }
            }
        }
        return "";
    }

    public static String getStarcomUUIDFromNickname(String mNickname){
        StarcomUUID[] starcomUUIDS = StarcomUUID.values();
        for (StarcomUUID starcomUUID : starcomUUIDS) {
            if (starcomUUID != null) {
                if (starcomUUID.getmNickname().equals(mNickname)) {
                    return starcomUUID.toString();
                }
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return this.mLabel;
    }

    public String getmNickname() {
        return mNickname;
    }
    public String getmLabel() {
        return mLabel;
    }
    public UUID getmUUID() {
        return mUUID;
    }
}
