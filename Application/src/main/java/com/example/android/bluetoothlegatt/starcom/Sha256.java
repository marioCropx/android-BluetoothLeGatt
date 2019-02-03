package com.example.android.bluetoothlegatt.starcom;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256 {

    private static final byte[] BLE_DEVICE_KEY = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};

    private static final String TAG = Sha256.class.getSimpleName();
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars).toUpperCase();
    }
    // PREPROCESSING [§6.2.1]
    public static byte[] getSHA256Token(byte[] values){
        byte[] mergedBytes = new byte[BLE_DEVICE_KEY.length + values.length];
        System.arraycopy(BLE_DEVICE_KEY, 0, mergedBytes, 0, BLE_DEVICE_KEY.length);
        System.arraycopy(values, 0, mergedBytes, BLE_DEVICE_KEY.length, values.length);
//        StringBuilder concatBytesAsString = new StringBuilder();
//        for (byte key : mergedBytes){
//            concatBytesAsString.append(Integer.toHexString(key));
//        }
        String data = bytesToHex(mergedBytes);
        Log.e(TAG, "getSHA256Token: mergedBytes = " + data);
//        String data = bin2hex(mergedBytes);
        byte[] result = sha256(mergedBytes);
        if (result != null) {
            Log.e(TAG, "getSHA256Token: result = " + bytesToHex(result));
        }
        return result;
    }

    private static byte[] sha256(byte[] data){
        try {
            MessageDigest md =  MessageDigest.getInstance("SHA-256");
            md.update(data);
            MessageDigest tc1 = (MessageDigest) md.clone();
            return tc1.digest();
        } catch (NoSuchAlgorithmException el) {
            Log.e(TAG,el.getMessage());
        } catch (CloneNotSupportedException e) {
            Log.e(TAG,e.getMessage());
        }
        return null;
    }

    static String bin2hex(byte[] data) {
        StringBuilder hex = new StringBuilder();
        for (byte b : data)
            hex.append(String.format("%02x", b & 0xFF));
        return hex.toString();
    }
}
