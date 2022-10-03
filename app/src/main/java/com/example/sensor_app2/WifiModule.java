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

import java.util.Arrays;
import java.util.List;

public class WifiModule {
    private FileModule file;

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

    public void start(FileModule file_in){
        //wifi 측정 시작
        flag_running = true;
        scan_counter = 0;
        file = file_in;
        file.remove_file();
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
            str += "Found " + scanResults.size() + " APs\n";

            int [] signal_index = selectAPs(scanResults); // 상위 3개 신호의 인덱스 저장

            // 신호 전체 출력
            for (int k = 0; k<scanResults.size(); k ++){
                str += scanResults.get(k).SSID + ", "; // service set id
                str += scanResults.get(k).BSSID + ", "; //wifi ap의 주소
                str += scanResults.get(k).frequency + "MHz , ";
                str += scanResults.get(k).level + "dBm";

                float curr_p = scanResults.get(k).level;
                dist =(float)Math.pow(10, (P0 - curr_p) / 10 * eta);
                str += String.format(", distance: %.2fm\n", dist);
            }

            //신호 세기 상위 3개의 정보 출력
            str += "상위 3개의 wifi 신호 정보\n";
            for (int k = 0; k<3; k ++){
                str += scanResults.get(signal_index[k]).SSID + ", "; // service set id
                str += scanResults.get(signal_index[k]).BSSID + ", "; //wifi ap의 주소
                str += scanResults.get(signal_index[k]).frequency + "MHz , ";
                str += scanResults.get(signal_index[k]).level + "dBm";

                float curr_p = scanResults.get(signal_index[k]).level;
                dist =(float)Math.pow(10, (P0 - curr_p) / 10 * eta);
                str += String.format(", distance: %.2fm\n", dist);
            }
            file.save_str_to_file(str);
            Log.d(TAG, "[UPDATE]WIFI_sensor_txt");

            current_state = "Scan counter: " + scan_counter + ", # APs: " + scanResults.size();
            // 나중에 구현
        }

        //신호 세기 중 상위 3개 요소의 scanResult 인덱스 리턴
        private int[] selectAPs(List<ScanResult> scanResults){
            int [] signal_levels = new int [scanResults.size()];
            int [] signal_index = new int[3];

            //신호의 level로 구성된 배열 생성
            for (int i = 0; i < scanResults.size(); i ++){
                signal_levels[i] = scanResults.get(i).level;
            }

            //신호의 세기 내림차순 정렬(오름차순 정렬 후 -> 뒤집음)
            Arrays.sort(signal_levels);

            for(int i = 0; i < signal_levels.length/2; i++) {
                int tmp = signal_levels[i];
                signal_levels[i] = signal_levels[signal_levels.length-1-i];
                signal_levels[signal_levels.length-i-1] = tmp;
            }
            //내림차순 정렬된 신호의 세기 배열 출력
            Log.d(TAG, "[signal_levels]" + Arrays.toString(signal_levels));

            // 상위 3개의 신호 인덱스 추출
            for (int i = 0; i < 3 ; i ++){
                for(int k = 0; k < scanResults.size(); k ++){
                    if (signal_levels[i] == scanResults.get(k).level){
                        signal_index[i] = k;
                    }
                }
            }
            return signal_index;
        }
    }
}
