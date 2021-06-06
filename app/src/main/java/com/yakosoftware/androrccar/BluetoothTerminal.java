package com.yakosoftware.androrccar;


import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


public class BluetoothTerminal implements Runnable {//farklı bir thread'de çalışacağı için Runnable'ı implement ediyoruz

    private boolean connected = false;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice = null;
    protected BlockingQueue<String> queue = null;
    private Activity parentActivity;
    private boolean disconnect = false;
    private TextView t;


    public BluetoothTerminal(BlockingQueue<String> queue, Activity parentActivity,
                             BluetoothDevice chosenDevice, TextView t, boolean disconnect) {
        this.queue = queue;
        this.parentActivity = parentActivity;
        this.mmDevice = chosenDevice;
        this.t = t;
        this.disconnect = disconnect;
    }

    @Override
    public void run() {
        String msg;
        disconnect = false;



        //HC-06 modülün UUID'sini tanımlıyoruz bunun dışındaki cihazlara uygulama üzerinden bağlantı fail olacak
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


        try {
            if (mmDevice != null) {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);

                if (!mmSocket.isConnected()){
                    mmSocket.connect();
                    parentActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            t.setText("Connected");
                        }
                    });
                    connected = true;
                }

                while(connected) {
                    try {
                        if (disconnect) {
                            mmSocket.close();
                            connected = false;
                        }
                        msg = (String) queue.take();
                        sendBtMsg(msg);
                    } catch (InterruptedException e) {
                        connected = false;
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
            parentActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(parentActivity, "Connection failed",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void sendBtMsg(String msg) {
        OutputStream mmOutputStream;
        try {
            mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
    }
    public void setDisconnect(boolean dc) {
        disconnect = dc;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setMmDevice(BluetoothDevice mm) {
        mmDevice = mm;
    }

}
