package edu.umich.mbowyer.bluetoothsensortutorial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    //LAYOUT COMPONENTS
    Button sendBTDataButton;
    TextView StatusView;
    TextView RecievedDataView;
    EditText dataToWrite;
    ListView deviceListView;
    TextView GyroData;

    //BLUETOOTH RELATED VARIABLES
    Bluetooth myBT;
    ArrayList<String> deviceList;
    ArrayAdapter<String> deviceListAdapter;
    public final int REQUEST_ENABLE_BT = 1;

    //SENSOR RELATED VARIABLES
    private SensorManager sManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //ASSIGNING LAYOUT Components
        sendBTDataButton = (Button) findViewById(R.id.sendDataButton);
        StatusView= (TextView) findViewById(R.id.StatusView);
        dataToWrite = (EditText) findViewById(R.id.textToSend);
        RecievedDataView = (TextView) findViewById(R.id.textReadIn);
        deviceListView = (ListView)findViewById(R.id.BTPairedDevicesListView);
        GyroData = (TextView) findViewById(R.id.GyroData);

        //initalize Bluetooth handler and get device list
        myBT = new Bluetooth(this,mHandler);// create instance of bluetooth object
        deviceList=myBT.getDeviceList();//save list of paired devices.

        //get bluetooth device list to display to user
        deviceListAdapter = new ArrayAdapter<String>(this,R.layout.bt_list_element_layout,deviceList);
        deviceListView.setAdapter(deviceListAdapter);

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedDeviceName = deviceList.get(position);//get device name
                connectService(selectedDeviceName);//start connectino with device
            }
        });

        //Initialize Sensor Manager
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

    }


    ///////////*BLUETOOTH FUNCTIONS*////////////
    public void SendBTData(View v)//send bluetooth data
    {
        String str_send = dataToWrite.getText().toString();//create string to send from EditText
        myBT.sendMessage(str_send);//send the string
    }
    public void updateRecievedDataTextView(String rec_str)//update textview of received data
    {
        RecievedDataView.setText(rec_str);//update textview
    }

    public void connectService(String deviceName){//Creates connection to remote device
        try {
            StatusView.setText("Connecting to "+deviceName);//update activity of connection status
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//grab bluetooth adapter of the device(this is BT radio of the device)
            if (bluetoothAdapter.isEnabled()) {//check if BT is enabled on the device.
                myBT.start();//start bluetooth connection in BT object
                myBT.connectDevice(deviceName);//connect to device
                Log.v("myDebug", "Btservice started - listening");
                StatusView.setText("Connected to "+ myBT.connectedDeviceName);//update activity of connection status
            } else {//bleutooth is not enabled.
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //create an intent to request to turn on bluetooth
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);// start bluetooth if application enableBTIntent returns true
                Log.w("myDebug", "Btservice started - bluetooth is not enabled, requesting access");
                StatusView.setText("Bluetooth Not enabled");//update activity of connection status
            }
        } catch(Exception e){
            Log.v("myDebug", "Unable to start bt ",e);
            StatusView.setText("Unable to connect " +e);
        }
    }

    //Handle messages which are sent from bluetooth threads
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Bluetooth.MESSAGE_STATE_CHANGE://when state is changed IE connected, COnnected, disconnected.
                    Log.d("myDebug", "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    break;
                case Bluetooth.MESSAGE_WRITE:// This device attempted to send a message to the remote device
                    Log.d("myDebug", "MESSAGE_WRITE ");
                    break;
                case Bluetooth.MESSAGE_READ://this dewice received a message, and saved it in the myBTRecievedString
                    Log.d("myDebug", "MESSAGE_READ =" + myBT.recievedString);
                    updateRecievedDataTextView(myBT.recievedString);
                    break;
                case Bluetooth.MESSAGE_DEVICE_NAME://the device name when the device has become connected
                    Log.d("myDebug", "MESSAGE_DEVICE_NAME "+myBT.connectedDeviceName);
                    break;
                case Bluetooth.MESSAGE_TOAST://when a message failed to send.
                    Log.d("myDebug", "MESSAGE_TOAST "+msg);
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    ///////////*SENSOR FUNCTIONS*////////////
    //when this Activity starts
    @Override
    protected void onResume()
    {
        super.onResume();
        /*register the sensor listener to listen to the gyroscope sensor, use the
        callbacks defined in this class, and gather the sensor information as quick
        as possible*/
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_FASTEST);
    }

    //When this Activity isn't visible anymore
    @Override
    protected void onStop()
    {
        //unregister the sensor listener
        sManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1)
    {
        //Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        //if sensor is unreliable, return void
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

        //else it will output the Roll, Pitch and Yawn values
        GyroData.setText("Orientation X (Roll) :"+ Float.toString(event.values[2]) +"\n"+
                "Orientation Y (Pitch) :"+ Float.toString(event.values[1]) +"\n"+
                "Orientation Z (Yaw) :"+ Float.toString(event.values[0]));


        myBT.sendMessage("Orientation X (Roll) :"+ Float.toString(event.values[2]) +"\n"+
                "Orientation Y (Pitch) :"+ Float.toString(event.values[1]) +"\n"+
                "Orientation Z (Yaw) :"+ Float.toString(event.values[0]));//send the string
    }
}
