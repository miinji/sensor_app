package com.example.sensor_app2;

import static android.content.Context.WIFI_SERVICE;
import static android.os.SystemClock.elapsedRealtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.util.List;

public class WifiModule {

    WifiManager wifiManager;
    WifiReceiver wifiReceiver;

    //wifi 스캔 관련 변수들
    boolean flag_running = false;
    int scan_counter = 0; //몇번째 스캔을 진행하였는지
    long last_scan_time_ms = elapsedRealtime();
    final int scan_interval_ms = 5000; //5초 마다 스캔

    // 스레드 관련
    Looper wifi_scan_looper;

    // 마지막 실행
    String current_state = "";

    //디버깅 관련
    String TAG = "WIFI_MODULE";

    WifiModule(Context context){
        // wifi 리시버를 필터와 함께 등록함
        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE); //wifi서비스를 와이파이 매니저로 가져오겠다
        wifiReceiver = new WifiReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION); //여러 정보 중 wifi 새로운 결과가 도착했을 때 addaction
        context.registerReceiver(wifiReceiver, intentFilter); //wifi 리시버를 인텐트 필터에 등록

        // 스레드 생성하고 시작
        HandlerThread handlerThread = new HandlerThread( "WIFI_THREAD", Process.THREAD_PRIORITY_DISPLAY);
        handlerThread.start();
        wifi_scan_looper = handlerThread.getLooper();
    }

    public String get_latest_state(){
        return current_state;
    }

    public void start(){
        //wifi 측정 시작
        flag_running = true;
        scan_counter = 0;

        invoke_wifi_scan_thread(); // 처음은 UI thread에서 실행 나중에는 새로운 스레드에서 무한 반복
    }

    private void invoke_wifi_scan_thread(){
        if(!flag_running) // flag_running이 false면 종료
            return;

        // 매 5초 마다 스캔실행 (5초 마다 invoke_wifi_scan_thread 실행)
        wifiManager.startScan();
        scan_counter += 1;
        last_scan_time_ms = elapsedRealtime();

        Handler handler = new Handler(wifi_scan_looper);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invoke_wifi_scan_thread();
            }
        }, scan_interval_ms);
    }
    public void stop(){
        //wifi 측정 종료
        flag_running = false;
    }

    class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Scan results received");
            // level dbm scale로 변환된 신호의 세기를 알려줌
            List<ScanResult> scanResults = wifiManager.getScanResults(); //하나의 ap에서 온 신호를 리스트로

            float P0 = -30;
            float eta = 2;
            float dist;

            String str = "";
            str += "Found " + scanResults.size() + " APs ";
            for (int k = 0; k<scanResults.size(); k ++){
                str += scanResults.get(k).SSID + ", "; // service set id
                str += scanResults.get(k).BSSID + ", "; //wifi ap의 주소
                str += scanResults.get(k).frequency + "MHz , ";
                str += scanResults.get(k).level + "dBm\n";

                float curr_p = scanResults.get(k).level;
                dist =(float) Math.pow(10, (P0 - curr_p) / 10 * eta);
                str += String.format(", distance: %.2f m\n", dist);
            }

            current_state = "Scan counter: " + scan_counter + ", # APs: " + scanResults.size();
            // 나중에 구현
        }
    }
}
