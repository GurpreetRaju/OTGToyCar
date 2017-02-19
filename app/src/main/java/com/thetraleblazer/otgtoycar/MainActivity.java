package com.thetraleblazer.otgtoycar;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbEndpoint;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.app.PendingIntent;
import android.content.Context;
import android.view.View;
import android.widget.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

import static java.lang.System.in;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.thetraleblazer.otgtoycar.USB_PERMISSION";
    private UsbDevice deviceFound = null;
    private static UsbDeviceConnection usbDeviceConnection = null;
    private UsbInterface usbInterface = null;
    private UsbEndpoint endpointOut = null;
    private UsbEndpoint endpointIn = null;
    private PendingIntent mPermissionIntent;
    private static int TIMEOUT = 0;
    private boolean forceClaim = true;
    private Button connectButton;
    private Button buttonFW;
    private Button buttonRV;
    private Button buttonSP;
    private Button buttonLT;
    private Button buttonRT;
    private Button buttonST;
    private TextView message;
    private static final String TAG = "MainActivity";
    boolean hasPermission;

    UsbManager UsbManagerOBJ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //control buttons
        buttonFW = (Button) findViewById(R.id.button2);
        buttonRV = (Button) findViewById(R.id.button4);
        buttonSP = (Button) findViewById(R.id.button5);
        buttonLT = (Button) findViewById(R.id.button);
        buttonRT = (Button) findViewById(R.id.button3);
        buttonST = (Button) findViewById(R.id.button6);
        //message box to show all connection information
        message = (TextView) findViewById(R.id.message);
        //Connect button to connect to USB device
        connectButton = (Button) findViewById(R.id.connectButton);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    deviceFound = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(deviceFound != null){
                            Toaster("Permission granted.");
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + deviceFound);
                    }
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                deviceFound = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (deviceFound != null) {
                    closeUsb();
                }
            }
        }
    };

    private void connectUsb(){
        if(!hasPermission) {
            UsbManagerOBJ = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = UsbManagerOBJ.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            message.setText("Started.");

            while (deviceIterator.hasNext()) {
                deviceFound = deviceIterator.next();

                if (deviceFound != null) {
                    UsbManagerOBJ.requestPermission(deviceFound, mPermissionIntent);
                    hasPermission = UsbManagerOBJ.hasPermission(deviceFound);
                    message.setText("asking permission now");
                    if (hasPermission) {
                        usbInterface = deviceFound.getInterface(1);
                        endpointOut = usbInterface.getEndpoint(0);
                        message.setText("Endpoint direction: " + endpointOut.getType()+ "Vendor id: " + deviceFound.getVendorId() + "Product ID: " + deviceFound.getProductId());
                        usbDeviceConnection = UsbManagerOBJ.openDevice(deviceFound);
                        usbDeviceConnection.claimInterface(usbInterface, true);
                        Toaster("Connection Successful.");
                        final int RQSID_SET_LINE_CODING = 0x20;
                        final int RQSID_SET_CONTROL_LINE_STATE = 0x22;

                        int usbResult;
                        usbResult = usbDeviceConnection.controlTransfer(0x21, // requestType
                                RQSID_SET_CONTROL_LINE_STATE, // SET_CONTROL_LINE_STATE
                                0, // value
                                0, // index
                                null, // buffer
                                0, // length
                                0); // timeout

                        Toast.makeText(
                                MainActivity.this,
                                "controlTransfer(SET_CONTROL_LINE_STATE): " + usbResult,
                                Toast.LENGTH_LONG).show();

                        // baud rate = 9600
                        // 8 data bit
                        // 1 stop bit
                        byte[] encodingSetting = new byte[]{(byte) 0x80, 0x25, 0x00,
                                0x00, 0x00, 0x00, 0x08};
                        usbResult = usbDeviceConnection.controlTransfer(0x21, // requestType
                                RQSID_SET_LINE_CODING, // SET_LINE_CODING
                                0, // value
                                0, // index
                                encodingSetting, // buffer
                                7, // length
                                0); // timeout
                        Toast.makeText(MainActivity.this,
                                "controlTransfer(RQSID_SET_LINE_CODING): " + usbResult,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toaster("Permission not granted!");
                    }
                } else {
                    Toaster("No device found");
                }
            }
        }
    }

    private void interfaceSearch() {
        for (int i = 0; i < deviceFound.getInterfaceCount(); i++) {
            UsbInterface usbif = deviceFound.getInterface(i);

            UsbEndpoint tOut = null;
            UsbEndpoint tIn = null;

            int tEndpointCnt = usbif.getEndpointCount();
            if (tEndpointCnt >= 2) {
                for (int j = 0; j < tEndpointCnt; j++) {
                    if (usbif.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (usbif.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT) {
                            tOut = usbif.getEndpoint(j);
                        } else if (usbif.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN) {
                            tIn = usbif.getEndpoint(j);
                        }
                    }
                }

                if (tOut != null && tIn != null) {
                    // This interface have both USB_DIR_OUT
                    // and USB_DIR_IN of USB_ENDPOINT_XFER_BULK
                    usbInterface = usbif;
                    endpointOut = tOut;
                    endpointIn = tIn;
                }
            }

        }

        if (usbInterface == null) {
            message.setText("No suitable interface found!");
        } else {
            message.setText("UsbInterface found: "
                    + usbInterface.toString() + "\n\n"
                    + "Endpoint OUT: " + endpointOut.toString() + "\n\n"
                    + "Endpoint IN: " + endpointIn.toString());
        }
    }
    @Override
    public void onStart() {
        super.onStart();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectUsb();
            }
        });
        buttonFW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toaster(""+endpointOut.getDirection());
                byte[] bytes = new byte[1];
                bytes[0] = (byte) 1;
                sendData(bytes);
                //usbDeviceConnection.bulkTransfer(endpointOut, bytes, bytes.length, TIMEOUT);usbDeviceConnection.bulkTransfer(endpointOut, bytes, bytes.length, TIMEOUT);
            }
        });
        buttonRV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) 2;
                sendData(bytes);
            }
        });
        buttonSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) 5;
                sendData(bytes);
            }
        });
        buttonLT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) 3;
                sendData(bytes);
            }
        });
        buttonRT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) 4;
                sendData(bytes);
            }
        });
        buttonST.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) 6;
                sendData(bytes);
            }
        });
    }

    private void sendData(byte[] bytes){
        usbDeviceConnection.bulkTransfer(endpointOut, bytes, bytes.length, TIMEOUT);
    }

    @Override
    public void onStop() {
        closeUsb();
        unregisterReceiver(mUsbReceiver);
        super.onStop();
    }

    private void closeUsb(){
        usbDeviceConnection.releaseInterface(usbInterface);
        usbDeviceConnection.close();
        deviceFound = null;
        usbDeviceConnection = null;
        usbInterface = null;
        UsbManagerOBJ = null;
    }
    private void Toaster(String s){
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
    }
}
