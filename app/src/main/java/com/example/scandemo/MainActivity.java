package com.example.scandemo;

import static android.os.SystemClock.sleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.lang.UProperty;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;

public class MainActivity extends AppCompatActivity {
    Button open_button, scan_button, clean_button;
    TextView view;
    String name = "/dev/ttyS1";
    int baud = 115200;
    String path = "/sys/kernel/my_dev_ctrl/gpio_ctrl";
    private static final String TAG = "MHJ";
    private Boolean flag_textview = true;

    private IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = findViewById(R.id.textView);
        view.setMovementMethod(ScrollingMovementMethod.getInstance());
        open_button = findViewById(R.id.button_serial);
        scan_button = findViewById(R.id.button_scan);
        clean_button = findViewById(R.id.button_clean);
        open_button.setOnClickListener(v -> {
            seralOpen();
        });
        scan_button.setOnClickListener(v -> {
            scan();
        });
        clean_button.setOnClickListener(v -> {
            view.setText("");
        });
        Log.d(TAG, "onCreate: success!");

        intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.action.keyevent.KEYCODE_KEYCODE_SCAN_L_DOWN");
        intentFilter.addAction("com.android.action.keyevent.KEYCODE_KEYCODE_SCAN_L_UP");
        registerReceiver(receiver, intentFilter);

    }
    @Override
    protected void onResume() {
        super.onResume();
        write("7");
        Log.d(TAG, "onResume: write 7 shangdian");
    }

    @Override
    protected void onPause() {
        super.onPause();
        write("8");
        Log.d(TAG, "onPause: write 8 xiadian");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private android_serialport_api.SerialPort mSerialPort = null;
    private InputStream mInputStream;

    private void seralOpen() {
        if(mSerialPort != null){
            open_button.setText("打开");
            stopSerail();
        }else{
            open_button.setText("关闭");
            initSerail();
        }

    }

    private void initSerail() {
        if (mSerialPort == null) {
            try {
                mSerialPort = new android_serialport_api.SerialPort(new File(name), baud, 0, 8, 1, 0);
                mInputStream = mSerialPort.getInputStream();
                mReadThread = new ReadThread();
                mReadThread.start();
                Log.d(TAG, "InitSerail: init success!" + mSerialPort);
            } catch (Exception e) {
                view.setText("串口初始化失败:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    private void stopSerail() {
        try {
            if (mSerialPort != null) {
                mSerialPort.close();
                if (mInputStream != null)
                    mInputStream.close();
                mSerialPort = null;
                if (mReadThread != null && mReadThread.isAlive())
                    mReadThread.interrupt();
                mReadThread = null;
            }
            Log.d(TAG, "StopSerail: stop success");
        } catch (Exception e) {
            view.setText("关闭发生异常:" + e.getMessage());
            e.printStackTrace();
        }

    }

    private void scan() {
        write("10");
        //Log.d(TAG, "Scan: write 10 success!");
        sleep(200);
        write("11");
       // Log.d(TAG, "Scan: write 11 success!");
    }

    private void write(String i) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(i);
            Log.d(TAG, "write: write success " + i);
            writer.close();
        } catch (Exception e) {
            view.setText("触发异常:" + e.getMessage());
            e.printStackTrace();
        }
    }


    private ReadThread mReadThread = null;

    class ReadThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                boolean flag = false;
                int off_size = 0;
                byte[] buffer = new byte[3072];
                while (true) {
                    try {
                        int len = Math.min(mInputStream.available(), buffer.length);
                        Log.d("read", "run: mInputStream.available() = " + mInputStream.available());
                        Log.d("read", "run: len_length = " + len);
                        int ReadResult = mInputStream.read(buffer, off_size, len);
                        Log.d("read", "run: readresult = " + ReadResult);
                        Log.d("read", "run: buffer =" + new String(buffer));
                        if (ReadResult > 0) updateView(new String(buffer));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateView(String s) {
        runOnUiThread(() -> {
            if(flag_textview == true) view.setText("");
            view.append(s + "\n");
            int offset=view.getLineCount()*view.getLineHeight() - view.getHeight() + 10;
            Log.d("offset", "updateView: " + offset);
            Log.d("offset", "updateView: " + view.getLineCount());
            Log.d("offset", "updateView: " + view.getLineHeight());
            Log.d("offset", "updateView: " + view.getHeight());
            view.scrollTo(0,offset);

            flag_textview = false;
        });
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "com.android.action.keyevent.KEYCODE_KEYCODE_SCAN_L_DOWN":
                        write("10");
                        break;
                    case "com.android.action.keyevent.KEYCODE_KEYCODE_SCAN_L_UP":
                        write("11");
                        break;
                }
            }
        }
    };

}
