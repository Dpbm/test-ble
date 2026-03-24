package com.example.testble

import android.Manifest
import android.annotation.SuppressLint
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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.Alignment
import java.util.UUID
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.ScanFilter
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect

const val BUTTON_TEXT_DEFAULT = "Grant";
const val BUTTON_TEXT_LOADING = "Granting";
const val BLE_TAG = "BLE";

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
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_ADVERTISE);

    fun remainingPermissions():List<String>{
        return allPermissions.filter { p -> ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED };
    }

    fun hasGrantedPermissions():Boolean{
        return remainingPermissions().isEmpty();
    }
}

class BtManager(
    val context: Context,
    val pm: PermissionsManager,
    val request: ActivityResultLauncher<Intent?>
){
    val devices = mutableMapOf<String, String>();
    var onChange: ((Map<String,String>) -> Unit)? = null;
    var onReceive: ((String) -> Unit)? = null;
    var onSearch: ((Boolean) -> Unit)? = null;

    private val manager: BluetoothManager? by lazy{
        context.getSystemService(BluetoothManager::class.java);
    }
    private val adapter: BluetoothAdapter? = manager?.adapter;
    private val advertiser: BluetoothLeAdvertiser? = adapter?.bluetoothLeAdvertiser;
    private var scanner: BluetoothLeScanner? = null;

    val dataTobeSent = "Hello dear!";
    private val serviceID  = UUID.fromString("ec2ea133-88b0-4fac-bf1b-d6b7727c4758");
    private val charID  = UUID.fromString("3a8295e8-074d-4a48-be1c-0b62eacd8ba1");

    private var server : BluetoothGattServer? = null;

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device;
            val address = device.address;
            val name = device?.name ?: "Unknow";
            Log.i(BLE_TAG, "Found -> $address - $name");
            devices[address] = name;
            onChange?.invoke(devices);
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(BLE_TAG, "Scan failed: $errorCode")
        }
    }
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(BLE_TAG, "Discovering Services....");
                gatt?.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(BLE_TAG, "Disconnected GATT!")
                gatt?.disconnect();
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.i(BLE_TAG, "Success FIND SERVICES!")
                Log.i(BLE_TAG, "Connect GATT")

                val service = gatt?.getService(serviceID) ?: return;
                Log.i(BLE_TAG, "Got Service: ${service.uuid}")

                val characteristic = service.getCharacteristic(charID) ?: return;
                Log.i(BLE_TAG, "Got Char: ${characteristic.uuid}")

                val props = characteristic.properties

                val supportsNoResponse = (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                val supportsWrite = (props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0

                if(!supportsNoResponse && !supportsWrite){
                    Log.e(BLE_TAG, "DEVICE SUPPORTS NO WRITING!")
                    return;
                }

                Log.i(BLE_TAG, "Supports No Response: $supportsNoResponse")
                Log.i(BLE_TAG, "Supports Standard Write: $supportsWrite")

                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

                Handler(Looper.getMainLooper()).postDelayed({
                    val result = gatt.writeCharacteristic(
                        characteristic,
                        dataTobeSent.toByteArray(),
                        if(supportsNoResponse) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                    Log.i(BLE_TAG, "Write initiated: $result")
                }, 150)
            }else{
                Log.e(BLE_TAG, "Failed on discover services $status")
            }
        }


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(BLE_TAG, "Write successful")
            } else {
                Log.e(BLE_TAG, "Write failed: $status")
            }
            Handler(Looper.getMainLooper()).postDelayed({
                Log.i(BLE_TAG, "Closing connection safely")
                gatt?.disconnect()
            }, 500)
        }
    }

    private val serverCallback = object: BluetoothGattServerCallback(){
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            val data = String(value);
            Log.i(BLE_TAG, "Got via GATT: $data")
            onReceive?.invoke(data)
        }
    }

    private val advertiseCallback = object: AdvertiseCallback(){
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(BLE_TAG, "Advertising STARTED")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(BLE_TAG, "Advertising FAILED: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun createGATTServer(){
        Log.i(BLE_TAG, "Creating Server...")
       server = manager?.openGattServer(context, serverCallback);
        gattAddService()
        gattAdvertise()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun gattAddService(){
        val service = BluetoothGattService(
            serviceID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        val characteristic = BluetoothGattCharacteristic(
            charID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_WRITE or
                    BluetoothGattCharacteristic.PERMISSION_READ
        );

        service.addCharacteristic(characteristic)
        server?.addService(service)
    }

    private fun gattAdvertise(){
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceID))
            .build()

        Log.i(BLE_TAG, "Advertising...")
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun listLocalDevices(){

        if(adapter == null || !pm.hasGrantedPermissions()){
            return;
        }

        if(!adapter.isEnabled){
            activateBluetooth();
            return;
        }

        if(!isLocationEnabled()){
            activateGPS();
            return;
        }

        getDevices();
    }

    fun activateBluetooth(){
        Log.i(BLE_TAG, "Asking to activate ble")
        request.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    fun activateGPS(){
        Log.i(BLE_TAG, "Asking to activate GPS")
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun getDevices(){
        Log.i(BLE_TAG, "Started Scanning!")
        scanner = adapter?.getBluetoothLeScanner() ?: return;
        val btSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceID))
            .build()
        scanner?.startScan(listOf(filter), btSettings, scanCallback);
        onSearch?.invoke(true);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning(){
        Log.i(BLE_TAG, "Stopped scanning")
        scanner?.stopScan(scanCallback);
        onSearch?.invoke(false);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun pair(address: String) {
        val device = getDeviceByAddress(address) ?: return;
        Log.i(BLE_TAG, "Pairing to $address")
        device.createBond();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(address: String){
        val device = getDeviceByAddress(address) ?: return;
        Log.i(BLE_TAG, "GATT connect to $address")
        device.connectGatt(context, false, bluetoothGattCallback);
    }

    @SuppressLint("MissingPermission")
    fun clearServer(){
        Log.i(BLE_TAG, "Stopping server");
        server?.close();
        advertiser?.stopAdvertising(advertiseCallback);

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getDeviceByAddress(address: String): BluetoothDevice? {
        return try {
            adapter?.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    val isEnabled: Boolean
        get() = adapter?.isEnabled ?: false;

    val supportAdv: Boolean
        get() = adapter?.isMultipleAdvertisementSupported ?: false;
}


class MainActivity : ComponentActivity() {
    val btEnable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }

    lateinit var pm:PermissionsManager;
    lateinit var bm:BtManager;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pm = PermissionsManager(this)
        bm = BtManager(this, pm, btEnable)


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
                content = {
                    p -> Data(pm,bm,p)
                }


            )
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        bm.clearServer();
        bm.stopScanning();
        super.onDestroy()
    }
}

@Composable
fun Data(pm: PermissionsManager, bm: BtManager, p: PaddingValues){

    Column(modifier = Modifier.padding(p)) {
            HasBLE(
                has = BLEManager.hasBLE(LocalContext.current)
            )

            PermissionBasedFlow(pm, bm)
    }

}
@SuppressLint("MissingPermission")
@Composable
fun PermissionBasedFlow(pm: PermissionsManager, bm:BtManager){
    var granted by remember {mutableStateOf(pm.hasGrantedPermissions()) }
    val callback = { g:Boolean ->
        if(g) {
            bm.createGATTServer()
        }

        granted = g;
    }

    LaunchedEffect(Unit) {
        if(granted){
            bm.createGATTServer()
        }
    }

    if(!granted) {
        Permissions(pm,callback)
    }else{
        BLEData(bm)
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
fun Permissions(pm: PermissionsManager, onGrant:((Boolean) -> Unit)){

    var permissionsList by remember { mutableStateOf(pm.remainingPermissions()); };
    var buttonText by remember{ mutableStateOf(BUTTON_TEXT_DEFAULT); }
    val pmRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            values ->
                permissionsList = values.filter { !it.value }.keys.toList();
                buttonText = BUTTON_TEXT_DEFAULT;
                onGrant.invoke(permissionsList.isEmpty());
    }


    LazyColumn(modifier = Modifier.padding(paddingValues = PaddingValues(all=Dp(20.0f))).fillMaxSize()) {
        item{
            Text(
                text = "Not Granted Permissions:",
                fontSize = TextUnit(15.0f, type = TextUnitType.Sp),
            )
        }

        items(permissionsList.size){ i ->
            Text(permissionsList[i], color = AppColors.red)
        }

        item{
            CustomButton({
                buttonText = BUTTON_TEXT_LOADING;
                pmRequest.launch(permissionsList.toTypedArray());
            }, permissionsList.isNotEmpty(), buttonText)
        }

    }
}

@SuppressLint("MissingPermission")
@Composable
fun BLEData(bm: BtManager){
    val context = LocalContext.current;

    var btEnabled :Boolean by remember { mutableStateOf(bm.isEnabled) }
    var gpsEnabled :Boolean by remember { mutableStateOf(bm.isLocationEnabled()) }
    var searching: Boolean by remember { mutableStateOf(false) }
    val data = remember { mutableStateMapOf<String,String>(); }
    var received: String by remember {mutableStateOf("")}

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val isEnabled = state == BluetoothAdapter.STATE_ON
                    btEnabled = isEnabled
                    searching = searching && isEnabled
                    Log.i(BLE_TAG, "Changed BT $btEnabled - is searching=$searching")
                }else if(intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION){
                    val isEnabled = bm.isLocationEnabled();
                    gpsEnabled = isEnabled
                    searching = searching && isEnabled
                    Log.i(BLE_TAG, "Changed GPS $gpsEnabled - is searching=$searching")
                }
            }
        }

        val btFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val gpsFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(receiver, btFilter)
        context.registerReceiver(receiver, gpsFilter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    DisposableEffect(bm) {
        bm.onReceive = { data ->
            received = data;
        }

        bm.onChange = { devices ->
            data.clear();
            data.putAll(devices);
        }

        bm.onSearch = { s ->
           searching = s;
        }

        onDispose {
            bm.onReceive = null;
            bm.onChange = null;
            bm.onSearch = null;
        }
    }


    val itemsList = data.entries.toList()

   LazyColumn(
       modifier = Modifier.padding(Dp(20.0f))
   ) {

       item{
           Text("Bluetooth enabled: $btEnabled")
           Text("Location enabled: $gpsEnabled")
           Text("Supports ADV: ${bm.supportAdv}")
           Text("GATT data to be sent: ${bm.dataTobeSent}")
       }

       item{
           CustomButton(
               callback={
                   if(!searching){
                       bm.listLocalDevices()
                   }else{
                       bm.stopScanning();
                       data.clear()
                   }
               },
               enabled=true,
               text=if(searching) "Stop Search" else "Search Devices")

       }

       items(itemsList, key = { it.key }){(address, name) ->
           Row(
               verticalAlignment = Alignment.CenterVertically,
               horizontalArrangement = Arrangement.SpaceBetween,
               modifier = Modifier.fillMaxWidth()
           ){
               Text("$address - $name")
               CustomButton({ bm.pair(address) }, true, "Pair")
               CustomButton(callback = { bm.connect(address) }, enabled = true, text="Send")
           }
       }

       item{
           Text("Received Data: ")
           Text(received.ifEmpty { "None" })
       }

   }
}