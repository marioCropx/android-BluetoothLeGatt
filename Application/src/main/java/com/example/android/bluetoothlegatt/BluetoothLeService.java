/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.android.bluetoothlegatt.starcom.BLECommand;
import com.example.android.bluetoothlegatt.starcom.Sha256;
import com.example.android.bluetoothlegatt.starcom.StarcomUUID;

import java.util.List;
import java.util.UUID;

import static com.example.android.bluetoothlegatt.starcom.Sha256.bytesToHex;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (mBluetoothDeviceAddress!= null && status == BluetoothGatt.GATT_SUCCESS &&
                    gatt.getDevice().getAddress().equals(mBluetoothDeviceAddress)){
                Log.e(TAG,"RSSI: " + rssi);
            }
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        /**
         * this function is called after services are discovered
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> gattServices = gatt.getServices();
                if (gattServices==null || gattServices.size()<=0){
                    return;
                }

                // set notifications for the READ characteristic
                BluetoothGattCharacteristic characteristicRead =
                        gatt.getService(StarcomUUID.SERVICE.getmUUID())
                                .getCharacteristic(StarcomUUID.READ.getmUUID());
                setCharacteristicNotification(characteristicRead,true);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * this function is called AFTER on description was written
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                if (characteristic!=null){
                    if (!readCharacteristic(characteristic)){
                        Log.e(TAG, "onDescriptorWrite readCharacteristic(characteristic) failed");
                    }
                } else {
                    Log.e(TAG, "onDescriptorWrite characteristic==null");
                }
            } else {
                Log.e(TAG, "onDescriptorWrite received: " + status);
            }
        }

        /**
         * this function is called AFTER characteristic was read
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getValue()!=null){
                    Log.e(TAG, "onCharacteristicRead: characteristic.getProperties() = " + characteristic.getProperties());
                    Log.e(TAG, "onCharacteristicRead: characteristic.getUuid() = " + characteristic.getUuid());
                    Log.e(TAG, "onCharacteristicRead: characteristic.getValue().length = " +characteristic.getValue().length);
                    final byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        Log.e(TAG,"onCharacteristicRead: " + new String(data)+ bytesToHex(data));
                    }
                    if (bleAuthorizationSent){
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    } else if (characteristic.getUuid().equals(StarcomUUID.READ.getmUUID())){
                        byte[] token = Sha256.getSHA256Token(characteristic.getValue());
                        if (token!=null){
                            writeCharacteristic(token);
                        }
                    }
                }
            } else  {
                Log.e(TAG,"onCharacteristicRead failed");
            }
        }

        /**
         * this function is called after write characteristic
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (bleAuthorizationSent){
//                    sendCommand(BLECommand.ReadVersion); //CHECKS IF THE AUTHENTICATION WAS SUCCESSFUL
                }
            } else  {
                Log.e(TAG,"onCharacteristicWrite failed");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.e(TAG,"onCharacteristicChanged");
            if (characteristic.getValue()!=null ){
                Log.e(TAG, "onCharacteristicChanged: characteristic.getProperties() = " + characteristic.getProperties());
                Log.e(TAG, "onCharacteristicChanged: characteristic.getUuid() = " + characteristic.getUuid());
                Log.e(TAG, "onCharacteristicChanged: characteristic.getValue().length = " +characteristic.getValue().length);
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    Log.e(TAG,"onCharacteristicChanged: " + new String(data));
                }
                if (bleAuthorizationSent){
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                } else{
                    byte[] token = Sha256.getSHA256Token(characteristic.getValue());
                    if (token!=null){
                        writeCharacteristic(token);
                    }
                }
            }
        }
    };

    /**
     * this function writes characteristic to WRITE!!
     * @param data - the value to insert
     */
    private void writeCharacteristic(byte[] data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
        BluetoothGattCharacteristic characteristic =
                mBluetoothGatt.getService(StarcomUUID.SERVICE.getmUUID())
                        .getCharacteristic(StarcomUUID.WRITE.getmUUID());
        characteristic.setValue(data);
        if (!mBluetoothGatt.writeCharacteristic(characteristic)){
            Log.e(TAG,"writeCharacteristic: mBluetoothGatt.writeCharacteristic(" + characteristic.getUuid() + "): - false");
        }else {
            bleAuthorizationSent = true;
        }


    }

    private void sendCommand(BLECommand bleCommand) {
        BluetoothGattCharacteristic characteristic =
                mBluetoothGatt.getService(StarcomUUID.SERVICE.getmUUID())
                        .getCharacteristic(StarcomUUID.WRITE.getmUUID());

        characteristic.setValue(BLECommand.getData(bleCommand.getValue()));
//        characteristicWrite.setWriteType(WRITE_TYPE_SIGNED);
        if (!mBluetoothGatt.writeCharacteristic(characteristic)){
            Log.e(TAG,"sendCommand: mBluetoothGatt.writeCharacteristic(" + characteristic.getUuid() + "): - false");
        }
    }

    private boolean bleAuthorizationSent = false;

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

//         This is special handling for the Heart Rate Measurement profile.  Data parsing is
//         carried out as per profile specifications:
//         http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            Log.e(TAG,"broadcastUpdate: " + new String(data));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     * THIS FUNCTION IS CALLED ONLY FOR THE READ CHARACTERISTIC!!!!
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (characteristic != null) {
            if (mBluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_NOTIFICATION_CONFIG));
                if (descriptor != null) {
                    // Prefer notify over indicate
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        Log.d(TAG, "Characteristic " + StarcomUUID.getStarcomUUIDFromUUID(characteristic.getUuid()) + " set NOTIFY");
                        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        Log.d(TAG, "Characteristic " + StarcomUUID.getStarcomUUIDFromUUID(characteristic.getUuid()) + " set INDICATE");
                        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    } else {
                        Log.d(TAG, "Characteristic " + StarcomUUID.getStarcomUUIDFromUUID(characteristic.getUuid()) + " does not have NOTIFY or INDICATE property set");
                    }

                    try {
                        if (mBluetoothGatt.writeDescriptor(descriptor)) {
                            Log.d(TAG, "setNotify complete");
                        } else {
                            Log.e(TAG,"Failed to set client characteristic notification for " + StarcomUUID.getStarcomUUIDFromUUID(characteristic.getUuid()));
                        }
                    } catch (Exception e) {
                        Log.e(TAG,"Failed to set client characteristic notification for " + StarcomUUID.getStarcomUUIDFromUUID(characteristic.getUuid()) + ", error: " + e.getMessage());
                    }

                } else {
                    Log.e(TAG,"Set notification failed for " + StarcomUUID.getStarcomUUIDFromUUID(characteristic.getUuid()));
                }

            } else {
                Log.e(TAG,"Failed to register notification for " + StarcomUUID.getStarcomUUIDFromUUID(characteristic.getUuid()));
            }

        } else {
            Log.e(TAG,"Characteristic not found");
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
