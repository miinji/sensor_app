package com.example.sensor_app2;

import Jama.*;

import java.util.ArrayList;
import java.util.List;


public class ExtendedKalman {
    // 위치 추정 기법 중에 칼만 필터를 이용하여 x-y좌표를 찾는 것을 구현
    // 강의 7 참고
    // 경로손실모델로 얻은 길이는 wifimodule에서 받아오기
    private WifiModule wifimodule;
    private Positioning positioning;
    private WifiAPManager wifiAPManager;

    List<Double> curr_pos = new ArrayList<>();

    double tmp_x = 0;
    double tmp_y = 0;

    //초기 공분산 행렬
    double sigma = 1;
    double [][] vals = {{sigma, 0},{0, sigma}}; // 초기 공분산 행렬
    Matrix COV_Matrix = new Matrix(vals);

    //sigma_tr 의 행렬
    double sigma_tr = 0.5;
    double [][] vals_tr = {{sigma_tr,0},{0,sigma_tr}};
    Matrix sigma_tr_Matrix = new Matrix(vals_tr);

    //Wk의 diag행렬 - wk의 공분산 행렬
    double sn_k=1.5;
    double [][] vals_diag ={{sn_k,0,0,0,0},{0,sn_k,0,0,0},{0,0,sn_k,0,0},{0,0,0,sn_k,0},{0,0,0,0,sn_k}};
    Matrix Wk_cov_matrix = new Matrix(vals_diag);

    double [] Refx = new double[5]; //positioning 에서 받아오기 ***********************************************
    double [] Refy = new double[5];
    double [] past_x = new double[5]; //이전 값 저장
    double [] past_y = new double[5];
    double [] dist = new double[5]; // 경로손실모델로 얻은 dist
    Matrix H_k;
    Matrix Kalman_gain;

    private void Inital_val(){
        if(curr_pos.isEmpty()){
            for(int i = 0; i < 5 ; i++){
                tmp_x += Refx[i];
                tmp_y += Refy[i];
            }
            curr_pos.add(tmp_x/5); //단말 위치를 상위 5개 위치의 중심으로 초기화
            curr_pos.add(tmp_y/5);

            //공분산 배열에 초기값 넣어주기 - 위에서 시그마로 만듦
            //공분산 행렬 - 평균값 구해서 각각 원소 빼고, 나누기 원소의 개수(5)

        }else{ // curr_pos에 값이 있어서 추정 -> 업데이트 반복
            // 비용함수로 curr_pos를 최적화
            tmp_x = curr_pos.get(0);
            tmp_y = curr_pos.get(1);

            cost_func(tmp_x, tmp_y,Refx,Refy,dist); // z^(k|k-1) 최적화
            COV_matrix(Refx, Refy);

            curr_pos.set(0,tmp_x); // 바꾼 값 변경하기
            curr_pos.set(1,tmp_y);

            // 공분산 행렬 만들기
        }


   }//--------------------------예측 start------------------------------//
   private double [] cost_func(double past_x, double past_y, double [] Refx, double [] Refy, double [] dist ){ // 상태 추정을 위한 비용함수
        //편미분된 비용함수
       double [] partial_diff = new double[2];
       double sum_x = 0;
       double sum_y = 0;
       for(int i = 0; i < 5; i++){
           double tmp = Math.pow(past_x-Refx[i],2) + Math.pow(past_y-Refy[i],2);
           double partial_x = 2 * (past_x - Refx[i]) - (2 * dist[i] * (past_x - Refx[i]) / Math.pow(tmp,1/2));
           double partial_y = 2 * (past_y - Refy[i]) - (2 * dist[i] * (past_y - Refy[i]) / Math.pow(tmp,1/2));


       }

       //partial_diff[0] = partial_x;

     //  partial_diff[1] = partial_y;

       return partial_diff; // 현재 추정된 단말의 위치 -> z^(k|k-1)


   }
   private Matrix COV_matrix(double [] Refx, double [] Refy){
        double tmp_x = 0;
        double tmp_y = 0;
       for(int i = 0; i < 5 ; i++){ // 이전의 x,y값 평균내기
           tmp_x += Refx[i];
           tmp_y += Refy[i];
       }
       double avg_x = tmp_x/5;
       double avg_y = tmp_y/5;

       tmp_x = 0;
       tmp_y = 0;
       for(int i = 0 ; i < 5 ; i++){
           tmp_x += Math.pow(Refx[i] - avg_x,2);
           tmp_y += Math.pow(Refy[i] - avg_y,2);
       }
       double [][] vals = {{tmp_x/5, 0},{0, tmp_y/5}}; // 공분산 만드는 행렬
       Matrix cov_Matrix = new Matrix(vals);

       return cov_Matrix;
   }

//------------------------------상태업데이트-----------------------------------//

