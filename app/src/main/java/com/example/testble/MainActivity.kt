package com.example.testble

import android.Manifest
import android.annotation.SuppressLint
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
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.os.persistableBundleOf


typealias Devices = HashMap<String,String>;


object AppColors{
    val green = Color(green=103, red=47, blue=63);
    val white = Color(green=255, red=255, blue=255);
    val red = Color(red=215, green=68, blue=86);
    val gray = Color(191,201,209);
    val purple = Color(red=155,green=142,blue=199);
}

class BLEManager{
    companion object{
        fun hasBLE(context: Context):Boolean{
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        }
    }
}

class PermissionsManager(val context: Context){

    @SuppressLint("InlinedApi")
    val allPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION);

    fun remainingPermissions():List<String>{
        return allPermissions.filter { p -> ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED };
    }
}

class MainActivity : ComponentActivity() {
    private val pm = PermissionsManager(this);
    private val pmRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        values -> if(!values.all { it.value }){
           makeToast("Failed on grant permissions!");
        }else{
            this.recreate();
        }
    }

    private fun makeToast(text:String){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /*
    private var btAdapter: BluetoothAdapter? = null;
    private var scanner: BluetoothLeScanner? = null;

    private val devices: Devices = HashMap<String,String>();

    @SuppressLint("MissingPermission")
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

    // For OLD BLUETOOTH
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
    //For old bluetooth
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


     */

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Test BLE") },
                        colors = TopAppBarColors(
                            containerColor = AppColors.green,
                            scrolledContainerColor = AppColors.green,
                            navigationIconContentColor = AppColors.green,
                            titleContentColor = AppColors.white,
                            actionIconContentColor = AppColors.green
                        )
                    )
                },
                content = { p ->
                    Column(modifier = Modifier.padding(p)) {
                        HasBLE(
                            has = BLEManager.hasBLE(LocalContext.current)
                        )

                        Permissions(pm.remainingPermissions()) {
                            pmRequest.launch(
                                pm.remainingPermissions().toTypedArray()
                            )
                        }

                        /*TextButton(onClick =  { listLocalDevices() }) {
                            Text("List Devices")
                        }*/
                    }
                }


            )
        }
    }
}


@Composable
fun HasBLE(has: Boolean) {
    Text(
        text = "This Device has " + if(!has) "not " else "" +  "access to BLE!",
        modifier = Modifier.padding(paddingValues = PaddingValues(all= Dp(20.0f))),
            fontSize = TextUnit(20.0f, type = TextUnitType.Sp),
            color = if(has) AppColors.green else AppColors.red
    )
}

@Composable
fun Permissions(permissions: List<String>, callback:(() -> Unit)){
    Column(modifier = Modifier.padding(paddingValues = PaddingValues(top = Dp(20.0f), start = Dp(20.0f), end=Dp(20.0f)))) {
        Text(
            text = "Not Granted Permissions:",
            fontSize = TextUnit(15.0f, type = TextUnitType.Sp),
        )

        LazyColumn {
            items(permissions.size){ i ->
                Text(permissions[i], color = AppColors.red)
            }
        }

        TextButton(
            onClick = { callback(); },
            colors= ButtonColors(
                containerColor = AppColors.purple,
                contentColor =   AppColors.white,
                disabledContainerColor = AppColors.gray,
                disabledContentColor= AppColors.white
            ),
                enabled = permissions.isNotEmpty()
        ) {
            Text("Grant")
        }


    }
}