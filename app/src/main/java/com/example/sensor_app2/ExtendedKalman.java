package com.example.sensor_app2;

import Jama.*;

import java.util.ArrayList;
import java.util.List;
// positioning에서 초기값 받아오기
// wifimodule에서 경로손실모델 dn 가져오기
// Refx, Refy data String으로 해서 받아오기

public class ExtendedKalman {
    public Positioning positioning;
    // 위치 추정 기법 중에 칼만 필터를 이용하여 x-y좌표를 찾는 것을 구현
    // 강의 7 참고
    // 경로손실모델로 얻은 길이는 wifimodule에서 받아오기 -> -------------- 와이파이 신호를 받아와야함


    //초기 공분산 행렬
    double sigma = 1;
    double [][] vals = {{sigma, 0},{0, sigma}}; // 초기 공분산 행렬
    Matrix init_cov_Matrix = new Matrix(vals);

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

    double [] Refx = new double[5]; //positioning 에서 받아오기 ***********************************************
    double [] Refy = new double[5];
    double [] dist = new double[5]; // 경로손실모델로 얻은 dist   -> wifimodule에 있음
    double past_x; //이전 값 저장
    double past_y;


    //공분산 행렬
   public Matrix Make_cov_Matrix(double [] x_val, double [] y_val){
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

    //야코비안 행렬 / input - 상위 5개 AP 신호 좌표, 추정한 좌표
    public Matrix Make_Jacobian(double [] Refx, double [] Refy, double x, double y) {
        double [][] jacobian = new double[5][2];
        for(int i = 0;i<5;i++){
            double dis = Math.pow((Math.pow(x - Refx[i],2) + Math.pow(y - Refy[i],2)),0.5);
            jacobian[i][0] = (x-Refx[i]) / dis;
            jacobian[i][1] = (y-Refy[i]) / dis;
            }
        Matrix jacobian_Matrix = new Matrix(jacobian);
        return jacobian_Matrix;
        }

    //------------------------------상태업데이트-----------------------------------//

    //z는 이전 상태와 같다고 가정, P는 시그마tr을 더하는 걸로
    //이전의 x,y값, 상위 AP신호의 x,y값, 경로손실모델로 얻은 거리,
    public void update_state(double [] Refx, double [] Refy, double [] dist, double x, double y, Matrix P){
        //double [] error_dist = new double[5];
       // double [] func_z = new double[5];
        double [] func_h = new double[5];
        double [][] func_e = new double[5][1];
        double [][] val_z = {{x},{y}}; // z의 상태추정 -> 센서신호있으면 추가 없으면 이전의 값과 같다고 판단단
        Matrix z = new Matrix(val_z);
        Matrix func_P = P.plus(sigma_tr_Matrix); // P의 상태추정, constant인 sigma_tr과 더함

        for(int i = 0;i < 5;i++){
            //error_dist[i] = Math.pow(Math.pow(x - Refx[i],2) + Math.pow(y - Refy[i],2),0.5) - dist[i];
            func_h[i] = Math.pow(Math.pow(x - Refx[i],2) + Math.pow(y - Refy[i],2),0.5); // Matrix norm 만 따로 구현
            //func_h[i] = func_z[i] + error_dist[i];
        }

        //innovation 행렬로 만들기 위해 double [][]
        for(int i = 0;i<5;i++){
            func_e[i][0] = dist[i] - func_h[i];
        }
        Matrix func_e_matrix = new Matrix(func_e);

        //Jacobain 적용
        Matrix func_H = Make_Jacobian(Refx,Refy,x,y);
        Matrix W_k = new Matrix(val_diag);

        //func_e의 공분산 행렬
        double [][] S_k = new double[5][5];
        Matrix func_S = new Matrix(S_k);
        func_S = func_H.arrayTimes(func_P);
        func_S = func_S.arrayTimesEquals(func_H.transpose());
        func_S = func_S.plus(W_k);

        //kalman gain
        double [][] gain = new double[2][5];
        Matrix kalman_gain = new Matrix(gain);
        kalman_gain = func_P.arrayTimes(func_H.transpose());
        kalman_gain = kalman_gain.arrayTimesEquals(func_S.inverse());


        //update된 상태
        double [][] update = new double[2][1];
        Matrix update_z = new Matrix(update);
        update_z = kalman_gain.arrayTimes(func_e_matrix);
        update_z = update_z.plus(z);

        //update 된 공분산 행렬
        double [][] update_cov = new double[2][2];
        Matrix update_cov_matrix = new Matrix(update_cov);
        Matrix tmp_cov = kalman_gain.arrayTimes(func_H);
        tmp_cov = identity_matrix.minus(tmp_cov);
        update_cov_matrix = tmp_cov.arrayTimes(func_P);

//update된 z와 P를 다시 신호 받아올 때까지 저장해두고, 이전 값으로 사용

    }


}
