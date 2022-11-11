package com.example.sensor_app2;

import static android.content.Context.WIFI_SERVICE;
import static android.os.SystemClock.elapsedRealtime;

import android.app.Activity;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WifiModule {
    private FileModule file;
    private WifiAPManager wifiAPManager;
    public Positioning positioning;
    public String scan_final;
    private Activity mActivity;

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
        WifiAPManager wifiAPManager = new WifiAPManager(MyApplication.ApplicationContext());
        Positioning positioning = new Positioning(mActivity);
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Scan results received");
            // level dbm scale로 변환된 신호의 세기를 알려줌
            List<ScanResult> scanResults = wifiManager.getScanResults(); //하나의 ap에서 온 신호를 리스트로
            List <ScanResult> buffer = new ArrayList<>();
            float P0 = -30;
            float eta = 2;
            float dist;
            scan_final =""; //초기화

            String str = "";
            str += "Found " + scanResults.size() + " APs\n";

            List<ScanResult> scanResults2 = checkAPs(wifiAPManager, scanResults); //scanResults중 APinfo에 저장된 값만 추출하여 배열
            int [] signal_index = selectAPs(scanResults2);
            for(int k=0; k<signal_index.length; k++){
                buffer.add(scanResults2.get(signal_index[k]));
            }
            // 신호의 세기 순서대로 나열된 인덱스 저장
            if (buffer.size() >= 5){
                List<ScanResult> final_results = filterResult(buffer);
                //신호 세기 상위 5개의 정보 출력
                str += "APinfo.txt 안에 상위 5개의 wifi 신호 정보\n";
                for (int k = 0; k<5; k ++){
                    str += final_results.get(k).SSID + ", "; // service set id
                    str += final_results.get(k).BSSID + ", "; //wifi ap의 주소
                    str += final_results.get(k).frequency + "MHz , ";
                    str += final_results.get(k).level + "dBm";
                    float curr_p = final_results.get(k).level;
                    dist =(float)Math.pow(10, (P0 - curr_p) / eta);
                    str += String.format(", distance: %.2fm\n", dist);
                    scan_final += final_results.get(k).BSSID+"," + String.format("%.2f\n", dist);
                }
                file.save_str_to_file(str);
                Log.d(TAG, "[UPDATE]WIFI_sensor_txt");

                file.save_str_to_file("\nscan_final\n" + scan_final);
                positioning.start(scan_final);
                current_state = "Scan counter: " + scan_counter + ", # APs: " + scanResults.size();
                // 나중에 구현
            }
            else{
                //신호 세기 상위 5개의 정보 출력
                int [] signal_index2 = selectAPs(scanResults);
                str += "상위 5개의 wifi 신호 정보\n";
                for (int k = 0; k<5; k ++){
                    str += scanResults.get(signal_index2[k]).SSID + ", "; // service set id
                    str += scanResults.get(signal_index2[k]).BSSID + ", "; //wifi ap의 주소
                    str += scanResults.get(signal_index2[k]).frequency + "MHz , ";
                    str += scanResults.get(signal_index2[k]).level + "dBm";

                    float curr_p = scanResults.get(signal_index2[k]).level;
                    dist =(float)Math.pow(10, (P0 - curr_p) / eta);
                    str += String.format(", distance: %.2fm\n", dist);
                    scan_final += scanResults.get(signal_index2[k]).BSSID+"," + String.format("%.2f\n", dist);
                }
                file.save_str_to_file(str);
                Log.d(TAG, "[UPDATE]WIFI_sensor_txt");
                file.save_str_to_file("\nscan_final\n" + scan_final);
                Toast.makeText(MyApplication.ApplicationContext(), "[ERROR]Move into the KNU_library!", Toast.LENGTH_SHORT).show();
                current_state = "Scan counter: " + scan_counter + ", # APs: " + scanResults.size();
                // 나중에 구현
            }
        }

        //신호 세기 중 상위 3개 요소의 scanResult 인덱스 리턴
        private int[] selectAPs(List<ScanResult> scanResults){
            Map<Integer, Integer> signalMap = new HashMap<>();
            int [] signal_index = new int[scanResults.size()];
            int [] signal_indexs = new int[scanResults.size()];

            //신호의 level로 구성된 배열 생성
            //딕셔너리 생성 -> levels 오름차순 배열
            for (int i = 0; i < scanResults.size(); i ++){
                signalMap.put(i, scanResults.get(i).level);
                //signal_levels[i] = scanResults.get(i).level;
            }
            Log.d("signalMap", signalMap.toString());

            List<Map.Entry<Integer, Integer>> entryList = new LinkedList<>(signalMap.entrySet());
            entryList.sort(Map.Entry.comparingByValue());

            int n = 0;
            for(Map.Entry<Integer, Integer> entry : entryList){
                signal_indexs[n] = entry.getKey().intValue();
                n++;
            }

            for(int i = 0; i<scanResults.size() ; i++ ){
                signal_index[i] = signal_indexs[scanResults.size()-i-1];
            }
            Log.d("signalMap2", Arrays.toString(signal_index) + "Update");
            return signal_index;
        }
        //받은 신호 중 Apinfo에 있는 신호로만 구성된 scanResult 반환
        private List<ScanResult> checkAPs(WifiAPManager wifiAPManager,List<ScanResult> scanResults) {
            List <Integer> signal_index = new ArrayList<>();
            List <ScanResult> scanResults2 = new ArrayList<>();
            int count = 0;
            for(int i = 0; i < scanResults.size(); i++){
                for( int j = 0; j< wifiAPManager.apInfoList.size(); j ++){
                    if (scanResults.get(i).BSSID.equals(wifiAPManager.apInfoList.get(j).mac_addr1)){
                        i = new Integer(i);
                        signal_index.add(i);
                        count ++;
                    }
                    else if (scanResults.get(i).BSSID.equals(wifiAPManager.apInfoList.get(j).mac_addr2)){
                        i = new Integer(i);
                        signal_index.add(i);
                        count ++;
                    }
                }
            }
            Log.d("checkAPs", String.valueOf(count));

            for (int i = 0; i < signal_index.size(); i ++){
                scanResults2.add(scanResults.get(signal_index.get(i).intValue()));
            }
            return scanResults2;
        }
        private List<ScanResult> filterResult(List <ScanResult> scanResults){
            int [] check = new int[wifiAPManager.apInfoList.size()];
            List<ScanResult> filterresults = new ArrayList<>();
            Arrays.fill(check, 0);
            for(int i =0; i < scanResults.size(); i ++){
                for(int j =0; j < wifiAPManager.apInfoList.size(); j++){
                    if(scanResults.get(i).BSSID.equals(wifiAPManager.apInfoList.get(j).mac_addr1)){
                        if(check[j] != 1){
                            filterresults.add(scanResults.get(i));
                            check[j] = 1;
                        }
                    }
                    else if(scanResults.get(i).BSSID.equals(wifiAPManager.apInfoList.get(j).mac_addr2)){
                        if(check[j] != 1){
                            filterresults.add(scanResults.get(i));
                            check[j] = 1;
                        }
                    }
                }
            }
            return filterresults;
        }
    }
}
