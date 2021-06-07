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


    public BluetoothTerminal(BlockingQueue<String> queue, Activity parentActivity,//BluetoothTerminal'in Constructor'u
                             BluetoothDevice chosenDevice, TextView t, boolean disconnect) {
        //Bluetooth terminalinin değişkenleri MainActivity'den aldığımız değişkenlerle assign ediliyor
        this.queue = queue;
        this.parentActivity = parentActivity;
        this.mmDevice = chosenDevice;
        this.t = t;
        this.disconnect = disconnect;
    }

    @Override
    public void run() {//Main Thread'den farklı bir thread'de çalışacağı için Runnable interface'inden implement ettiğimiz
        //run metodunda Seri port işlemleri gerçekleşiyor
        String msg;//seri porta yollanacak mesaj için string
        disconnect = false;//Bluetooth aygıtının bağlantı durumu



        //HC-06 modülün UUID'sini tanımlıyoruz bunun dışındaki cihazlara uygulama üzerinden bağlantı fail olacak
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


        try {//Socket yapısında IO işlemleri yapacağımız için IOException'u handle etmek zorundayız
            if (mmDevice != null) {//eğer bluetooth aygıtı seçilip bağlantı gerçekleştiyse burası null olmayacak
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);//aygıtımızdan bir socket oluşturuyoruz

                if (!mmSocket.isConnected()){//socket'in bağlı olup olmadığı kontrol ediliyor
                    mmSocket.connect();//sockete bağlanılıyor
                    parentActivity.runOnUiThread(new Runnable() {//main activity'nin ui thread'inde değişiklik yapmak için ui threadinde çalıştırılıyor
                        public void run() {
                            t.setText("Connected");//ui'daki text Connected olarak değiştiriliyor
                        }
                    });
                    connected = true;//connected boolean'i true olarak güncelleniyor
                }

                while(connected) {//aygıta connect olunduğu sürece keisntisiz veri akışı için sonsuz döngü çalışıyor
                    try {
                        if (disconnect) {//disconnect true olursa
                            mmSocket.close();//socket bağlantısı kapatılıyor
                            connected = false;//connected false olarak değiştiriliyor
                        }
                        msg = (String) queue.take();//ana thread'den kuyruğa eklenen veri take ediliyor ve mesaja atanıyor
                        sendBtMsg(msg);//sendBtMsg metodu aracılığıyla mesaj bluetooth üzerinden arduino'nun seri portuna gönderiliyor
                    } catch (InterruptedException e) {//hata olması durumunda yakalanıyor
                        connected = false;//connected false ediliyor
                        e.printStackTrace();//hata mesajı konsola bastırılıyor
                    }
                }
            }
        } catch (IOException e) {//IO ile ilgili bir hata olması durumunda
            e.printStackTrace();//hata mesajı konsola bastırılıyor
            connected = false;//connected yine false ediliyor
            parentActivity.runOnUiThread(new Runnable() {
                public void run() {//ui threadine katılınıyor
                    Toast.makeText(parentActivity, "Connection failed",//connection failed şeklinde bir toast mesajı bastırılıyor
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