    //h를 야코비안 행렬로 만들어서 H 만들기
    public double [] pre_h(double [] Refx, double [] Refy, double [] dist){
        //거리추정오차, ||z-zn|| 생성 -> ||z-zn||을 거리로 생각 - Matrix norm
        double [] error_dist = new double[5];
        double [] h_z = new double[5];
        double x=0; // 일단 0으로,,,
        double y=0; //추정해야할 단말의 x,y

        for(int i = 0;i < 5;i++){
            error_dist[i] = Math.pow(Math.pow(x - Refx[i],2) + Math.pow(y - Refy[i],2),1/2) - dist[i];
            h_z[i] = Math.pow(Math.pow(x - Refx[i],2) + Math.pow(y - Refy[i],2),1/2) - error_dist[i];
        }

        return h_z;
    }
    //상위 신호 5개 받아오기, 거리추정오차 받아오기
    //어차피 야코비안 때리면 wk는 상수로 사라지지 않을까?
    public Matrix Jacobian(double [] Refx, double [] Refy) {
        //거리추정오차, ||z-zn|| 생성 -> ||z-zn||을 거리로 생각 - Matrix norm
        double x=0;
        double y=0; //추정해야할 단말의 x,y

        double [][] jacobian = new double[5][2];
        for(int i = 0;i<5;i++){
            jacobian[i][0] = 2*(x-Refx[i]) / Math.pow(Math.pow(x - Refx[i],2) + Math.pow(y - Refy[i],2),0.5);
            jacobian[i][1] = 2*(y-Refy[i]) / Math.pow(Math.pow(x - Refx[i],2) + Math.pow(y - Refy[i],2),0.5);
            }
        Matrix jacobian_Matrix = new Matrix(jacobian);
        return jacobian_Matrix;
        }

    //거리 추정 오차가 zero일때의 e(k)
    private double [] innovation(double [] dis){
        double [] e_k = new double[5];
        double [] h_z = pre_h(Refx, Refy, dist); //return h_z
        for(int i = 0;i<5;i++){
            e_k[i] = dis[i] - h_z[i];
        }
       return e_k;

    }

    //e(k)의 공분산 행렬, 5x5 행렬
    private Matrix inno_cov_matrix(){
        H_k = Jacobian(Refx, Refy);
        Matrix tmp_P = COV_matrix(Refx, Refy);
        H_k = H_k.arrayTimesEquals(tmp_P);
        H_k = H_k.arrayTimesEquals(H_k.transpose());
        H_k = H_k.plus(Wk_cov_matrix);

        return H_k;
    }
    //kalman 이득
    private Matrix kalman_gain(){
        Matrix tmp_P = COV_matrix(Refx, Refy);
        Matrix H_k = Jacobian(Refx, Refy);
        Matrix S_k = inno_cov_matrix();
        S_k = S_k.inverse();

        tmp_P = tmp_P.arrayTimesEquals(H_k.transpose());
        Kalman_gain = tmp_P.arrayTimes(S_k);

        return Kalman_gain;
    }

    //업데이트한 상태
    private void update_z(){
        //double[][] pre_z = cost_func(past_x, past_y, Refx, Refy, dist);
        Matrix K_k = kalman_gain();
       // double [][] pre_e_k = innovation(dist);
        //Matrix e_k = new Matrix(pre_e_k);
       // K_k = K_k.arrayTimesEquals(e_k);

      //  Matrix post_z = pre_z.plus(K_k);

    }

    //업데이트한 공분산 행렬
    private void update_cov_matrix(){

    }
}
