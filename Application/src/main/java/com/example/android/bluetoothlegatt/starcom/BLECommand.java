package com.example.android.bluetoothlegatt.starcom;

import android.util.Log;

import static com.example.android.bluetoothlegatt.starcom.Sha256.bytesToHex;

public enum BLECommand {
    ReadVersion("Read version","get_ver"),
    Restart("Restart", "restart"),
    ReadCropXState("Read CropX state", "get_state"),
    CropXStateActive("CropX state: Active", "set_state:1"),
    CropXStateHibernate("CropX state: Hibernate", "set_state:0"),
    CropXForceMeasurement("CropX force measurement", "cropx_measure"),
    CropXGetMoist("CropX get moist", "get_cropx_moist"),
    CropXGetTemp("CropX get temp", "get_cropx_temp"),
    CropXGetEc("CropX get ec", "get_cropx_ec"),
    CropXGetQueue1("CropX get queue 1", "get_queue:1"),
    CropXGetQueue2("CropX get queue 2", "get_queue:2");

    private String mLabel;
    private String mValue;

    private static final String TAG = BLECommand.class.getSimpleName();

    BLECommand(String mLabel, String mValue ) {
        this.mLabel = mLabel;
        this.mValue = mValue;
    }

    public static BLECommand getBLECommandFromLabel(String mLabel){
        BLECommand[] bleCommands = BLECommand.values();
        for (BLECommand bleCommand : bleCommands) {
            if (bleCommand != null) {
                if (bleCommand.getLabel().equals(mLabel)) {
                    return bleCommand;
                }
            }
        }
        return null;
    }

    public static BLECommand getBLECommandFromValue(String mValue){
        BLECommand[] bleCommands = BLECommand.values();
        for (BLECommand bleCommand : bleCommands) {
            if (bleCommand != null) {
                if (bleCommand.getValue().equals(mValue)) {
                    return bleCommand;
                }
            }
        }
        return null;
    }

    public static byte[] getData(String value) {
        byte[] data = new byte[value.length()];
        for (int i=0;i<value.length();i++){
            data[i] = (byte) value.charAt(i);
        }
        Log.e(TAG,"getData(" + value + "): " + bytesToHex(data));
        return data;
    }

    @Override
    public String toString() {
        return this.mLabel;
    }

    public String getValue() {
        return this.mValue;
    }
    public String getLabel() {
        return this.mLabel;
    }

}
