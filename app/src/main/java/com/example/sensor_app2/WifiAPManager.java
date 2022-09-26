package com.example.sensor_app2;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class WifiAPManager {
    // 현재 층에 설치된 AP의 정보를 관리

    private ArrayList<APInfo> apInfoList = new ArrayList<>();

    public WifiAPManager(Context context){
        String line;

        try {
            InputStream inputStream = context.getAssets().open("KNU_library_1f.txt");

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while((line = bufferedReader.readLine()) != null){
                // AP정보가 저장된 파일에서 한줄씩 읽어온다
                // 첫글자가 #인 부분은 건너뛴다
                if (line.charAt(0) == '#')
                    continue;
                APInfo apInfo = new APInfo(line);
                apInfoList.add(apInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("WifiAPManager", String.format("Loaded %d AP(s)", apInfoList.size()));
    }

    class APInfo{
        public String mac_addr;
        public float x, y;
        public int freq;

        public APInfo(String line){
            String[] items = line.split(" ");
            mac_addr =items[0];
            x = Float.parseFloat(items[1]);
            y = Float.parseFloat(items[2]);
            freq = Integer.parseInt(items[3]);

            validate();
        }

        private void validate(){
            Log.d("APInfo", String.format("mac:%s, x:%lf, y:%lf, freq:%d", mac_addr, x, y, freq));
        }
    }
}
