package com.example.sensor_app2;

import static android.os.SystemClock.elapsedRealtime;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

//main에 sensor에 필요한 것들 다 가져오면 됨
public class SensorModule implements SensorEventListener{

    private boolean flag_is_sensor_running = false;
    private Activity mActivity; //현재의 엑티비티를 저장할 수 있는 공간을 만듦
    private  FileModule file;

    SensorManager sm;
    Sensor s1, s2, s3, s4, s5, s6, s7, s8;

    private int [] count = new int[8]; // 센서 측정 개수를 저장

    private int sensor_sampling_interval_ms = 10; // 측정 간격
    private long sensor_measurement_start_time; // 센서 측정을 언제 시작 했는지

    //데이터 저장 공간
    float[] accL = new float[4]; // acc(디바이스 좌표계에서 측정)
    float[] gyroL = new float[4]; // gyro
    float[] magL = new float[4]; // magnetic
    float prx;                 // proximity
    float press;                // air pressure
    float light;                // light

    float[] quat = new float[4]; //quaternion
    float[] game_quat = new float[4]; //game quaternion

    float[] rot_mat = new float[16];
    // float[][] rMat = new float[3][3]; -> 2차원배열로 해도 됨!
    float[] rot_mat_opengl = new float[16]; // 단말의 orientation에 상관없이 무조건 9.8이 나오도록
    float[] game_rot_mat = new float[16];
    float[] orientation_angle = new float[4];
    float[] accW = new float[4]; // 기준좌표계(World 좌표계)
    float[] gyroW = new float[4];

    // 현재까지 측정 결과
    String current_state = "";

    SensorModule(Activity activity){
        mActivity = activity; //m액티비티에 현재 엑티비티 저장
        sm = (SensorManager) activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        s1 = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        s2 = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        s3 = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        s4 = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        s5 = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        s6 = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        //가상의 센서들
        s7 = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        s8 = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    public void start(long start_time, FileModule file_in){
        Log.d("SENSOR_MODULE", "Started");

        flag_is_sensor_running = true;
        sensor_measurement_start_time = start_time;
        file = file_in;
        //registerListener ;; 원하는 센서의 데이터 수집, SensorEventListener;센서에 새로운 값 측정되면 처리
        sm.registerListener((SensorEventListener) this, s1, SensorManager.SENSOR_DELAY_NORMAL);
        //SamplingPeriodUs 자리에 10000 혹은 SensorManager.SENSOR_DELAY_FASTEST, SENSOR_DELAY_NORMAL 를 사용한다.
        sm.registerListener((SensorEventListener) this, s2, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener((SensorEventListener) this, s3, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener((SensorEventListener) this, s4, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener((SensorEventListener) this, s5, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener((SensorEventListener) this, s6, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener((SensorEventListener) this, s7, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener((SensorEventListener) this, s8, SensorManager.SENSOR_DELAY_NORMAL);

        for (int i = 0; i<8; i++){ // count 배열 초기화
            count[i] = 0;
        }

    }
    public void stop(){
        flag_is_sensor_running = false;

        sm.unregisterListener((SensorEventListener) this, s1);
        sm.unregisterListener((SensorEventListener) this, s2);
        sm.unregisterListener((SensorEventListener) this, s3);
        sm.unregisterListener((SensorEventListener) this, s4);
        sm.unregisterListener((SensorEventListener) this, s5);
        sm.unregisterListener((SensorEventListener) this, s6);
        sm.unregisterListener((SensorEventListener) this, s7);
        sm.unregisterListener((SensorEventListener) this, s8);
    }

    public float get_heading(){
        return orientation_angle[0] * 180f / 3.141592f; //첫번째 요소가 heading z축
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d("SENSOR_MODULE", orientation_angle[0] + "");

        float elapsed_time_s = (float)(elapsedRealtime()/1e3 - sensor_measurement_start_time/1e3);
        float elapsed_fw_time_s = (float)(sensorEvent.timestamp / 1e9 - sensor_measurement_start_time / 1e3);

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, accL, 0, 3);
            file.save_str_to_file(String.format("ACC, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, accL[0], accL[1], accL[2]));
            count[0] += 1;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(sensorEvent.values, 0, gyroL, 0, 3);
            file.save_str_to_file(String.format("GYRO, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, gyroL[0], gyroL[1], gyroL[2]));
            count[1] += 1;

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, magL, 0, 3);
            file.save_str_to_file(String.format("MAG, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, magL[0], magL[1], magL[2]));
            count[2] += 1;

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            prx = sensorEvent.values[0];
            file.save_str_to_file(String.format("PRX, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, prx));
            count[3] += 1;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {
            press = sensorEvent.values[0];
            file.save_str_to_file(String.format("PRESS, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, press));
            count[4] += 1;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            light = sensorEvent.values[0];
            file.save_str_to_file(String.format("LIGHT, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, light));
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            System.arraycopy(sensorEvent.values, 0, quat, 0, 4);
            SensorManager.getRotationMatrixFromVector(rot_mat, quat); //quaternion값을 이용해 회전변환획득
            SensorManager.getOrientation(rot_mat, orientation_angle); //회전 변환으로부터 단말의 euler angles획득
            Matrix.transposeM(rot_mat_opengl, 0, rot_mat, 0); //가속도값 획득
            Matrix.multiplyMV(accW, 0, rot_mat_opengl, 0, accL, 0);
            file.save_str_to_file(String.format("ROT_VEC, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, quat[0], quat[1], quat[2], quat[3]));
            count[5] += 1;
            Log.d("SENSOR_MODULE", orientation_angle[0] + "");
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            System.arraycopy(sensorEvent.values, 0, game_quat, 0, 4);
            file.save_str_to_file(String.format("GAME_ROT_VEC, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, game_quat[0], game_quat[1], game_quat[2], game_quat[3]));
            count[6] += 1;
        }
        String str = "";
        str += String.format("Acc x: %2.3f, y: %2.3f, z: %2.3f\n", accL[0], accL[1], accL[2]);
        str += String.format("Gyro x: %2.3f, y: %2.3f, z: %2.3f\n", gyroL[0], gyroL[1], gyroL[2]);
        str += String.format("Mag x: %2.3f, y: %2.3f, z: %2.3f\n", magL[0], magL[1], magL[2]);
        str += String.format("Prx: %2.3f cm\n", prx);
        str += String.format("Press: %2.3f hPa\n", press);
        str += String.format("Light: %2.3f\n", light);
        str += String.format("Heading: %f, Pitch: %f, Roll: %f\n", orientation_angle[0]*180/3.1415, orientation_angle[1]*180/3.1415, orientation_angle[2]*180/3.1415);
        str += String.format("AccW x: %2.3f, y: %2.3f, z: %2.3f", accW[0], accW[1], accW[2]);

        current_state = str;

    }

    public String get_latest_state(){
        return current_state;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i){
    }
}
