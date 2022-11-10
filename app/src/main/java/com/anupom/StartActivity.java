package com.anupom;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.anupom.cameraroll.R;

public class StartActivity extends AppCompatActivity {

    private Button submit;
    private EditText timeInterval, fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        submit          = findViewById(R.id.submit);
        timeInterval    = findViewById(R.id.time_interval);
        fileName        = findViewById(R.id.file_name);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitData();
            }
        });
    }
    // *** end of oncreate method ***

    private void submitData() {
        String time_interval    = timeInterval.getText().toString().trim();
        String file_name        = fileName.getText().toString().trim();

        if(checkGpsStatus()) {
            if(time_interval.length() > 0 && file_name.length() > 0) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("time_interval", time_interval);
                intent.putExtra("file_name", file_name);
                startActivity(intent);
            }
            else {
                Toast.makeText(getApplicationContext(), "Please enter the details!", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Please turn on the GPS!", Toast.LENGTH_LONG).show();
            promptGPSEnableOp();
        }

    }

    public boolean checkGpsStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        boolean GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return GpsStatus;
    }

    // handle the GPS turn on operation
    private void promptGPSEnableOp() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Info");
        alertBuilder.setMessage("You have to turn on the location...")
                .setCancelable(false)
                .setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                        Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent1);

                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

}