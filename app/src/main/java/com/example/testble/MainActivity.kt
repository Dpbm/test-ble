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
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment

const val BUTTON_TEXT_DEFAULT = "Grant";
const val BUTTON_TEXT_LOADING = "Granting";

data class DeviceData(val name:String, val device: BluetoothDevice);
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

    fun hasGrantedPermissions():Boolean{
        return remainingPermissions().isEmpty();
    }
}

class BtManager(
    context: Context,
    val pm: PermissionsManager,
    val request: ManagedActivityResultLauncher<Intent, ActivityResult>
){

    val devices = mutableMapOf<String, DeviceData>();
    var onChange: ((Map<String,DeviceData>) -> Unit)? = null;
    private var adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java).adapter;
    private var scanner: BluetoothLeScanner? = null;
    private val btSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device;
            devices[device.address] = DeviceData(name=device?.name ?: "Unknown", device=device);
            onChange?.invoke(devices);
        }
    }


    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
            }
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun listLocalDevices(){

        if(adapter == null || !pm.hasGrantedPermissions()){
            return;
        }

        if(adapter?.isEnabled == false){
            activateBluetooth();
            return;
        }

        getDevices();
    }


    fun activateBluetooth(){
        request.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun getDevices(){
        scanner = adapter?.getBluetoothLeScanner() ?: return;
        scanner?.startScan(null, btSettings, scanCallback);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning(){
        scanner?.stopScan(scanCallback);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun pair(device: BluetoothDevice) {
        device.createBond();
    }

    /*@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice){
        device.connectGatt(baseContext, false, gattCallback);
    }*/

        val isEnabled: Boolean
            get() = adapter?.isEnabled ?: false;
}


class MainActivity : ComponentActivity() {
    val pm = PermissionsManager(this);

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

                        Permissions(pm)

                        BLEData(pm)


                        // TEXT INPUT TO SEND
                        // TEXTAREA TO RECIEVE

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
fun CustomButton(
    callback: (() -> Unit),
    enabled:Boolean=true,
    text:String
){
    TextButton(
        onClick = {
            callback()
        },
        colors= ButtonColors(
            containerColor = AppColors.purple,
            contentColor =   AppColors.white,
            disabledContainerColor = AppColors.gray,
            disabledContentColor= AppColors.white
        ),
        enabled = enabled
    ) {
        Text(text)
    }
}

@Composable
fun Permissions(pm: PermissionsManager){

    var permissionsList by remember { mutableStateOf(pm.remainingPermissions()); };
    if(permissionsList.isEmpty()) return;

    var buttonText by remember{ mutableStateOf(BUTTON_TEXT_DEFAULT); }

    val pmRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            values ->
                permissionsList = values.filter { !it.value }.keys.toList();
                buttonText = BUTTON_TEXT_DEFAULT;
    }


    Column(modifier = Modifier.padding(paddingValues = PaddingValues(top = Dp(20.0f), start = Dp(20.0f), end=Dp(20.0f)))) {
        Text(
            text = "Not Granted Permissions:",
            fontSize = TextUnit(15.0f, type = TextUnitType.Sp),
        )

        LazyColumn {
            items(permissionsList.size){ i ->
                Text(permissionsList[i], color = AppColors.red)
            }
        }
        CustomButton({
            buttonText = BUTTON_TEXT_LOADING;
            pmRequest.launch(permissionsList.toTypedArray());
        }, permissionsList.isNotEmpty(), buttonText)

    }
}

@Composable
fun BLEData(pm: PermissionsManager){

    if(!pm.hasGrantedPermissions()) return;

    val btEnable = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }

    val context = LocalContext.current;
    val bm = BtManager(context, pm, btEnable);

    var enabled :Boolean by remember { mutableStateOf(bm.isEnabled) }
    var searching: Boolean by remember { mutableStateOf(false) }
    val data = remember { mutableStateMapOf<String,DeviceData>(); }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    enabled = state == BluetoothAdapter.STATE_ON
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    DisposableEffect(Unit) {
        bm.onChange = { devices ->
            data.clear();
            data.putAll(devices);
        }

        onDispose {
            bm.onChange = null;
        }
    }



    val itemsList by remember {
        derivedStateOf { data.entries.toList() }
    }

   Column(
       modifier = Modifier.padding(Dp(20.0f))
   ) {
       Text("Bluetooth enabled: $enabled")

       CustomButton(
           callback={
               if(!searching){
                   bm.listLocalDevices()
                   searching = true;
               }else{
                  bm.stopScanning();
                   searching = false;
                   data.clear();
               }
                        },
           enabled=true,
           text=if(searching) "Stop Search" else "Search Devices")

       LazyColumn() {

           items(itemsList, key = { it.key }){(key, value) ->
               Row(
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.SpaceBetween,
                   modifier = Modifier.fillMaxWidth()
               ){
                   Text("$key - ${value.name}")
                   CustomButton({ bm.pair(value.device) }, true, "Pair")
                   CustomButton(callback = {}, enabled = true, text="Send")
               }
           }


       }
   }
}