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
    public ArrayList<APInfo> apInfoList = new ArrayList<>();

    WifiAPManager(Context context){
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
        public String name, mac_addr1, mac_addr2;
        public float ref_x, ref_y, x, y;
        public float scale;

        public APInfo(String line){
            String[] items = line.split(",");
            name =items[0];
            ref_x = Float.parseFloat(items[1]);
            ref_y = Float.parseFloat(items[2]);
            x = Float.parseFloat(items[3]);
            y = Float.parseFloat(items[4]);
            mac_addr1 = items[5];
            mac_addr2 = items[6];
            scale = Float.parseFloat(items[7]);

            validate();
        }

        private void validate(){
            Log.d("APInfo", String.format("name: %s, mac:%s %s, x:%f, y:%f",name, mac_addr1, mac_addr2,x, y));
        }
    }
}
