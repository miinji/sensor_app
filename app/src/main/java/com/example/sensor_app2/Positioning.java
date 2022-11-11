package com.example.sensor_app2;

import android.app.Activity;
import android.util.Log;
import java.util.Arrays;
import java.util.Timer;

import Jama.Matrix;

public class Positioning {
    Activity mActivity;
    boolean flag_running = false;
    // 마지막 실행
    String current_state = "";
    //디버깅 관련
    String TAG = "Positioning";

    private String[] data = new String[5];
    WifiAPManager wifiAPManager = new WifiAPManager(MyApplication.ApplicationContext());

    Positioning(Activity activity) {
        mActivity = activity;
    }

    public String get_latest_state() {
        return current_state;
    }

    public void start(String measure) {
        flag_running = true;
        String[] lines = measure.split("\n");
        Log.d(TAG, "lines" + Arrays.toString(lines));
        data = selectAPs(lines);

        double [] firstValue = firstValue(data); // [x, y]로 초기화
        Log.d(TAG, Arrays.toString(firstValue));
//        칼만필터 5초마다 반복 실행
//        Timer timer = new Timer();
//        if (flag_running){
//            timer.schedule(extendedKalmanFilter(), 0, 5000);
//        }
    }

    //APinfo 파일에서 추출한 신호의 주소, dist(계산값), X, Y 좌표 값 구함
    private String[] selectAPs(String[] lines) {

        String[] data = new String[5];
        for (int i = 0; i < 5; i++) {
            String addr = lines[i].split(",")[0];
            for (int j = 0; j < wifiAPManager.apInfoList.size(); j++) {
                if (addr.equals(wifiAPManager.apInfoList.get(j).mac_addr1)) {
                    data[i] = lines[i] + "," + wifiAPManager.apInfoList.get(j).ref_x + wifiAPManager.apInfoList.get(j).x + "," + wifiAPManager.apInfoList.get(j).ref_y + wifiAPManager.apInfoList.get(j).y;
                } else if (addr.equals(wifiAPManager.apInfoList.get(j).mac_addr2)) {
                    data[i] = lines[i] + "," + wifiAPManager.apInfoList.get(j).ref_x + wifiAPManager.apInfoList.get(j).x + "," + wifiAPManager.apInfoList.get(j).ref_y + wifiAPManager.apInfoList.get(j).y;
                }
            }
        }
        return data;
    }

    private double [] firstValue(String[] data){
        double[] x = new double[5];
        double[] y = new double[5];
        double[] distVal = new double[5];// 앱 실행 후 구한 단말기와 와이파이 간의 distance 값들로 초기화
        String[] items = new String[4];

        double n1 = 0;
        double n2 = 0;

        double [] firstValue = new double[2];

        for (int i = 0; i < 5; i++) {
            items = data[i].split(",");
            Log.d("items", items.toString());

            x[i] = Double.parseDouble(items[2]);
            y[i] = Double.parseDouble(items[3]);
            distVal[i] = Double.parseDouble(items[1]);
        }
        for(int i = 0; i < 5; i ++){
            n1 += x[i];
            n2 += y[i];
        }
        firstValue[0] = n1 / 5;
        firstValue[1] = n2 / 5;
        return firstValue;
    }

//    private Matrix linearRslt(String[] data) {
//        //선형위치추정 방식으로 단말의 위치추정.
//        // x1~x5, y1~y5 값들은 상위5개 신호들의 Refx, Refy에서 가져와 초기화해야 함.
//        double[] Refx = new double[5];
//        double[] Refy = new double[5];
//        double[] distVal = new double[5];// 앱 실행 후 구한 단말기와 와이파이 간의 distance 값들로 초기화
//        String[] items = new String[4];
//        for (int i = 0; i < 5; i++) {
//            items = data[i].split(",");
//            Log.d("items", items.toString());
//
//            Refx[i] = Double.parseDouble(items[2]);
//            Refy[i] = Double.parseDouble(items[3]);
//            distVal[i] = Double.parseDouble(items[1]);
//        }
//        double[][] array = {
//                {2 * (Refx[1] - Refx[0]), 2 * (Refy[1] - Refy[0])},
//                {2 * (Refx[2] - Refx[1]), 2 * (Refy[2] - Refy[1])},
//                {2 * (Refx[3] - Refx[2]), 2 * (Refy[3] - Refy[2])},
//                {2 * (Refx[4] - Refx[3]), 2 * (Refy[4] - Refy[3])},
//        };
//
//        double[][] d = {
//                {Math.pow((distVal[1]), 2) - Math.pow((distVal[0]), 2) + Math.pow((Refx[0]), 2) - Math.pow((Refx[1]), 2) + Math.pow(Refy[0], 2) - Math.pow(Refy[1], 2)},
//                {Math.pow((distVal[2]), 2) - Math.pow((distVal[0]), 2) + Math.pow(Refx[0], 2) - Math.pow(Refx[2], 2) + Math.pow(Refy[0], 2) - Math.pow(Refy[2], 2)},
//                {Math.pow((distVal[3]), 2) - Math.pow((distVal[0]), 2) + Math.pow(Refx[0], 2) - Math.pow(Refx[3], 2) + Math.pow(Refy[0], 2) - Math.pow(Refy[3], 2)},
//                {Math.pow((distVal[4]), 2) - Math.pow((distVal[0]), 2) + Math.pow(Refx[0], 2) - Math.pow(Refx[4], 2) + Math.pow(Refy[0], 2) - Math.pow(Refy[4], 2)}
//        };
//        Matrix A = new Matrix(array);
//        Matrix b = new Matrix(d);
//        Matrix z = A.solve(b);
//        return z;
//    }
}