package com.example.sensor_app2;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.SystemClock.elapsedRealtime;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sensor_app2.FileModule;
import com.example.sensor_app2.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView tv;
    FloatingActionButton fab, fab2;
    boolean flag_running = false;
    public static Context mContext;


    // File 쓸 수 있도록 fileModule 생성
    FileModule fileModule;
    private long begin_time; // start 할 때 시간을 저장할 변수

    //Modules
    ImageModule imageModule;
    SensorModule sensorModule;
    WifiModule wifiModule;
    WifiAPManager wifiAPManager;
    Positioning positioning;


    // permission-related
    private boolean is_permission_granted = false; // permission을 요구하는 상태로 초기화.

    // Threads
    Looper display_update_looper;
    int count = 0;

    public float X;
    public float Y;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        // permission-request
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, 1);


        tv = (TextView) findViewById(R.id.tv);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        imageModule = new ImageModule(this);
        sensorModule = new SensorModule(this); //인스턴스 생성
        wifiModule = new WifiModule(getApplicationContext());
        wifiAPManager = new WifiAPManager(getApplicationContext());
        positioning = new Positioning(getApplicationContext());


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag_running)
                    stop();
                else
                    start();
            }
        });

        //btn2.setOnClickListener(new View.OnClickListener(){
        //    @Override
        //    public void onClick(View view){
        //        wifiManager.startScan();
        //    }
        //});

        HandlerThread handlerThread = new HandlerThread("DISPLAY_UPDATE_THREAD", Process.THREAD_PRIORITY_DISPLAY);
        handlerThread.start();

        display_update_looper = handlerThread.getLooper();


        Handler handler = new Handler(); // 핸들러와 루퍼 정보를 연결했다.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                display_update_thread();
            }
        }, 0); //delay 없이 실행
        thread_test();
    }

    private void display_update_thread(){
        count += 1;
        tv.setText(count + "");


        if(flag_running){
            float deg = sensorModule.get_heading();
            imageModule.plot_arrow(X,Y, deg);
            Log.d("image", X + ", " + Y);

            String str = "";
            str += "[WIFI]\n" + wifiModule.get_latest_state() + "\n";
            str += "[Sensor]\n" + sensorModule.get_latest_state();
            tv.setText(str);
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                display_update_thread();
            }
        }, 100); //100ms 후에 다시 display_update_thread 실행
    }

    private void thread_test(){
        Thread thread = Thread.currentThread();
        Log.d("THREAD_TEST", thread.getName() + ", " + thread.getId()); //로그 메세지에 출력
        for (int i = 0; i<0 ; i++){
            Log.d("THREAD_TEST", thread.getName() + ":" + i );
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void stop() {
        flag_running = false;

        sensorModule.stop();
        wifiModule.stop();

        fab.setImageTintList(ColorStateList.valueOf(Color.rgb(0, 0, 0)));

    }

    private void start() {
        if (!is_permission_granted) {
            Toast.makeText(getApplicationContext(), "Permission is not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        begin_time = elapsedRealtime();

        FileModule file1 = new FileModule(this, "test", true, true, ".txt");
        sensorModule.start(begin_time, file1);

        FileModule file2 = new FileModule(this, "WIFI_sensor.txt");
        wifiModule.start(file2);

        flag_running = true;
        fab.setImageTintList(ColorStateList.valueOf(Color.rgb(57, 155, 226)));
    }

//사용자의 위치등 동의여부 구할때 사용할 코드
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult){
        for (int i=0; i<grantResult.length; i++)
            if (grantResult[i] != PERMISSION_GRANTED) {// PERMISSION_GRANTED 대신에 0으로 해도 됨!
                Toast.makeText(getApplicationContext(), "Warning: " + permissions[i] + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        is_permission_granted = true;
    }
    public void getX(double num){ X = (float) ((float)num * 11); }
    public void getY(double num){
        Y = (float) ((float)num * 11);
    }
}