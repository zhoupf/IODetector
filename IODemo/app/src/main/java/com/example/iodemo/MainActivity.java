package com.example.iodemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    PrintWriter writer = null;

    private int screen_width;
    private int screen_height;
    private float GPSSnrUpperThreshold = 0f;
    private float GPSSnrMiddleThreshold = 0f;
    private float GPSSnrLowerThreshold = 0f;
    private float GPSCountUpperThreshold = 0f;
    private float GPSCountMiddleThreshold = 0f;
    private float GPSCountLowerThreshold = 0f;

    private float indoorConfidence = 0f;
    private float semioutdoorConfidence = 0f;
    private float outdoorConfidence = 0f;

    private TelephonyManager telephonyManager;

    private SensorManager IOSensorManager;
    private Sensor IOProximity;
    private Boolean ProximitySensorAvailable = false;
    private float ProximityValue;
    private Calendar cal = Calendar.getInstance();
    private Sensor IOLight;
    private Boolean LightSensorAvailable = false;
    private float LightValue;
    private Sensor IOMagnetism;
    private Boolean MagnetismSensorAvailable = false;
    private float MagnetismValue;

    private int round_count = 0;
    private float[] GPSSnrValue = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private float[] GPSSnrTrend = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private float GPSSnrTrendMax = 0.0f;

    private float[] cellularSNR = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private float currentCellularSNR = 0f;
    private int previousCellID = 0;
    private int currentCellID = 0;

    private float[] magnetismStrength = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private float magnetismVariation =0f;

    private boolean firstRound = true;
    private boolean clickOnce = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        View view = this.getWindow().getDecorView();
