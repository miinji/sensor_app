package com.example.sensor_app2;

import android.app.Activity;
import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import Jama.Matrix;


public class Positioning {
    public double past_x;
    public double past_y;
    private Activity mActivity;
    boolean flag_running = false;
    // 마지막 실행
    String current_state = "";
    //디버깅 관련
    String TAG = "Positioning";
    String scanData; //wifiModule 에서 받아온 scan data 저장
    String preData;
    private String[] data = new String[5];
    WifiAPManager wifiAPManager = new WifiAPManager(MyApplication.ApplicationContext());
    Positioning(Context context) {
    }

    public String get_latest_state() {
        return current_state;
    }

    public void start(String measure) {
        flag_running = true;
        String[] lines = measure.split("\n");
        Log.d(TAG, "lines" + Arrays.toString(lines));
        data = selectAPs(lines);

        double [] firstvalue = firstValue(data); // [x, y]로 초기화
        Log.d(TAG, Arrays.toString(firstvalue));
        ExtendedKalman extendedKalman = new ExtendedKalman(firstvalue);
//        imageModule.getKalmanX(firstvalue[0]);
//        imageModule.getKalmanX(firstvalue[1]);
        Log.d(TAG, "initialize extendedKalman");
        run(extendedKalman);
    }
    public void run(ExtendedKalman extendedKalman){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(flag_running){
                    extendedKalman.start();
                }
            }
        };
        timer.schedule(task, 6000, 5000);
    }

    //wifiModule에서 받은 scan_final을 scanData로 저장
    public String getScan_final(String measure){
        scanData = measure;
        return scanData;
    }
    //받아온 data중 APinfo에 저장된
    //String [] 형태로 저장된 data의 addr와 APinfo.txt addr 비교 후 추출
    public String[] selectAPs(String[] lines) {
        String[] data = new String[5];
        for (int i = 0; i < 5; i++) {
            String addr = lines[i].split(",")[0];
            for (int j = 0; j < wifiAPManager.apInfoList.size(); j++) {
                if (addr.equals(wifiAPManager.apInfoList.get(j).mac_addr1)) {
                    double X = wifiAPManager.apInfoList.get(j).x;
                    double Y = wifiAPManager.apInfoList.get(j).y;
                    data[i] = lines[i] + "," + X +"," + Y;
                } else if (addr.equals(wifiAPManager.apInfoList.get(j).mac_addr2)) {
                    double X = wifiAPManager.apInfoList.get(j).x;
                    double Y = wifiAPManager.apInfoList.get(j).y;
                    data[i] = lines[i] + "," + X +"," + Y;
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
    public class ExtendedKalman {
        public Matrix past_P;
        // 위치 추정 기법 중에 칼만 필터를 이용하여 x-y좌표를 찾는 것을 구현
        // 강의 7 참고
        // 경로손실모델로 얻은 길이는 wifimodule에서 받아오기 -> -------------- 와이파이 신호를 받아와야함
        //초기 공분산 행렬
        ExtendedKalman(double [] initValue){
            past_x = initValue[0]; //이전 값 저장
            past_y = initValue[1];
            double sigma = 1;
            double [][] vals = {{sigma, 0},{0, sigma}}; // 초기 공분산 행렬
            past_P = new Matrix(vals);
        }
        public void start(){
            if(scanData != preData){
                update_state(past_x, past_y, past_P, scanData);
                Log.d(TAG, "[update] Kalman");
                preData = scanData;
            }
        }


        //공분산 행렬
//        public Matrix Make_cov_Matrix(double [] x_val, double [] y_val){
//            double tmp_x = 0;
//            double tmp_y = 0;
//            for(int i = 0; i < 5 ; i++){ // 이전의 x,y값 평균내기
//                tmp_x += X[i];
//                tmp_y += Y[i];
//            }
//            double avg_x = tmp_x/5;
//            double avg_y = tmp_y/5;
//
//            tmp_x = 0;
//            tmp_y = 0;
//            for(int i = 0 ; i < 5 ; i++){
//                tmp_x += Math.pow(Refx[i] - avg_x,2);
//                tmp_y += Math.pow(Refy[i] - avg_y,2);
//            }
//            double [][] vals = {{tmp_x/5, 0},{0, tmp_y/5}}; // 공분산 만드는 행렬
//            Matrix cov_Matrix = new Matrix(vals);
//            return cov_Matrix;
//        }

        //야코비안 행렬 / input - 상위 5개 AP 신호 좌표, 추정한 좌표
        public Matrix Make_Jacobian(double [] X, double [] Y, double x, double y) {
            double [][] jacobian = new double[5][2];
            for(int i = 0;i<5;i++){
                double dis = Math.pow((Math.pow(x - X[i],2) + Math.pow(y - Y[i],2)),0.5);
                jacobian[i][0] = (x-X[i]) / dis;
                jacobian[i][1] = (y-Y[i]) / dis;
            }
            Matrix jacobian_Matrix = new Matrix(jacobian);
            return jacobian_Matrix;
        }

        //------------------------------상태업데이트-----------------------------------//

        //z는 이전 상태와 같다고 가정, P는 시그마tr을 더하는 걸로
        //이전의 x,y값, 상위 AP신호의 x,y값, 경로손실모델로 얻은 거리,
        public void update_state(double preX, double preY, Matrix preP, String measuredata){
            //double [] error_dist = new double[5];
            // double [] func_z = new double[5];
            String[] lines = measuredata.split("\n");
            data = selectAPs(lines);

            //sigma_tr 1 ~ 2 정도의 값
            double sigma_tr = 1.5;
            double [][] vals_tr = {{sigma_tr,0},{0,sigma_tr}};
            Matrix sigma_tr_Matrix = new Matrix(vals_tr);

            //Wk의 diag행렬 - wk의 공분산 행렬
            double sn_k = 1.5; //1 ~ 2의 값으로 진행
            double [][] val_diag ={{sn_k,0,0,0,0},{0,sn_k,0,0,0},{0,0,sn_k,0,0},{0,0,0,sn_k,0},{0,0,0,0,sn_k}};

            //identity matrix
            double [][] vals_iden = {{1,0},{0,1}};
            Matrix identity_matrix = new Matrix(vals_iden);
            //wifiModule에서 받아와야 할 정보***********************************************
            double [] X = new double[5];
            double [] Y = new double[5];
            double [] dist = new double[5]; // 경로손실모델로 얻은 dist   -> wifimodule에 있음
            String [] items = new String[4];
            // wifiModule 에서 받아온 신호들 각 double에 넣기
            for(int i =0 ; i < 5 ; i++){
                items = data[i].split(",");
                Log.d("items", items.toString());

                X[i] = Double.parseDouble(items[2]);
                Y[i] = Double.parseDouble(items[3]);
                dist[i] = Double.parseDouble(items[1]);
            }
            double [] func_h = new double[5];
            double [][] func_e = new double[5][1];
            double [][] val_z = {{preX},{preY}}; // z의 상태추정 -> 센서신호있으면 추가 없으면 이전의 값과 같다고 판단단
            Matrix z = new Matrix(val_z);
            Matrix func_P = preP.plus(sigma_tr_Matrix); // P의 상태추정, constant인 sigma_tr과 더함

            for(int i = 0;i < 5;i++){
                //error_dist[i] = Math.pow(Math.pow(x - X[i],2) + Math.pow(y - Y[i],2),0.5) - dist[i];
                func_h[i] = Math.pow(Math.pow(preX - X[i],2) + Math.pow(preY - Y[i],2),0.5); // Matrix norm 만 따로 구현
                //func_h[i] = func_z[i] + error_dist[i];
            }

            //innovation 행렬로 만들기 위해 double [][]
            for(int i = 0;i<5;i++){
                func_e[i][0] = dist[i] - func_h[i];
            }
            Matrix func_e_matrix = new Matrix(func_e);

            //Jacobain 적용
            Matrix func_H = Make_Jacobian(X,Y,preX,preY);
            Matrix W_k = new Matrix(val_diag);

            //func_e의 공분산 행렬
            double [][] S_k = new double[5][5];
            Matrix func_S = new Matrix(S_k);
            func_S = func_H.times(func_P);
            func_S = func_S.times(func_H.transpose());
            func_S = func_S.plus(W_k);

            //kalman gain
            double [][] gain = new double[2][5];
            Matrix kalman_gain = new Matrix(gain);
            kalman_gain = func_P.times(func_H.transpose());
            kalman_gain = kalman_gain.times(func_S.inverse());


            //update된 상태
            double [][] update = new double[2][1];
            Matrix update_z = new Matrix(update);
            update_z = kalman_gain.times(func_e_matrix);
            update_z = update_z.plus(z);

            //update 된 공분산 행렬
            double [][] update_cov = new double[2][2];
            Matrix update_cov_matrix = new Matrix(update_cov);
            Matrix tmp_cov = kalman_gain.times(func_H);
            tmp_cov = identity_matrix.minus(tmp_cov);
            update_cov_matrix = tmp_cov.times(func_P);

            past_P = update_cov_matrix;
            past_x = update_z.get(0, 0);
            past_y = update_z.get(1, 0);
//            imageModule.getKalmanX(past_x);
//            imageModule.getKalmanX(past_y);
            Log.d(TAG, String.valueOf(past_x) + ", " + String.valueOf(past_y));
//update된 z와 P를 다시 신호 받아올 때까지 저장해두고, 이전 값으로 사용
        }
    }
    public float getX(){
        return (float)past_x;
    }
    public float getY(){
        return (float)past_y;
    }
}