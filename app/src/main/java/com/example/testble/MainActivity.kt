package com.example.testble

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.material3.TextButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var btAdapter: BluetoothAdapter? = null;
    private var scanner: BluetoothLeScanner? = null;


    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.i("correct", "ENABLED");
            getDevices();
        } else {
            makeToast("Denied")
        }
    }

    private val scanCallback = object : ScanCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown"
            val address = device.address

            Log.i("BLE", "Device: $name - $address")
            pair(device);


        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "Scan failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback(){

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun pair(device: BluetoothDevice){
        scanner?.stopScan(scanCallback);
        //device.connectGatt(baseContext, false, gattCallback);
            device.createBond();
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun getDevices(){
        Log.i("dd", "INSIDE");


        /*
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        registerReceiver(receiver, filter)*/

        if(btAdapter == null) return;
        scanner = btAdapter?.getBluetoothLeScanner() ?: return;
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanner?.startScan(null, settings, scanCallback);

        //btAdapter?.startDiscovery();
    }

    /*
    private val receiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    val name = device?.name ?: "Unknown"
                    val address = device?.address

                    Log.i("BT_CLASSIC", "Found: $name - $address")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i("BT_CLASSIC", "Discovery finished")
                }
            }
        }

    }*/

    private fun testHasBLE():Boolean{
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private fun checkBLPermissions():Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION);

            val missing = permissions.filter{ permission -> ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED }

            if(missing.isNotEmpty()) {
                requestPermissions(missing.toTypedArray(), 100);
                return false;
            }
        }

        return true;
    }

    private fun makeToast(text:String){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun listLocalDevices(){
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java);
        btAdapter = bluetoothManager.adapter;

        if (btAdapter == null) {
            makeToast("Error on get Adapter");
            return;
        }

        if(!checkBLPermissions()){
            return;
        }

        if(btAdapter?.isEnabled == false){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent);
        }else{
            Log.i("status", "SEARCHING FOR DEVICES...")
            getDevices();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column() {
                HasBLE(
                    has = testHasBLE()
                )

                TextButton(onClick =  { listLocalDevices() }) {
                    Text("List Devices")

                }


            }
        }
    }
}

@Composable
fun HasBLE(has: Boolean) {
    Text(
        text = if(has) "Has BLE" else "Has not BLE feature!",
        modifier = Modifier.padding(paddingValues = PaddingValues(all= Dp(20.0f)))
    )
}
