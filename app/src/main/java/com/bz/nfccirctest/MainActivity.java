package com.bz.nfccirctest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.hardware.Camera;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bozen.camerascan.BzApiManager;
import com.ivsign.android.IDCReader.IDCardInfo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "888888";
    private TextView mTextView,mTextGps,mTextScan ;
    private Handler mHandler = new Handler();
    private static ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isCircTest = new AtomicBoolean(false);
    private BzApiManager manager;
    private Location mLocation = new Location("demo");
    private String scan_result = "MDQ2MGQ4MDQyMTg1OTcwNGYyMDAxOTE1OTQzODAwMDAwMDAwMDAwMDAwMjMwNTEyMTAwNjQyNDAwMDA1NDU0MTBhMDAwMDAxNg==";

    Camera redCamera;
    Camera colorCamera;
    SurfaceView surfaceViewRed;
    SurfaceView surfaceViewColor;
    SurfaceHolder surfaceHolderRed;
    SurfaceHolder surfaceHolderColor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.tp);
        mTextGps = findViewById(R.id.gps);
        mTextScan = findViewById(R.id.sacn);

        manager = BzApiManager.getInstance(this);
        manager.enableIRLight();

        surfaceViewRed = findViewById(R.id.redsurface);
        surfaceViewColor = findViewById(R.id.colorsurface);
        surfaceHolderRed = surfaceViewRed.getHolder();
        surfaceHolderColor = surfaceViewColor.getHolder();

        initColorCamera();
        initRedCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {

            @Override
            public void run() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "====开始执行====");
                        StartTest(findViewById(R.id.btnCard));
                        StartGps(findViewById(R.id.btnLoaction));
                        StartScan(findViewById(R.id.btnScan));
                    }
                }, 3000);
            }
        }).start();
    }

    private void initRedCamera() {
        redCamera = Camera.open(1);
        redCamera.setDisplayOrientation(0);
        Camera.Parameters parameters = redCamera.getParameters();
        final int w = parameters.getPreviewSize().width;
        final int h = parameters.getPreviewSize().height;
        redCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //预览帧数据处理
            }
        });
        redCamera.startPreview();   //显示相机
        surfaceHolderRed.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    redCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }

    public void initColorCamera() {
        colorCamera = Camera.open(0);
        colorCamera.setDisplayOrientation(0);
        Camera.Parameters parameters = colorCamera.getParameters();
        final int w = parameters.getPreviewSize().width;
        final int h = parameters.getPreviewSize().height;
        colorCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //预览帧数据处理
            }
        });
        colorCamera.startPreview();   //显示相机
        surfaceHolderColor.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    colorCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void closeCamera(){
        if (redCamera != null) {
            redCamera.release();
            redCamera = null;
        }
        if (colorCamera != null) {
            colorCamera.release();
            colorCamera = null;
        }
    }

    private void showMsg(final boolean isError, final String msg){
        Log.e(TAG,msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String timeInfo = "" + System.currentTimeMillis();
                String msgInfo = timeInfo + ":" + msg;
                mTextView.setText(msgInfo);
                Log.e(TAG,msgInfo);
                if (isError) {
                    mTextView.setTextColor(Color.RED);
                } else {
                    mTextView.setTextColor(Color.BLACK);
                }
            }
        });
    }

    private Runnable getCircCardSerialRunnable = new Runnable() {
        @Override
        public void run() {
            SystemClock.sleep(100);
            if(!isCircTest.get()){
                Log.e(TAG,"用户退出");
                showMsg(true,"用户退出");
                return;
            }

            //Query sync
            do {
                byte cType = manager.nfcQuerySync((byte) 0x26);
                if(cType == 0){
                    return;//call nfcClose
                }
                if(cType < 0){
                    showMsg(true,"寻卡失败:"+cType);
                    continue;
                }
                final byte[] serialData = new byte[5];
                boolean ret = manager.nfcSerial(serialData);
                if(!ret){
                    showMsg(true,"获取序列号失败");
                    continue;
                }
                String serialInfo = "";
                serialInfo += HexDump.toHexString(new byte[]{cType},0,1);
                serialInfo += ":";
                serialInfo += HexDump.toHexString(serialData,0,4);
                showMsg(false,"获取序列号成功\n"+serialInfo);
                manager.nfcHalt();
                SystemClock.sleep(1000);
            }while (isCircTest.get());

            //Query async
//            if(!manager.nfcQuery((byte) 0x26, new BzApiManager.NfcWaitLisenter() {
//                @Override
//                public void waitResult(byte cType) {
//                    if(cType == 0){
//                        if(isCircTest.get()) mSingleThreadExecutor.execute(getCircCardSerialRunnable);
//                        return;//call nfcClose
//                    }
//                    if(cType < 0){
//                        showMsg(true,"寻卡失败:"+cType);
//                        if(isCircTest.get()) mSingleThreadExecutor.execute(getCircCardSerialRunnable);
//                        return;
//                    }
//                    final byte[] serialData = new byte[5];
//                    boolean ret = manager.nfcSerial(serialData);
//                    if(!ret){
//                        showMsg(true,"获取序列号失败");
//                        if(isCircTest.get()) mSingleThreadExecutor.execute(getCircCardSerialRunnable);
//                        return;
//                    }
//                    String serialInfo = "";
//                    serialInfo += HexDump.toHexString(new byte[]{cType},0,1);
//                    serialInfo += ":";
//                    serialInfo += HexDump.toHexString(serialData,0,4);
//
//                    showMsg(false,"获取序列号成功\n"+serialInfo);
//
//                    if(isCircTest.get()) mSingleThreadExecutor.execute(getCircCardSerialRunnable);
//                }
//            })){
//               if(isCircTest.get()) mSingleThreadExecutor.execute(getCircCardSerialRunnable);
//            }
        }
    };

    public void StartNfc(){
        if(isCircTest.get()){
            Toast.makeText(this,"循环测试中...",Toast.LENGTH_SHORT).show();
            return;
        }
        int handle = manager.nfcOpen();
        if(handle < 0){
            showMsg(true,"打开NFC设备失败:"+handle);
            return;
        }
        int result = manager.nfcPcdconfig();
        if(result < 0){
            showMsg(true,"NFC设备初始化失败:"+result);
            return;
        }

        isCircTest.set(true);
        mSingleThreadExecutor.execute(getCircCardSerialRunnable);
    }

    public void StartTest(View view){
        if(isCircTest.get()){
            Toast.makeText(this,"循环测试中...",Toast.LENGTH_SHORT).show();
            return;
        }
        int handle = manager.nfcOpen();
        if(handle < 0){
            showMsg(true,"打开NFC设备失败:"+handle);
            return;
        }
        int result = manager.nfcPcdconfig();
        if(result < 0){
            showMsg(true,"NFC设备初始化失败:"+result);
            return;
        }

        isCircTest.set(true);
        mSingleThreadExecutor.execute(getCircCardSerialRunnable);
    }

    public void StopTest(View view){
        if(!isCircTest.get()){
            Toast.makeText(this,"没有测试进行...",Toast.LENGTH_SHORT).show();
            return;
        }else{
            isCircTest.set(false);
            manager.nfcClose();
        }
    }

    private static String utcToDateString(long timems,int zonehour){
        if(timems <= 0){
            Log.e("bozheng","Java Demo utcToDateString: timems <= 0, timems = "+timems);
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(timems));
        calendar.set(Calendar.HOUR,calendar.get(Calendar.HOUR)+ zonehour);
        Date bjDate = calendar.getTime();
        String localTime = sdf.format(bjDate.getTime());
        Log.e("bozheng","Java Demo utcToDateString: localTime = "+localTime);
        return localTime;
    }

    private BzApiManager.ScanResultCallback scanResultCallback = new BzApiManager.ScanResultCallback() {
        @Override
        public void scanResult(final String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(scan_result.equals(s)){
                        mTextScan.setText("扫码成功："+s);
                    }else{
                        mTextScan.setText("扫码失败："+s);
                    }
                }
            });
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void updateLocationResult(Location location) {
            Log.e("yxljl1314","demo updateLocationResult 1");
            mLocation = new Location(location);
            if(mLocation.getLatitude() <= 0 || mLocation.getLongitude() <= 0
                    || mLocation.getBearingAccuracyDegrees() <= 0
                    || mLocation.getTime() <= 0){
                Log.e("yxljl1314","demo getLatitude:"+mLocation.getLatitude());
                Log.e("yxljl1314","demo getLongitude:"+mLocation.getLongitude());
                Log.e("yxljl1314","demo getBearingAccuracyDegrees:"+mLocation.getBearingAccuracyDegrees());
                Log.e("yxljl1314","demo getTime:"+mLocation.getTime());
                Log.e("yxljl1314","demo updateLocationResult 2");
                return;//no need update
            }
            Log.e("yxljl1314","demo updateLocationResult 3");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("yxljl1314","demo updateLocationResult 4");
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("终端毫秒:"+System.currentTimeMillis());
                    stringBuffer.append("\n");

                    stringBuffer.append("经度:"+mLocation.getLongitude());
                    stringBuffer.append("\n");

                    stringBuffer.append("纬度:"+mLocation.getLatitude());
                    stringBuffer.append("\n");

                    stringBuffer.append("速度:"+String.format("%.2f", mLocation.getSpeed()*1.852)+"km/h");
                    stringBuffer.append("\n");

                    mTextGps.setText(stringBuffer.toString());
                    Log.e("yxljl1314","demo updateLocationResult 5");
                }
            });
        }

        @Override
        public void notifyIdcard(IDCardInfo idCardInfo) {

        }
    };

    public void StartGps(View view){
        manager.startLocation(scanResultCallback);
    }

    public void StopGps(View view){
        manager.stopLocation();
    }

    public void StartScan(View view){
        manager.startScan(scanResultCallback);
    }

    public void StopScan(View view){
        manager.stopScan();
    }

    public void StopCamera(View view) {
        if (redCamera != null) {
            redCamera.setPreviewCallback(null);
            redCamera.stopPreview();
        }
        if (colorCamera != null) {
            colorCamera.setPreviewCallback(null);
            colorCamera.stopPreview();
        }

        Permission.cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                closeCamera();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        isCircTest.set(false);
        manager.nfcClose();
        manager.disableIRLight();
        manager.stopLocation();
        manager.stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (redCamera != null) {
            redCamera.setPreviewCallback(null);
            redCamera.stopPreview();
        }
        if (colorCamera != null) {
            colorCamera.setPreviewCallback(null);
            colorCamera.stopPreview();
        }

        Permission.cachedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                closeCamera();
            }
        });
    }
}