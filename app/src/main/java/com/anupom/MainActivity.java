package com.anupom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.anupom.cameraroll.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button cameraButton, captureImageOp;
    private PreviewView mPreviewView;
    private TextView counterTextView;
    RelativeLayout mainLayout;

    private int REQUEST_CODE_PERMISSIONS = 1001;

    private final String[] REQUIRED_PERMISSIONS = new String[] {
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
    };

    private Executor executor = Executors.newSingleThreadExecutor();
    private final Timer timer1 = new Timer();

    //----------------------------------------------
    private int timeInterval = 0;
    private String fileName = "";
    private String latitude = "", longitude = "";
    public int photoCaptureCounter = 0;
    private Location currentLocation;
    //----------------------------------------------

    private ImageView screenSetting;
    private TextView screenSettingText;
    private LinearLayout screenSettingLayout;
    private boolean screenSettingFlag = false;

    ImageCapture imageCapture = null;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreviewView        = findViewById(R.id.camera_preview);
        captureImageOp      = findViewById(R.id.capture_image_op);
        mainLayout          = findViewById(R.id.main_layout);
        counterTextView     = findViewById(R.id.count_down);
        screenSetting       = findViewById(R.id.screen_setting);
        screenSettingText   = findViewById(R.id.screen_text);
        screenSettingLayout = findViewById(R.id.screen_setting_layout);

        setScreenOnFlag();

        screenSettingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // toggle flag variable
                if(screenSettingFlag)
                    screenSettingFlag = false;
                else
                    screenSettingFlag = true;

                setScreenOnFlag();
            }
        });

        captureImageOp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(timer1!=null)
                    timer1.cancel();

                finish();
            }
        });

        //------------------------------------------------------------------------------------------------------------
        // get the details from intent
        if (getIntent().hasExtra("time_interval") && getIntent().hasExtra("file_name")) {
            timeInterval    = Integer.parseInt(getIntent().getStringExtra("time_interval").toString().trim());
            fileName        = getIntent().getStringExtra("file_name").toString().trim();
        }
        //------------------------------------------------------------------------------------------------------------

        photoCaptureCounter = 0;    // initialize the photo capture counter

        if(allPermissionsGranted()) {
            if(checkGpsStatus()) {
                startCamera();      //start camera if permission has been granted by user
            }
            else {
                //Toast.makeText(getApplicationContext(), "Turn on the GPS", Toast.LENGTH_LONG).show();
                promptGPSEnableOp();
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

    }
    //*** end of oncreate

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(timer1!=null)
            timer1.cancel();
    }

    private void setScreenOnFlag() {
        if(!screenSettingFlag) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            screenSetting.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_open_in_browser_24));
            screenSettingText.setText("Screen on always");
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // clear the flag
            screenSetting.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_browser_not_supported_24));
            screenSettingText.setText("Screen off allowed");
        }
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

                        /*Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent1);*/

                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(timer1!=null)
                            timer1.cancel();

                        dialogInterface.cancel();

                        finish();
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public boolean checkGpsStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        boolean GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return GpsStatus;
    }

    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                if(checkGpsStatus()) {
                    startCamera(); //start camera if permission has been granted by user
                }
                else {
                    //Toast.makeText(getApplicationContext(), "Turn on the GPS", Toast.LENGTH_LONG).show();
                    promptGPSEnableOp();
                }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
        else {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void startCamera() {
        //-------------------------------------------------------------------------------------------------------------------------
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        //-------------------------------------------------------------------------------------------------------------------------

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        //HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        /*if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }*/

        imageCapture = builder.setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()).build();

        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);

        //Set the schedule function and rate
        timer1.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

                File dirName = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CameraRoll"+dirDateFormat.format(new Date()));

                if(!dirName.exists()) {
                    dirName.mkdirs();
                }

                File file = new File(dirName, fileName+"_"+mDateFormat.format(new Date())+ ".jpg");

                ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

                if(latitude.length() > 0 && longitude.length() > 0) {
                    File finalFile = file;
                    imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback () {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            try {
                                if(writeGPSCoordinate(finalFile, latitude, longitude)) {
                                    writeLocData(finalFile, Double.parseDouble(latitude), Double.parseDouble(longitude));
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            animBlinkEffect();

                            System.out.println("Saved successfully!"+"::"+latitude+"::"+longitude);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException error) {
                            error.printStackTrace();
                        }
                    });
                }
                else {
                    printMessage();
                }
            }
        }, 0, timeInterval);
    }

    public void writeLocData(File photo, double latitude, double longitude) throws IOException {
        ExifInterface exif = null;

        try{

            exif = new ExifInterface(photo.getCanonicalPath());

            if (exif != null) {
                double alat     = Math.abs(latitude);
                double along    = Math.abs(longitude);

                String[] cord = convert(alat, along).split("_");

                if(cord.length >= 2) {
                    String stringLati   = cord[0];
                    String stringLongi  = cord[1];

                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, getLonGeoCoordinates(currentLocation));
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, currentLocation.getLongitude() < 0 ? "W" : "E");

                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, getLatGeoCoordinates(currentLocation));
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, currentLocation.getLatitude() < 0 ? "S" : "N");

                    exif.saveAttributes();

                    /*String o2 = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                    String o3 = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                    String o4 = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                    String o5 = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

                    System.out.println("2nd:"+o2+"::"+o3+"::"+o4+"::"+o5);*/
                    //-----------------------------------------------------------------

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String convert(double latitude, double longitude) {
        StringBuilder builder = new StringBuilder();

        if (latitude < 0) {
            builder.append("S ");
        } else {
            builder.append("N ");
        }

        String latitudeDegrees = Location.convert(Math.abs(latitude), Location.FORMAT_SECONDS);
        String[] latitudeSplit = latitudeDegrees.split(":");
        builder.append(latitudeSplit[0]);
        builder.append("°");
        builder.append(latitudeSplit[1]);
        builder.append("'");
        builder.append(latitudeSplit[2]);
        builder.append("\"");

        builder.append("_");    // lat_long

        if (longitude < 0) {
            builder.append("W ");
        } else {
            builder.append("E ");
        }

        String longitudeDegrees = Location.convert(Math.abs(longitude), Location.FORMAT_SECONDS);
        String[] longitudeSplit = longitudeDegrees.split(":");
        builder.append(longitudeSplit[0]);
        builder.append("°");
        builder.append(longitudeSplit[1]);
        builder.append("'");
        builder.append(longitudeSplit[2]);
        builder.append("\"");

        return builder.toString();
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            //------------------------------------
            currentLocation = loc;
            //------------------------------------

            longitude   = String.valueOf(loc.getLongitude());
            latitude    = String.valueOf(loc.getLatitude());

            //------- To get city name from coordinates --------
            /*String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                    cityName = addresses.get(0).getLocality();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            String s = longitude + "\n" + latitude + "\n\nMy Current City is: " + cityName;*/

        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    private void animBlinkEffect() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // set the image capture counter
                photoCaptureCounter++;
                counterTextView.setText(String.valueOf(photoCaptureCounter));

                ObjectAnimator anim1 = ObjectAnimator.ofFloat(mainLayout, "alpha", 0f, 1f);
                anim1.setDuration(250);                      // Duration in milliseconds
                anim1.setRepeatCount(Animation.ABSOLUTE);
                anim1.start();
            }
        });
    }

    public String getLonGeoCoordinates(Location location) {
        if (location == null) return "0/1,0/1,0/1000";

        // You can adapt this to latitude very easily by passing location.getLatitude()
        String[] degMinSec = Location.convert(location.getLongitude(), Location.FORMAT_SECONDS).split(":");

        if(Integer.parseInt(degMinSec[0]) < 0) {
            degMinSec[0] = String.valueOf(Math.abs(Integer.parseInt(degMinSec[0])));
        }

        String lon = degMinSec[0] + "/1," + degMinSec[1] + "/1," + degMinSec[2] + "/1000";

        return lon;
    }

    public String getLatGeoCoordinates(Location location) {
        if (location == null) return "0/1,0/1,0/1000";

        // You can adapt this to latitude very easily by passing location.getLatitude()
        String[] degMinSec = Location.convert(location.getLatitude(), Location.FORMAT_SECONDS).split(":");

        return degMinSec[0] + "/1," + degMinSec[1] + "/1," + degMinSec[2] + "/1000";
    }

    public void printMessage() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Getting GPS coordinate...", Toast.LENGTH_LONG).show();
            }
        });
    }

    // method to write the GPS coordinate on the image
    private boolean writeGPSCoordinate(File fileName, String latitude, String longitude) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(fileName.getCanonicalPath());

            android.graphics.Bitmap.Config bitmapConfig = bitmap.getConfig();

            // set default bitmap config if none
            if(bitmapConfig == null) {
                bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
            }

            // resource bitmaps are imutable, so we need to convert it to mutable one
            bitmap = bitmap.copy(bitmapConfig, true);

            FileOutputStream out = new FileOutputStream(fileName);

            // NEWLY ADDED CODE STARTS HERE |
            Canvas canvas = new Canvas(bitmap);

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);    // Text Color
            paint.setTextSize(40);          // Text Size

            String coordinateValue = latitude+", "+longitude;   // prepare the coordinate string

            canvas.drawBitmap(bitmap, 0, 0, paint);
            canvas.drawText(coordinateValue, 20, bitmap.getHeight()-20, paint);

            // NEWLY ADDED CODE ENDS HERE ]

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }


}