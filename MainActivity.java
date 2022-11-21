package com.example.audiotracker;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import android.provider.Settings.Secure;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private MediaRecorder mediaRecorder;
    private ArrayList<File> outputFiles = new ArrayList<>();
    EditText frequency, duration;
    TextView imeitext,datetime,latitude,longitude;
    Button start,upload,imei;
    private static int  MIC_PERMISSION_CODE = 200;
    StorageReference audioStorageRef;
    private static final int REQUEST_CODE = 101;
    protected LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imeitext = findViewById(R.id.imeinumber);
        imei = findViewById(R.id.button);
        frequency = findViewById(R.id.frequency);
        duration = findViewById(R.id.duration);
        start = findViewById(R.id.submit);
        upload = findViewById(R.id.upload);
        datetime = findViewById(R.id.datetime);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        audioStorageRef = storageRef.child("audio");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        /* Taking Permissions from user to access Loaction */
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        /* To get the IMEI or device ID for the device*/
        imei.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE);
                    return;
                }
                String android_id = Secure.getString(getApplication().getContentResolver(),Secure.ANDROID_ID);
                imeitext.setText(android_id);
            }
        });

        //To access date and time
        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
        datetime.setText(currentDateTimeString);

        //Check the permissions for recording
        if (isMicPresent()) getMicsPermission();

        //start recording
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStart(v);
            }
        });

        //upload recording to the Firebase
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnUpload(v);
            }
        });

    }

    @Override
    public void onLocationChanged (Location location){
        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        latitude.setText(location.getLatitude()+" ");
        longitude.setText(location.getLongitude()+ " ");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    public void btnStart(View v) {
        try {
            int f,d;
            f= Integer.parseInt(String.valueOf(frequency.getText()));
            d= Integer.parseInt(String.valueOf(duration.getText()));
            int du= 1000*d;
            int fr= f*2000;
            Toast.makeText(this, "Recording has started", Toast.LENGTH_LONG).show();
            for(int i=0;i<3;i++){
                System.out.println("Recording - "+i);
                recordVoice(du);
                Thread.sleep(fr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recordVoice(int d) throws IOException {
        int du = d;
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        File outputFile = getRecordingFile();
        outputFiles.add(outputFile);
        mediaRecorder.setOutputFile(outputFile.getPath());
        mediaRecorder.prepare();
        mediaRecorder.start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mediaRecorder != null) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                }
            }
        }, du);
    }

    public boolean isMicPresent(){
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            return false;
        } else {
            return true;
        }
    }

    public void getMicsPermission(){
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_CODE);
        }
    }

    private File getRecordingFile(){
        ContextWrapper contextWrapper;
        contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory =  contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        String path;
        String currentDateTimeString1 = java.text.DateFormat.getDateTimeInstance().format(new Date());
        path = imeitext.getText().toString() + "_"+ longitude.getText().toString() + "_"+ latitude.getText().toString()
        + "_"+ currentDateTimeString1;
        return new File(musicDirectory, path+ ".mp3");

    }
    private void btnUpload(View view){
        uploadFileToDb(this, outputFiles);
    }


    private void uploadFileToDb(Context ctx, ArrayList<File> recordedFiles) {
        for (int i = 0; i < recordedFiles.size(); i++) {
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("audio/mpeg")
                    .build();
            File recordedFile = recordedFiles.get(i);
            Uri file = Uri.fromFile(new File(recordedFile.getPath()));
            StorageReference riversRef = audioStorageRef.child(file.getLastPathSegment());
            UploadTask uploadTask = riversRef.putFile(file, metadata);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(ctx, "Failed to upload file", Toast.LENGTH_SHORT).show();
                    System.out.println(exception.getMessage());
                    System.out.println(exception.getStackTrace());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(ctx, "Uploaded file successfully", Toast.LENGTH_SHORT).show();
                    recordedFiles.remove(recordedFile);
                }
            });
        }

    }
}


