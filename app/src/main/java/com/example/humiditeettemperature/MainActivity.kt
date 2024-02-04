package com.example.humiditeettemperature

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.example.humiditeettemperature.databinding.ActivityMainBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID.fromString

//                                                                                                     среднее, максимальное, меньшее ++ переработать функции

private lateinit var btLauncher: ActivityResultLauncher<Intent>

val mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

var temperatureValue = "30"
var humidityValue = "30"
var characterSwitcher = 0;
var scanStopVar = false
var databaseSaveVar = false

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val bp = BlePermission(this, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!bp.hasRequiredRuntimePermissions()) {
            bp.requestRelevantRuntimePermissions()
        }

        registerBtLauncher ()

        if (!mBluetoothAdapter.isEnabled) {
       //     binding.textView.text = "БЛЮТУП ОФ"
        } else {
       //     binding.textView.text = "БЛЮТУП ОН"
        }

        binding.apply {
            button.setOnClickListener(onClickListener())
            connect.setOnClickListener(onClickListener())
            disconnect.setOnClickListener(onClickListener())
            button4.setOnClickListener(onClickListener())
        }
        showingValues ()
    }


    @SuppressLint("SetTextI18n", "MissingPermission")
    private fun onClickListener(): View.OnClickListener = View.OnClickListener {
        when (it.id) {
            R.id.button -> {

                if (!mBluetoothAdapter.isEnabled) {
                    btLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            }

            R.id.connect -> {
                if (!mBluetoothAdapter.isEnabled) {
                    val text = "BLUETOOTH OFF"
                    val duration = Toast.LENGTH_SHORT
                    val toast = Toast.makeText(applicationContext, text, duration)
                    toast.show()
                } else {

                    if (isScanning) {
                        stopBleScan()
                    }
                    else {
                        startBleScan()
                    }
                }
            }

            R.id.disconnect -> {
                binding.tempValuesC.text = "0.0"
                binding.tempValuesF.text = "0.0"
                binding.humidValues.text = "0.0"
                scanStopVar = true
                databaseSaveVar = false
            }

            R.id.button4 -> {
                val intent = Intent(this@MainActivity, DatabaseView::class.java)
                startActivity(intent)
            }


        }
    }


    private fun showingValues () {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                if (databaseSaveVar) {
                    val t = decodeHex(temperatureValue)
                    val h = decodeHex(humidityValue)

                    binding.tempValuesC.text = t
                    val temp = t.toFloat() * 1.8 + 32
                    binding.tempValuesF.text = String.format("%.1f", temp) //(t.toFloat() * 1.8 + 32)//toString() // String.format("%.2f", random)
                    binding.humidValues.text = h

                    saveToDatabase(t, h)
                }
                handler.postDelayed(this,  1000)
            }
        }, 0)
    }


    fun decodeHex(input: String): String {
        return input
            .split(Regex("(\\s|,)+"))
            .map { Integer.parseInt(it, 16) }
            .map { it.toChar() }
            .joinToString("");
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            bp.RUNTIME_PERMISSION_REQUEST_CODE -> {
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                    it.second == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
                }
                val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                when {
                    containsPermanentDenial -> {}
                    containsDenial -> {
                        bp.requestRelevantRuntimePermissions()
                    }
                    allGranted && bp.hasRequiredRuntimePermissions() -> {
                        //     startBleScan()
                    }
                    else -> {
                        recreate()
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun startBleScan() {
        bluetoothAdapter.bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
        isScanning = true
    }


    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        isScanning = false
    }


    private val scanCallback = object : ScanCallback() {
        @SuppressLint("SetTextI18n", "MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {
                binding.textView2.text = "Имя устройства: ${name ?: "безымянное\n"}, адрес: $address\n"
                Log.i("ScanCallback", "Найдено BLE устройство: ${name ?: "безымянное"}, адрес: $address")
                if (name == "TempHumid") {
                    binding.textView2.text = "Имя устройства: ${name ?: "безымянное\n"}, адрес: $address\n"
                    stopBleScan()
                    Log.w("ScanResultAdapter", "Connecting to $address")
                  //  deviceAddress = address
                    connectGatt(applicationContext, false, gattCallback)
                }
            }
        }
    }


    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { binding.connect.text = if (value) "остановить поиск" else "поиск и подключение" }
        }


    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }


@SuppressLint("SetTextI18n")
private fun registerBtLauncher () {
    btLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK){
          //  binding.textView.text = "BLUETOOTH ON"
        }
    }
}


    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission", "SuspiciousIndentation")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")

                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
            }
        }


            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                with(gatt) {
                    Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")

                    printGattTable(gatt)

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed(object : Runnable {
                        override fun run() {
                            databaseSaveVar = true
                         if (characterSwitcher%2 == 0) {
                             ++characterSwitcher
                             readTempValue(gatt)
                         }
                            else {
                                ++characterSwitcher
                             readHumidValue(gatt) }

                            if (scanStopVar) {
                                scanStopVar = false
                                gatt.disconnect()
                                gatt.close()
                                return
                            }
                            handler.postDelayed(this,  100)
                        }
                    }, 0)
                }
            }


        @Deprecated("Deprecated in Java")
        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                      //  Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                        if ((uuid == fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8"))) {
                            temperatureValue = value.toHexString()
                        } else {
                            humidityValue = value.toHexString()
                        }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }
    }


    private val scanSettings: ScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()


    @SuppressLint("MissingPermission")
    fun readTempValue(gatt: BluetoothGatt) {
        val tempServiceUuid = fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val tempLevelCharUuid = fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val tempValueChar = gatt.getService(tempServiceUuid).getCharacteristic(tempLevelCharUuid)
        if (tempValueChar?.isReadable() == true) {
            gatt.readCharacteristic(tempValueChar)
        }
    }


    @SuppressLint("MissingPermission")
    fun readHumidValue(gatt: BluetoothGatt) {
        val humidServiceUuid = fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val humidLevelCharUuid = fromString("a653984b-c768-4e29-9cd9-62afdb633349")
        val humidValueChar = gatt.getService(humidServiceUuid).getCharacteristic(humidLevelCharUuid)
        if (humidValueChar?.isReadable() == true) {
            gatt.readCharacteristic(humidValueChar)
        }
    }


    fun printGattTable(gatt: BluetoothGatt) {
        if (gatt.services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        gatt.services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    private fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    //  fun BluetoothGattCharacteristic.isWritable(): Boolean =
    //      containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    //   fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
    //      containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)


    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }


    fun ByteArray.toHexString(): String = joinToString(separator = " ") { String.format("%02X", it) }


    private fun saveToDatabase (temp: String, humid: String) {
        val db = Database(this, null)
        val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy, HH:mm:ss"))
        db.addData(date, temp, humid)
    }

}