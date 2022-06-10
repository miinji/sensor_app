package com.example.sensor_app2;

import static android.os.SystemClock.elapsedRealtime;

import android.app.Activity;
import android.os.Build;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FileModule {
    //class ���� ����
    private File file;
    private Activity mActivity;
    private boolean is_file_created = false;

    FileModule(Activity activity, String filename){
        mActivity = activity;
        create_file(filename);
    }

    FileModule(Activity activity, String filename, boolean append_date, boolean append_model, String extension){
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        String model = Build.MODEL;

        if (append_date){
            filename += "_" + date;
        }
        if(append_model)
            filename += "_" + date;
        filename += extension;

        mActivity = activity;
        create_file(filename);

    }
    //create_file
    private void create_file(String filename){
        File folder = new File(mActivity.getApplicationContext().getExternalFilesDir(null), "measurement_data");
        if(!folder.exists())
            folder.mkdir();

        file = new File(folder, filename);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(mActivity.getApplicationContext(), "[ERROR] Failed to create file", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        is_file_created = true;

        // put header(���� ��������, ��ġ, �ܸ����� ��)
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        String header = "";
        header += "## Date: " + date + "\n"; // ���߿� ���Ͽ��� "##" �� ���� ������ ��������̹Ƿ� �ڼ��ϰ� �� �ʿ� ����
        header += "## File creation time since boot (ms): " + elapsedRealtime() + "\n";
        header += "## Model: " + Build.MODEL + "\n";
        header += "## SDK version: " + Build.VERSION.SDK_INT + "\n";
        save_str_to_file(header);

    }


    public void save_str_to_file(String data){
        // save a single line to file
        if (!is_file_created)
            return; // ������ �������� �ʾ��� �� ����ǵ��� ������ġ.
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            fos.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