//        view.setBackgroundColor(Color.BLUE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);     //  Fixed Portrait orientation

        setProperThreshold();

        IOSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //list all available sensors
        List<Sensor> IOList = IOSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : IOList) {
            if (sensor.getType() == Sensor.TYPE_LIGHT) {
                LightSensorAvailable = true;
            }
            if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                ProximitySensorAvailable = true;
            }
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                MagnetismSensorAvailable = true;
            }
        }
        if (LightSensorAvailable) {
            IOLight = IOSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            IOSensorManager.registerListener(this, IOLight, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (ProximitySensorAvailable) {
            IOProximity = IOSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            IOSensorManager.registerListener(this, IOProximity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (MagnetismSensorAvailable) {
            IOMagnetism = IOSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            IOSensorManager.registerListener(this, IOMagnetism, SensorManager.SENSOR_DELAY_NORMAL);
        }

        telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void setProperThreshold(){

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screen_width = size.x; //width
        screen_height = size.y; //height

        if(screen_width < 1200) { // 1080
            GPSSnrUpperThreshold = 23;
            GPSSnrMiddleThreshold = 18;
            GPSSnrLowerThreshold = 15;
        }
        else { // 1440
            GPSSnrUpperThreshold = 25;
            GPSSnrMiddleThreshold = 22;
            GPSSnrLowerThreshold = 19;
        }

        GPSCountUpperThreshold = 4.5f;
        GPSCountMiddleThreshold = 3.5f;
        GPSCountLowerThreshold = 1.5f;
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            ProximityValue = event.values[0];
        }
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            LightValue = event.values[0];
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            MagnetismValue = (float) Math.sqrt(event.values[0]*event.values[0] + event.values[1] * event.values[1] + event.values[2]*event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public LocationManager manager;

    public void initLocation() {
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please open GPS service", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location permission required")
                        .setMessage("You have to give the permission to access location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create().show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            manager.addGpsStatusListener(gpsStatusListener);
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        }
    }

    private GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                return;
            }
            GpsStatus gpsStatus = manager.getGpsStatus(null);
            int maxSatellites = gpsStatus.getMaxSatellites();
            Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
            int count_gps = 0;
            int count_gln = 0;
            float snr_gps = 0;
            float snr_gln = 0;
            float avg_snr_gps;
            float avg_snr_gln;

            StringBuilder sb = new StringBuilder();
            StringBuilder satelliteInfo = new StringBuilder();
            System.out.println("found satellites === " + iters.hasNext());
            while (iters.hasNext() && count_gps <= maxSatellites) {
                GpsSatellite s = iters.next();
                int prn = s.getPrn();
                float snr = s.getSnr();
                if (snr > 0.0){
                    if (prn < 33){ // gps 1-32
                        count_gps++;
                        snr_gps += snr;
                    }
                    if ((prn > 64 && prn < 89)||(prn > 37 && prn < 62)){ // gln 65-88 and 38-61
                        count_gln++;
                        snr_gln += snr;
                    }
                }
            }
            avg_snr_gps = snr_gps/count_gps;
            avg_snr_gln = snr_gln/count_gln;

            //the GPS snr in past 30 secs
            for(int i=0; i<29; i++) {
                GPSSnrValue[i] = GPSSnrValue[i+1];
            }
            GPSSnrValue[29] = avg_snr_gps;

            //detection round count
            round_count++;
            //the first 30-sec round ends
            if(round_count == 30){
                firstRound = false;
            }

            if(!firstRound){
                for(int i=0; i<20; i++)
                    GPSSnrTrend[i] = GPSSnrValue[i] - GPSSnrValue[i+9];

                //snr variation in past 30 secs
                GPSSnrTrendMax = GPSSnrTrend[0];
                for (int i = 1; i < GPSSnrTrend.length; i++) {
                    if (GPSSnrTrend[i] > GPSSnrTrendMax) {
                        GPSSnrTrendMax = GPSSnrTrend[i];
                    }
                }
            }

            String servingCellTower = null;
            String otherVisibleCelltowers = null;

            StringBuilder stringBuilder = new StringBuilder("**");
            List<CellInfo> cellInfo = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
            if(!cellInfo.isEmpty()) {
                for (int i = 0; i < cellInfo.size(); i++) {
                    if (cellInfo.get(i).isRegistered()) { //current cell tower is the serving cell tower

                        if (cellInfo.get(i) instanceof CellInfoWcdma) {
                            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo.get(i);
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                            currentCellID = cellInfoWcdma.getCellIdentity().getCid();
                            currentCellularSNR = cellSignalStrengthWcdma.getDbm();
                            servingCellTower = "WCDMA cell " + cellInfoWcdma.getCellIdentity().getCid() + "," + cellSignalStrengthWcdma.getDbm();
                        } else if (cellInfo.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfo.get(i);
                            CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                            currentCellID = cellInfogsm.getCellIdentity().getCid();
                            currentCellularSNR = cellSignalStrengthGsm.getDbm();
                            servingCellTower = "GSM cell " + cellInfogsm.getCellIdentity().getCid() + "," + cellSignalStrengthGsm.getDbm();
                        } else if (cellInfo.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo.get(i);
                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                            currentCellID = cellInfoLte.getCellIdentity().getCi();
                            currentCellularSNR = cellSignalStrengthLte.getDbm();
                            servingCellTower = "LTE cell " + cellInfoLte.getCellIdentity().getCi() + "," + cellSignalStrengthLte.getDbm();
                        } else if (cellInfo.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo.get(i);
                            CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                            currentCellID = cellInfoCdma.getCellIdentity().getBasestationId();
                            currentCellularSNR = cellSignalStrengthCdma.getDbm();
                            servingCellTower = "CDMA cell " + cellInfoCdma.getCellIdentity().getBasestationId() + "," + cellSignalStrengthCdma.getDbm();
                        }

                        if(currentCellID == previousCellID){
                            for(int j=0;j<9;j++) {
                                cellularSNR[j] = cellularSNR[j + 1];
                            }
                            cellularSNR[9] = currentCellularSNR;
                            previousCellID = currentCellID;
                        }else{
                            for(int k=0;k<9;k++){
                                cellularSNR[k] = 0f;
                            }
                            cellularSNR[9] = currentCellularSNR;
                            previousCellID = currentCellID;
                        }
                    }
//                    else{      //other visible but not connected cell towers
//                        if (cellInfo.get(i) instanceof CellInfoWcdma) {
//                            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo.get(i);
//                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
//                            stringBuilder.append(cellInfoWcdma.getCellIdentity().getCid() + ":" + cellSignalStrengthWcdma.getDbm() + "dBm; ");
//                        } else if (cellInfo.get(i) instanceof CellInfoGsm) {
//                            CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfo.get(i);
//                            CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
//                            stringBuilder.append(cellInfogsm.getCellIdentity().getCid() + ":" + cellSignalStrengthGsm.getDbm() + "dBm; ");
//                        } else if (cellInfo.get(i) instanceof CellInfoLte) {
//                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfo.get(i);
//                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
//                            stringBuilder.append(cellInfoLte.getCellIdentity().getCi() + ":" + cellSignalStrengthLte.getDbm() + "dBm; ");
//                        } else if (cellInfo.get(i) instanceof CellInfoCdma) {
//                            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo.get(i);
//                            CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
//                            stringBuilder.append(cellInfoCdma.getCellIdentity().getBasestationId() + ":" + cellSignalStrengthCdma.getDbm() + "dBm; ");
//                        }
//                    }
                }
            }
            otherVisibleCelltowers = stringBuilder.toString();
            
            IODetection(count_gps, avg_snr_gps);
        }
    };

    public void IODetection(int count, float snr) {
        TextView statusNow = findViewById(R.id.showResult);

        getConfidenceLevelFromSatellites(count, snr);
        getConfidenceLevelFromCellular();
        getConfidenceLevelFromMagneticField();

        if((outdoorConfidence > indoorConfidence) && (outdoorConfidence > semioutdoorConfidence)){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor("#2cb457")); //outdoor
            statusNow.setText("Detection result: outdoor");
        }else if((semioutdoorConfidence > indoorConfidence) &&(semioutdoorConfidence > outdoorConfidence)){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor("#ffce26")); // semi
            statusNow.setText("Detection result: semi-outdoor");
        }else if((indoorConfidence > outdoorConfidence)&&(indoorConfidence > semioutdoorConfidence)) {
            getWindow().getDecorView().setBackgroundColor(Color.parseColor("#ff6714")); //indoor
            statusNow.setText("Detection result: indoor");
        }else if(indoorConfidence == outdoorConfidence && indoorConfidence == semioutdoorConfidence){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor("#00c3e3")); //unknown
            statusNow.setText("Detection result: unknown");
        }

        indoorConfidence = 0f;
        semioutdoorConfidence = 0f;
        outdoorConfidence = 0f;
    }

    private void getConfidenceLevelFromSatellites(int count, float snr){
        if(ProximityValue > 3){
            if(LightValue > 3000 && count > GPSCountMiddleThreshold){
                outdoorConfidence = outdoorConfidence + 10;
            }
            else {
                if (snr > GPSSnrUpperThreshold) {
                    if (count > GPSCountMiddleThreshold) {
                        outdoorConfidence = outdoorConfidence + 9;
                    } else if(count > GPSCountLowerThreshold){
                        semioutdoorConfidence = semioutdoorConfidence + 8;
                    } else {
                        indoorConfidence = indoorConfidence + 9;
                    }
                } else {
                    if (snr > GPSSnrMiddleThreshold) {
                        if (count > GPSCountMiddleThreshold) {
                            if (GPSSnrTrendMax > 6.5) {
                                indoorConfidence = indoorConfidence + 9;
                            } else {
                                semioutdoorConfidence = semioutdoorConfidence + 8;
                            }
                        } else {
                            indoorConfidence = indoorConfidence + 8;
                        }
                    } else {
                        if (count < GPSCountUpperThreshold || snr < GPSSnrLowerThreshold) {
                            indoorConfidence = indoorConfidence + 10;
                        } else {
                            if (cal.get(Calendar.HOUR_OF_DAY) > 9 && cal.get(Calendar.HOUR_OF_DAY) < 17) { //daytime
                                if (LightValue < 1500) {
                                    indoorConfidence = indoorConfidence + 9;
                                }
                            } else if (GPSSnrTrendMax > 6.5) {
                                indoorConfidence = indoorConfidence + 7;
                            }
                        }
                    }
                }
            }
        }
        else{
            if(count > 4.5 && snr > (GPSCountUpperThreshold -2)){
                outdoorConfidence = outdoorConfidence + 9;
            }
            if(count > 4.5 && GPSSnrTrendMax>6.5 && (snr < GPSCountMiddleThreshold-2)){
                indoorConfidence = indoorConfidence + 7;
            }
            if(count > 2.5 && (snr > GPSSnrMiddleThreshold-2 ) && snr < (GPSCountUpperThreshold-2)){
                semioutdoorConfidence = semioutdoorConfidence + 7;
            }
            if(count <2.5 && (snr < GPSCountLowerThreshold-2)){
                indoorConfidence = indoorConfidence + 9;
            }
            if(count <0.5){
                indoorConfidence = indoorConfidence + 10;
            }
        }
    }

    private void getConfidenceLevelFromCellular(){
        boolean cellularConsistent = true;
        float cellularVariation = 0f;

        if(cellularSNR[0] == 0f){
            cellularConsistent = false;
        }else{
            cellularVariation = cellularSNR[9] - cellularSNR[0];

            if(cellularVariation > 10) {
                outdoorConfidence = outdoorConfidence + 6;
            }
            else if(cellularVariation < -10){
                indoorConfidence = indoorConfidence + 6;
            }
        }
    }

    private void getConfidenceLevelFromMagneticField(){
        boolean magnetismAvailable = true;
        for(int i=0; i<9;i++){
            magnetismStrength[i] = magnetismStrength[i+1];
        }
        magnetismStrength[9] = MagnetismValue;
        if(magnetismStrength[0] == 0f){
            magnetismAvailable = false;
        }else{
        magnetismVariation = varianceImperative(magnetismStrength);
        if(magnetismVariation > 150)
            indoorConfidence = indoorConfidence + 3;
        }
    }

    public static float varianceImperative(float[] signal) {
        double average = 0.0;
        for (double p : signal) {
            average += p;
        }
        average /= signal.length;

        double variance = 0.0;
        for (double p : signal) {
            variance += (p - average) * (p - average);
        }
        return (float) variance / signal.length;
    }

    public void searchGPSButtonClick(View v) {
        if(clickOnce) {
            clickOnce = false;
            TextView startButton = findViewById(R.id.getStarted);
            startButton.setText("Click to stop");
            TextView detectionResult = findViewById(R.id.showResult);
            detectionResult.setText("Detection result");
            TextView status = findViewById(R.id.status);
            status.setText("Updating every 1 sec");
            getWindow().getDecorView().setBackgroundColor(Color.parseColor("#ffffff"));
            initLocation();
        }else{
            clickOnce = true;

            finish(); //restart current activity
            startActivity(getIntent());
            TextView startButton = findViewById(R.id.getStarted);
            startButton.setText("Click to start");
            TextView detectionResult = findViewById(R.id.showResult);
            detectionResult.setText("Detection result");
            TextView status = findViewById(R.id.status);
            status.setText("Updating frequency");
        }
    }

    public void indoorMarker(View view) {
        String inLabel = "indoor";
        System.out.println("indoor marker was clicked.");
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#ff6714"));
    }

    public void outdoorMarker(View view) {
        String inLabel = "outdoor";
        System.out.println("outdoor marker was clicked.");
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#2cb457"));
    }

    public void semiMarker(View view) {
        String inLabel = "semi";
        System.out.println("semi-outdoor marker was clicked.");
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#ffce26"));
    }

    public void unknownMarker(View view) {
        String inLabel = "unknown";
        System.out.println("unknown marker was clicked.");
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#00c3e3"));
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.i("location","latitude and longitude："+location.getLatitude()+"，"+location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };
}














































