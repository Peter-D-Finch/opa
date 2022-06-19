/**
 * @author Peter Finch
 * 6/11/2022
 */

package com.dataxign.mark.aasruckmarchpacer.mdp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ObanSensor {

    private Context ctxt;
    private Activity act;

    public int given_roster_id; // The roster number the app will look for to read data from
    private final int ODIC_MANUF_ID = 2154; // The BLE ODIC manufacturer ID
    private final int bg = 7; // Beginning index in the advertisement data

    // Data that the sensor advertises. The message is 21 bytes show by following comments...
    private byte message_length; // byte position 0
    private byte message_format_type; // byte position 1
    private byte oban_device_type; // byte position 2. Unsigned int?
    private byte group_code; // byte position 3. Unsigned int?
    private byte[] roster_id; // byte positions 4 & 5. Big-Endian format, MSB-LSB.
    private byte source_component; // byte position 6. Unsigned int?
    private byte command; // byte position 7
    private byte payload_length; // byte position 8
    private byte HSI; // byte position 9. P8u.1:0 (HSI is 7.5). Unsigned int (0-170)?
    private byte HR; // byte position 10. Unsigned int. I8u
    private byte ECT; // byte position 11. P8u.1:32 (temp is 37C)
    private byte SKT; // byte position 12. P8u.1:20 (temp is 22.3C)
    private byte NII; // byte position 13. New NII algorithm
    private byte Risk; // byte position 14. Only included if Risk property was set to true.
    private byte Confidence; // byte position 15. Only included if Confidence property ^^^
    private byte Battery; // byte position 16. Only included if Battery Life property ^^^^
    private byte[] time_stamp; // byte positions 17, 18, 19, & 20. 32-bit second count

    // This handles reading data from the sensor when we bump into it with the BT scanner
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private final ScanCallback scanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {
            // Get a ScanRecord which holds the data from the scan
            ScanRecord scanRecord = result.getScanRecord();

            // Get the sensor advertising, and check what the roster number of the device is
            byte[] sensor_data = scanRecord.getBytes();
            int device_roster_id = get_u8x(Arrays.copyOfRange(sensor_data, 4+bg, 6+bg));

            // Get the manufacturer key
            SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
            int manufacturer_id = 0;
            String device_name = scanRecord.getDeviceName();
            for(int i = 0; i < manufacturerData.size(); i++){
                manufacturer_id = manufacturerData.keyAt(i);
            }

            // If the device is our registered one, then update the sensor values
            boolean name_correct = (device_name == null);
            boolean manid_correct = (manufacturer_id == ODIC_MANUF_ID);
            boolean roster_correct = (device_roster_id == given_roster_id);
            if (name_correct && manid_correct && roster_correct) {
                message_length = sensor_data[0+bg];
                message_format_type = sensor_data[1+bg];
                oban_device_type = sensor_data[2+bg];
                group_code = sensor_data[3+bg];
                roster_id = Arrays.copyOfRange(sensor_data, 4+bg, 6+bg);
                source_component = sensor_data[6+bg];
                command = sensor_data[7+bg];
                payload_length = sensor_data[8+bg];
                HSI = sensor_data[9+bg];
                HR = sensor_data[10+bg];
                ECT = sensor_data[11+bg];
                SKT = sensor_data[12+bg];
                NII = sensor_data[13+bg];
                Risk = sensor_data[14+bg];
                Confidence = sensor_data[15+bg];
                Battery = sensor_data[16+bg];
                time_stamp = Arrays.copyOfRange(sensor_data, 17+bg, 21+bg);
                Log.e("Sensor: scanCallback", "STATE: " + to_string());
            }
        }
    };

    // Constructor
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ObanSensor(int given_roster_id, Context ctxt, Activity act) {
        this.given_roster_id = given_roster_id;
        this.ctxt = ctxt;
        this.act = act;

        // Setting up Bluetooth stuff
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

        List<ScanFilter> filters = null;
        ScanSettings scanSettings = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L)
                    .build();
        }
        scanner.startScan(filters, scanSettings, scanCallback);
    }

    // Methods to access the fields of the class

    public int getRoster() {
        return get_u8x(roster_id);
    }

    public int getHR() {
        return get_unsigned(HR);
    }

    public int getBattery() {
        return Battery;
    }

    // Binary stuff for reading the sensor data

    /**
     * Returns the integer value of an unsigned byte
     * @param b The unsigned byte
     * @return The value of the unsigned byte
     */
    private int get_unsigned(byte b) {
        return b & 0xFF;
    }

    /**
     * Returns the integer value from an unsigned string of 8n bits
     * @param b Array of bytes
     * @return The integer value
     */
    private int get_u8x(byte[] b) {
        ByteBuffer rr = ByteBuffer.wrap(b);
        rr.position(0);
        int result = rr.getShort();
        return result;
    }

    public String to_string() {
        return "rostID: " + getRoster() + " ECT: " + ECT + " HR: " + getHR();
    }
}
