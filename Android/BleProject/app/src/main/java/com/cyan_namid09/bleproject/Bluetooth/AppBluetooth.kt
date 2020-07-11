package com.cyan_namid09.bleproject.Bluetooth

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*


private const val TAG = "AppBluetooth"

class AppBluetooth(appContext: Context) {
    val manager: BluetoothManager by lazy { appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val advertiser: BluetoothLeAdvertiser by lazy { manager.adapter.bluetoothLeAdvertiser }
    private val gattServer: BluetoothGattServer by lazy { manager.openGattServer(appContext, gattServerCallback) }

    init {
        // Notifyが使えるCharacteristicを含んだServiceをServerに設定
        gattServer.addService(BLEParams.service)

        // とりま値をセット
        BLEParams.notifyCharacteristic.value = "SAMPLE".toByteArray()
    }

    private var connectedDevice: BluetoothDevice? = null

    fun advertise() {
        advertiser.startAdvertising(
            AdvertiseSettings.Builder().build(),
            AdvertiseData.Builder().apply { addServiceUuid(ParcelUuid(BLEParams.UUID_SERVICE)) }.build(),
            object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.d(TAG, "Start Advertising")
                }

                override fun onStartFailure(errorCode: Int) {
                    Log.e(TAG, "onStartFailure. error code is $errorCode")
                    when (errorCode) {
                        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Log.e(TAG, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS")
                        ADVERTISE_FAILED_ALREADY_STARTED -> Log.e(TAG, "ADVERTISE_FAILED_ALREADY_STARTED")
                        ADVERTISE_FAILED_DATA_TOO_LARGE -> Log.e(TAG, "ADVERTISE_FAILED_DATA_TOO_LARGE")
                        ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> Log.e(TAG, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED")
                        ADVERTISE_FAILED_INTERNAL_ERROR -> Log.e(TAG, "ADVERTISE_FAILED_INTERNAL_ERROR")
                    }
                }
            }
        )
    }

    fun notify(value: ByteArray) {
        val device = this.connectedDevice
        if (device != null) {
            BLEParams.notifyCharacteristic.value = value
            val notifyFlag = gattServer.notifyCharacteristicChanged(device, BLEParams.notifyCharacteristic, false)
            if (notifyFlag) Log.d(TAG, "succeeded notification") else Log.e(TAG, "failed notification")
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "STATE CONNECTED")
                    connectedDevice = device
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "STATE DISCONNECTED")
                    connectedDevice = null
                }
            }
        }

        // Characteristic
        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            Log.d(TAG, "Characteristic Read Request")
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS , offset, characteristic?.value ?: "NONE".toByteArray())
        }
        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Log.d(TAG, "Characteristic Write Request")
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }
        // Descriptor
        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            Log.d(TAG, "Descriptor Read Request")
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "NONE".toByteArray())
        }
        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Log.d(TAG, "Descriptor Write Request")
            descriptor?.value = value
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.d(TAG, "sent operation succeeded")
                }
                else -> {
                    Log.d(TAG, "sent operation failed")
                }
            }
        }

    }

    companion object {
        lateinit var shared: AppBluetooth

        fun initialize(appContext: Context) {
            shared =
                AppBluetooth(appContext)
        }
    }
}
