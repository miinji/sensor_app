package com.example.sensor_app2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class ImageModule implements View.OnTouchListener{

    private ImageView imageView;
    private Bitmap bitmap, bitmap_altered, arrow; //bitmap : 원본 이미지 , altered : 위에 그림을 그릴 이미지
    private Canvas canvas;
    private int img_w, img_h;

    //Touch event
    private Matrix matrix = new Matrix();
    private Matrix prev_matrix = new Matrix(); // 현재 수열말고 그전 수열에서 얼마나 바뀌었는지 비교하기 위해 생성

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE; // 현재 touch 이벤트

    private PointF start = new PointF(); // 값을 두 개 저장할 수 있는 클래스 시작 지점 끝지점
    private PointF mid = new PointF(); // 중간 지점 저장
    private float[] lastEvent = null; // 마지막에 어떤 이벤트가 발생했는가
    private float oldDist = 1f; // 그전의 distance 저장

    ImageModule(Activity activity){
        imageView = (ImageView) activity.findViewById(R.id.imageView);
        imageView.setOnTouchListener(this);
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        // Load map image(지도 이미지 불러오기)
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 크기을 먼저 불러와서 크기를 줄인다.
        options.inJustDecodeBounds = true;
        // bound 만 읽어옴
        BitmapFactory.decodeResource(activity.getResources(), R.raw.knu_library_1f, options);
        img_h = options.outHeight;
        img_w = options.outWidth;

        // Load map image
        InputStream inputStream;
        inputStream = activity.getApplicationContext().getResources().openRawResource(R.raw.knu_library_1f);
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(inputStream, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Rect rect = new Rect(0, 0, img_h, img_w); // 이 사각형 안에 있는 이미지를 읽어올것이다.
        options.inJustDecodeBounds = false; // 이미지 전체를 읽을 것이다.
        options.inSampleSize = 4; //이미지를 두 배로 압축해서 읽어오자

        bitmap = decoder.decodeRegion(rect, options); //원본
        bitmap_altered = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getWidth(), bitmap.getConfig());

        // 비트맵을 그림판에 출력
        canvas = new Canvas(bitmap_altered);
        canvas.drawBitmap(bitmap, 0,0, null);

        // 그림판을 스크린(imageView)에 출력
        imageView.setImageBitmap(bitmap_altered);

        // 화살표 그림을 가져옴
        arrow = BitmapFactory.decodeResource(activity.getResources(), R.raw.arrow);

        plot_arrow(100, 100, 30);

    }

    //화살표 그리는 method
    public void plot_arrow(float x, float y, float deg){
        canvas.drawBitmap(bitmap, 0, 0, null);
        Matrix matrix = new Matrix();
        matrix.postScale(0.3f, 0.3f);
        matrix.postRotate(deg);
        Bitmap rotated_arrow = Bitmap.createBitmap(arrow, 0, 0, arrow.getWidth(), arrow.getHeight(), matrix, true);

        canvas.drawBitmap(rotated_arrow, x, y, null);
        imageView.invalidate(); // 이미지 갱신
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;
                lastEvent = null;
                prev_matrix.set(matrix);
                start.set(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    prev_matrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(prev_matrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                }
                else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(prev_matrix);
                        float scale = (newDist / oldDist);
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                    //float newDist = spacing(event);
                }
                break;

            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }

        imageView.setImageMatrix(matrix);

        //bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
        //Canvas canvas = new Canvas(bitmap);
        //view.draw(canvas);

        return true;
    }
    private float spacing(MotionEvent motionEvent) {
        float x = motionEvent.getX(0) - motionEvent.getX(1);
        float y = motionEvent.getY(0) - motionEvent.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent motionEvent) {
        float x = motionEvent.getX(0) + motionEvent.getX(1);
        float y = motionEvent.getY(0) + motionEvent.getY(1);
        point.set(x / 2, y / 2);
    }
}
