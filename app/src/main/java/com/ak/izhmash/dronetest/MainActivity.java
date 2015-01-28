package com.ak.izhmash.dronetest;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.sensorcon.sensordrone.DroneEventHandler;
import com.sensorcon.sensordrone.DroneEventObject;
import com.sensorcon.sensordrone.android.Drone;
import com.sensorcon.sensordrone.android.tools.DroneConnectionHelper;

import org.w3c.dom.Text;

public class MainActivity extends ActionBarActivity {

    // UI Elements
    Button btnConnect;
    Button btnDisconnect;
    Button btnMeasure;
    TextView tvStatus;
    TextView tvTemperature;
    TextView tvHumidity;

    // Sensordrone Objects
    Drone myDrone;
    DroneEventHandler myDroneEventHandler;
    DroneConnectionHelper myHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDrone = new Drone();
        myHelper = new DroneConnectionHelper();

        tvStatus = (TextView) findViewById(R.id.main_tv_connection_status);
        tvTemperature = (TextView) findViewById(R.id.main_tv_temperature);
        tvHumidity = (TextView) findViewById(R.id.main_tv_humidity);

        btnConnect = (Button) findViewById(R.id.main_btn_connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myDrone.isConnected) {
                    genericDialog("Alert", "You are already connected to a SensorDrone");
                } else {
                    myHelper.connectFromPairedDevices(myDrone, MainActivity.this);
                }
            }
        });

        btnDisconnect = (Button) findViewById(R.id.main_btn_disconnect);
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myDrone.isConnected) {
                    myDrone.disableTemperature();
                    myDrone.disableHumidity();
                    myDrone.setLEDs(0, 0, 0);
                    myDrone.disconnect();
                } else {
                    genericDialog("Alert", "You are not connected to a SensorDrone");
                }
            }
        });

        btnMeasure = (Button) findViewById(R.id.main_btn_measure);
        btnMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myDrone.isConnected) {
                    myDrone.measureTemperature();
                    myDrone.measureHumidity();
                    myDrone.setLEDs(238, 130, 238);
                } else if (myDrone.isConnected && !myDrone.temperatureStatus ) {
                    genericDialog("Alert", "The temperature sensor has not been enabled");
                } else if (myDrone.isConnected && !myDrone.humidityStatus ) {
                        genericDialog("Alert", "The humidity sensor has not been enabled");
                } else {
                    genericDialog("Alert", "You are not currently connected to a Sensordrone");
                }
            }
        });


        myDroneEventHandler = new DroneEventHandler() {
            @Override
            public void parseEvent(DroneEventObject droneEventObject) {
                if (droneEventObject.matches(DroneEventObject.droneEventType.CONNECTED)) {
                    myDrone.setLEDs(0, 0, 126);
                    updateTextViewFromUI(tvStatus, "Connected");
                    myDrone.enableTemperature();
                    myDrone.enableHumidity();
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.DISCONNECTED)) {
                    updateTextViewFromUI(tvStatus, "Not connected");
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.CONNECTION_LOST)) {
                    updateTextViewFromUI(tvStatus, "Connection lost");
                    uiToast("Connection lost");
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.TEMPERATURE_ENABLED)) {
                    myDrone.measureTemperature();
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.HUMIDITY_ENABLED)) {
                    myDrone.measureHumidity();
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.TEMPERATURE_MEASURED)) {
                    String temp = String.format("%.2f \u00b0C", myDrone.temperature_Celsius);
                    updateTextViewFromUI(tvTemperature, temp);
                    uiToast("Temperature updated");
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.HUMIDITY_MEASURED)) {
                    //uiToast("Made it!");  //debug
                    String humid = String.format("%.2f ", myDrone.humidity_Percent);
                    updateTextViewFromUI(tvHumidity, humid);
                    uiToast("Humidity updated");
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.TEMPERATURE_DISABLED)) {
                    //Not in use
                } else if (droneEventObject.matches(DroneEventObject.droneEventType.HUMIDITY_DISABLED)) {
                    //Not in use
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        myDrone.registerDroneListener(myDroneEventHandler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        myDrone.unregisterDroneListener(myDroneEventHandler);
    }

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

    public void genericDialog(String title, String msg) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    public void updateTextViewFromUI(final TextView textView, final String text) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    public void uiToast(final String msg) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}


