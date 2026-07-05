package com.newritage.app.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.util.UUID

/**
 * ESP32(GATT 서버) <-> 앱(GATT 클라이언트) 연결을 담당하는 싱글턴.
 *
 * 기기는 연결되면 항상 SENSOR 특성으로 압력값을 스트리밍한다. "측정 시작/종료" 명령은 없으므로,
 * 세션 경계는 [onSensorData]/[onConnectionChange] 콜백을 붙였다 떼는 것으로 앱이 처리한다.
 * 긴장도 분류·baseline 비교는 이 클래스의 책임이 아니며, 값을 그대로 위로 올려주기만 한다.
 */
object BleManager {

    private const val DEVICE_NAME = "MeditationStone"
    private val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    private val SENSOR_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    private val COMMAND_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
    private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private const val SCAN_TIMEOUT_MS = 15_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var appContext: Context? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var sensorCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanning = false

    /** f0(엄지), f1(검지), f2(손바닥), total = f0+f1+f2. 메인 스레드에서 호출된다. */
    var onSensorData: ((f0: Int, f1: Int, f2: Int, total: Int) -> Unit)? = null

    /** 메인 스레드에서 호출된다. */
    var onConnectionChange: ((Boolean) -> Unit)? = null

    var isConnected: Boolean = false
        private set

    /** API 레벨에 맞는 런타임 권한 목록. 스캔 시작 전에 요청해야 한다. */
    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        return requiredPermissions().all {
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /** SERVICE_UUID를 광고하는 기기를 15초간 스캔하고, 발견 즉시 연결한다. 이미 연결(시도) 중이면 무시. */
    @SuppressLint("MissingPermission")
    fun startScan(context: Context) {
        if (scanning || bluetoothGatt != null) return
        if (!hasRequiredPermissions(context)) return

        appContext = context.applicationContext

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return
        if (!adapter.isEnabled) return

        val leScanner = adapter.bluetoothLeScanner ?: return
        scanner = leScanner

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanning = true
        leScanner.startScan(filters, settings, scanCallback)
        mainHandler.postDelayed({ stopScan() }, SCAN_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!scanning) return
        scanning = false
        scanner?.stopScan(scanCallback)
    }

    /** 연결을 끊고 리소스를 정리한다. 기기 재광고는 펌웨어가 알아서 하므로 앱은 필요 시 재스캔만 하면 된다. */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        bluetoothGatt?.disconnect()
    }

    /** effect(1~123) 진동 효과 한 번 재생을 기기에 요청한다. */
    @SuppressLint("MissingPermission")
    fun sendVibration(effect: Int) {
        val gatt = bluetoothGatt ?: return
        val characteristic = commandCharacteristic ?: return
        val bytes = "V,$effect".toByteArray(Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val context = appContext ?: return
            stopScan()
            connect(result.device, context)
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice, context: Context) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    isConnected = false
                    sensorCharacteristic = null
                    commandCharacteristic = null
                    gatt.close()
                    if (bluetoothGatt === gatt) bluetoothGatt = null
                    notifyConnectionChange(false)
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt.getService(SERVICE_UUID) ?: return

            sensorCharacteristic = service.getCharacteristic(SENSOR_CHAR_UUID)
            commandCharacteristic = service.getCharacteristic(COMMAND_CHAR_UUID)

            val sensorChar = sensorCharacteristic ?: return
            gatt.setCharacteristicNotification(sensorChar, true)

            val cccd = sensorChar.getDescriptor(CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == CCCD_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                isConnected = true
                notifyConnectionChange(true)
            }
        }

        // API 32 이하: 구 시그니처. 33+에서는 아래 새 시그니처만 호출되도록 SDK 체크로 분기.
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            if (characteristic.uuid != SENSOR_CHAR_UUID) return
            @Suppress("DEPRECATION")
            handleSensorPayload(characteristic.value)
        }

        // API 33+: 신 시그니처.
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid != SENSOR_CHAR_UUID) return
            handleSensorPayload(value)
        }
    }

    private fun handleSensorPayload(bytes: ByteArray?) {
        val payload = bytes?.toString(Charsets.UTF_8) ?: return
        val parts = payload.split(",")
        if (parts.size != 4) return

        val f0 = parts[0].toIntOrNull() ?: return
        val f1 = parts[1].toIntOrNull() ?: return
        val f2 = parts[2].toIntOrNull() ?: return
        val total = parts[3].toIntOrNull() ?: return

        mainHandler.post { onSensorData?.invoke(f0, f1, f2, total) }
    }

    private fun notifyConnectionChange(connected: Boolean) {
        mainHandler.post { onConnectionChange?.invoke(connected) }
    }
}
