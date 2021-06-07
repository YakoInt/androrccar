package com.yakosoftware.androrccar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {


    // Yön sabitleri
    final int IR = -1; //oldState'in hiçbirşey olmadığını belirtmek için tanımladığımız sabit
    final int S = 0;//Stop
    final int F = 1;//forward-ileri
    final int B = 2;//backward-geri
    final int L = 3;//left-sol
    final int Ri = 4;//right-sağ


    // Bluetooth terminaline verileri göndermek için tanımladığımız BlockingQueue yapısı
    //Bu queue yapısını consumer-producer yapısıyla kullanıp bluetooth terminali yani
    //seri port işlemleri ile UI işlemlerini farklı thread de yapmak için kullanıyoruz zira
    // Blockingqueue yapısı Multithreading desteklemektedir bu yapı ile UI thread'i producer
    //ve bluetooth thread consumer olacak şekilde bir sistem kuruyoruz
    BlockingQueue<String> queue = new LinkedBlockingQueue<String>();

    private boolean lightsIsOn = false;//Işıklar açık/kapalı


    /* 2 uzunluklu int dizisinin ilk elemanı 0-> Still Stay, 1-> Forward, 2->Backward sabitlerini tutacak
     * yani ileri geri ve durgun durumlarını;
     * ikinci eleman ise 0-> Still Stay, 1-> Left, 2->Right yani sağ,sol ve durgun sabitlerini tutacak.
     */
    private int[] curState = new int[2];

    private BluetoothAdapter mBluetoothAdapter = null;//Bluetooth adaptörü yani android cihazın bluetooth modülünü yönetmek için kullandığımız değişken
    private BluetoothDevice chosenDevice = null;//Bluetooth adapter'den seçilen bluetooth cihazını atayacağımız değişken

    //UI bileşenlerini tanımlıyoruz
    private ImageButton f,b,l,r,btnHorn,btnSwitchLights;
    private Button retry;
    private Spinner pairedDev;
    private TextView t;
    private String speedStr = "4";
    private RadioButton btnLow,btnMid,btnHigh;

    //BluetoothTerminal sınıfının yani seri port işlemlerinin gerçekleşeceği threadi tanımladık
    Thread mRemoteService = null;
    BluetoothTerminal bt;

    boolean disconnect = false;//bluetooth bağlı mı

    @Override
    public void onStart() {
        super.onStart();

        // Cihazın bluetooth'u aktif değilse aktifleştirmek için bir istek gönderiyoruz
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 2);
            // Otherwise, setup the chat session
        }
        // setupChat() metodu activity result'ı gelene kadar ya da bt aktif olana kadar çalışmaz
        while (!mBluetoothAdapter.isEnabled());

        setupChat();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        disconnect = true;//uygulama pause edilirse yani durdurulursa veya arkaplanda askıya alınırsa disconnect edilir
    }

    @Override
    public synchronized void onResume() {
        super.onResume();//onResume'da super class'ın işlemleri devam eder

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        //curState'in başlangıçtaki elemanlarının değerlerini S yani durgun vaziyete alıyoruz
        curState[0] = S;
        curState[1] = S;

        // Cihazın bluetooth servisini yani adaptörünü mBluetoothAdapter'a çekiyoruz
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Eğer mBluetoothAdapter null ise cihaz bluetooth desteklemiyor bu nedenle bir Toast bastırarak bunu kullanıcıya belirtiyoruz
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // herhangi bir şekilde uygulama kullanıcı tarafından ya da sistem tarafından kapatılırsa bt threadi stop ediliyor
        if (mRemoteService != null) mRemoteService.stop();
    }

    private void setupChat() {
        // Bluetooth terminal threadi mRemoteService değişkeninde oluşturuluyor
        mRemoteService = new Thread(bt);

        //UI bileşenleri ilgili değişkenlere atanıyor
        t =  findViewById(R.id.text);
        f =  findViewById(R.id.imgF);
        b =  findViewById(R.id.imgB);
        l =  findViewById(R.id.imgL);
        r =  findViewById(R.id.imgR);
        retry = findViewById(R.id.retryConnection);
        pairedDev =  findViewById(R.id.pairedDevSpinner);
        bt = new BluetoothTerminal(queue, MainActivity.this, chosenDevice, t, disconnect);
        btnLow = findViewById(R.id.low);
        btnMid = findViewById(R.id.medium);
        btnHigh = findViewById(R.id.high);
        btnSwitchLights = findViewById(R.id.lightSwitch);
        btnHorn = findViewById(R.id.horn);

        // Set yapısına cihazda eşleşmiş olan bt cihazları atanıyor
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        final List<String> devicesList = new ArrayList<>();
        //eşleşmiş cihazlardan HC-06 yani bizim aracımızın bt ismi ile eşleşen cihaz varsa
        //spinnerda ilk sıraya gelmesi için deviceslistte ilk sıraya atanıyor
        //ve bu şekilde liste dolduruluyor
        for(BluetoothDevice device : pairedDevices) {
            if(device.getName().equals("HC-06")) {
                devicesList.add(0, device.getName());
                chosenDevice = device;
            } else {
                devicesList.add(device.getName());
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, devicesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pairedDev.setAdapter(adapter);
        //pairedDev spinner'ından seçilen cihaz chosenDevice(BluetoothDevice) değişkenine atanıyor
        pairedDev.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                for(BluetoothDevice device : pairedDevices) {
                    if(device.getName().equals(devicesList.get(position))) {
                        chosenDevice = device;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        //Hız ayarlaması için butonların listenerlarında işlemler yapılıyor
        btnLow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //eğer ilgili radiobutton seçili ise
                    if(isChecked){
                        speedStr = "2";//hız low yani 2 olarak atanıyor
                        changeSpeed();//changespeed methoduna gönderiliyor

                    }

            }
        });
        //diğer iki radiobutton için de btnLow'da yapılan işlemlerin benzeri gerçekleştiriliyor
        btnMid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    speedStr = "3";
                    changeSpeed();
                }

            }
        });
        btnHigh.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    speedStr = "4";
                    changeSpeed();
                }

            }
        });

        //Aracın korna çalabilmesi için gereken butonun listenerı tanımlanıyor
        btnHorn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {//eğer butona basılıyorsa
                    playHorn();//korna çalma methodu çağrılarak korna çalınıyor
                    btnHorn.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));//butonun rengi değiştiriliyor
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        playHorn();//parmak çekildiğinde tekrar aynı method çağrılarak seri porta korna ile ilgili state değeri gönderiliyor
                        //ve kornanın kapanması sağlanıyor
                        btnHorn.setColorFilter(null);//butonun rengi eski haline geliyor
                    }
                }
                return false;
            }
        });
        //Aracın ışıklarını switch edebilmesi için gereken butonun listenerı tanımlanıyor
        btnSwitchLights.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {//eğer butona basılıyorsa
                    lightsIsOn = !lightsIsOn;//lightsIsOn değişkeni not operatorü ile switch ediliyor
                    if(lightsIsOn)//ışıklar açıldı ise
                    btnSwitchLights.setImageResource(R.drawable.lightofficon);//ikon ışığı kapatacak şekilde değişiyor
                    else//değilse
                        btnSwitchLights.setImageResource(R.drawable.lightonicon);//ikon ışığı açacak şekilde değişiyor
                    switchLights();//ışığı switch etmek için gereken method çağrılıyor
                }
                return false;
            }
        });
        //İleri,geri, sağ ve sol yönlerini belirleyecek butonların listenerları tanımlanıyor

        f.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {//Forward butonu listener'ı
                if(event.getAction()==MotionEvent.ACTION_DOWN) {//butona basılıyorsa
                    handleCmd(F, IR);//yeni state F yani ileri eski state ise irrelevant olarak atanıyor
                    f.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));//buton rengi değişiyor
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {//butondan parmak kalktıysa
                        handleCmd(S, F);//yeni state durgun yani S eski state ise F olarak atanıyor
                        f.setColorFilter(null);//buton rengi eski haline dönüyor
                    }
                }
                return false;
            }
        });

        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {//backward butonu listener'ı
                if(event.getAction()==MotionEvent.ACTION_DOWN) {//butona basılıyorsa
                    handleCmd(B, IR);//yeni state B yani geri eski state ise irrelevant olarak atanıyor
                    b.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        handleCmd(S, B);//yeni state durgun yani S eski state ise B olarak atanıyor
                        b.setColorFilter(null);
                    }
                }
                return false;
            }
        });
        //İlk iki F ve B butonları için yaptığımız açıklamalar aşağıdaki left right listener'ları içinde geçerlidir
        l.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    handleCmd(L, IR);
                    l.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        handleCmd(S, L);
                        l.setColorFilter(null);
                    }
                }
                return false;
            }
        });

        r.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    handleCmd(Ri, IR);
                    r.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
                } else {
                    if(event.getAction()==MotionEvent.ACTION_UP) {
                        handleCmd(S, Ri);
                        r.setColorFilter(null);
                    }
                }
                return false;
            }
        });


//Connect butonunun listener'ı tanımlanıyor
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (!bt.isConnected()) {//eğer bağlantı sağlanamamışsa
                t.setText("Retrying...");//ekrandaki textimiz bağlanana kadar Retrying olarak değiştiriliyor
                // bağlandığında bt thread'inde Conected olarak güncellenecek
                disconnect = false;//bağlanırken disconnected false ediliyor
                bt.setMmDevice(chosenDevice);//seçili cihaz bt'ye BluetoothDevice olarak set ediliyor
                mRemoteService = new Thread(bt);//Thread'e yeni cihazla tekrar atama yapılıyor
                mRemoteService.start();//bt threadi başlatılıyor
            } else {
                t.setText("Already connected");//şayet bağlı ise zaten bağlı şeklinde text set ediliyor
            }
            }
        });
        mRemoteService.start();//setup chat'in başlangıcında atanan thread başlatılıyor
    }

    void changeSpeed()//hızı değiştiren method
    {
            handleCmd(curState[0],IR);//hız değiştikten sonra arduino tarafındaki state'e tekrar veri göndermemiz gerektiğinden
            //handleCmd current state ile tekrar çağrılıyor


    }
    void playHorn(){//korna çalan method

        try {
            queue.put("V");//kuyruğa V keywordü eklenerek korna çalacak state arduinoya gönderiliyor
            //hata yönetimi yapmamızın sebebi kuyruğun multithread çalışmasından dolayı threadin kesilmesi durumuna karşı
            //fırlatılacak Interrupt exception'u handle etmek
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void switchLights(){//ışıkları switch eden method

        try {
            queue.put("W");//kuyruğa W keywordü eklenerek ışıkları switch edecek state arduinoya gönderiliyor
            //hata yönetimi yapmamızın sebebi kuyruğun multithread çalışmasından dolayı threadin kesilmesi durumuna karşı
            //fırlatılacak Interrupt exception'u handle etmek
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //State'leri yönettiğimiz method
    void handleCmd(int newState, int oldState) {

        //Gelen newState'i switch içerisinde kontrol ediyoruz
        switch (newState) {
            case S://gelen newState durdurma komutu ise
                switch (oldState) {//old state'i kontrol ederek
                    case F:
                    case B:
                        curState[0] = newState;//ileri geri doğrultusunda ise curState'in 0. elemanına durdurma komutu veriyoruz
                        break;
                    case L:
                    case Ri:
                        curState[1] = newState;//Sağ sol doğrultusunda ise curState'in 1. elemanına durdurma komutu veriyoruz
                        break;
                }
                break;
            case F:
            case B://Gelen komut F veya B ise yani ileri geri yönünde ise direkt olarak 0. elemana
                curState[0] = newState;//newState'i atıyoruz
                break;
            case L:
            case Ri://Gelen komut L veya Ri ise yani Sağ Sol yönünde ise direkt olarak 1. elemana
                curState[1] = newState;//newState'i atıyoruz
                break;
        }

        try {
            //Bu blokta curState atanan verileri artık BluetoothTerminaline gönderilmek üzere kuyruğa ekliyoruz
            if(curState[0] == S) {//eğer ileri-geri doğrultusu durgunsa
                switch(curState[1]) {//sağ sol rotasyonunu kontrol ediyoruz
                    case S://eğer sağ sol da durgunsa
                        t.setText("Staying Still");//ekrana şuan durduğunu belirten texti bastırıyoruz
                        queue.put("S");//kuyruğa S put ediyoruz yani arduino'ya S, durdurma komutunu gönderiyoruz
                        break;
                    case L://eğer L yani sol geldiyse
                        t.setText("Still Left");//ekrana sola doğru direction verildiğini bastırıyoruz
                        queue.put("L");//kuyruğa L put ediyoruz yani arduino'ya L, direksiyonu sola döndürme komutu gönderiyoruz
                        break;
                    case Ri:
                        t.setText("Still Right");//ekrana sağa doğru direction verildiğini bastırıyoruz
                        queue.put("R");//kuyruğa R put ediyoruz yani arduino'ya R, direksiyonu sağa döndürme komutu gönderiyoruz
                        break;
                }
            } else {
                if (curState[0] == F) {//eğer curState ilk eleman ileri ise
                    switch(curState[1]) {//sağ sol rotasyonunu kontrol ediyoruz
                        case S:
                            t.setText("Moving Forward");//ekrana ileri gidildiğini bastırıyoruz.
                            queue.put(speedStr);//ileri yönde bir hız olacağı için o anki hızımız ne seçildi ise onu da kuyruğa put ediyoruz
                            queue.put("F");//kuyruğa F put ediyoruz yani arduino'ya F, ileri yönde motorları çalıştır komutu gönderiyoruz
                            break;
                        case L:
                            t.setText("Forward Left");//ekrana ileri yönde sola gidildiğini bastırıyoruz.
                            queue.put("G");;//kuyruğa G put ediyoruz yani arduino'ya G, ileri yönde;sola doğru gidecek şekilde
                            // motorları çalıştır komutu gönderiyoruz
                            break;
                        case Ri:
                            t.setText("Forward Right");//ekrana ileri yönde sağa gidildiğini bastırıyoruz.
                            queue.put("I");//kuyruğa I put ediyoruz yani arduino'ya I, ileri yönde;sağa doğru gidecek şekilde
                            // motorları çalıştır komutu gönderiyoruz
                            break;
                    }
                } else {
                    if (curState[0] == B) {//eğer curState ilk eleman geri ise
                        switch(curState[1]) {//sağ sol rotasyonunu kontrol ediyoruz
                            case S:
                                t.setText("Moving Backward");//ekrana geri gidildiğini bastırıyoruz.
                                queue.put(speedStr);//geri yönde bir hız olacağı için o anki hızımız ne seçildi ise onu da kuyruğa put ediyoruz
                                queue.put("B");//kuyruğa B put ediyoruz yani arduino'ya B, geri yönde motorları çalıştır komutu gönderiyoruz
                                break;
                            case L:
                                t.setText("Backward Left");//ekrana geri yönde sola gidildiğini bastırıyoruz.
                                queue.put("H");//kuyruğa H put ediyoruz yani arduino'ya H, geri yönde;sola doğru gidecek şekilde
                                // motorları çalıştır komutu gönderiyoruz
                                break;
                            case Ri:
                                t.setText("Backward Right");
                                queue.put("J");//kuyruğa J put ediyoruz yani arduino'ya J, geri yönde;sağa doğru gidecek şekilde
                                // motorları çalıştır komutu gönderiyoruz
                                break;
                        }
                    }
                }
            }

            /*
            * Diğer hata yönetimi bloklarında da bahsettiğimiz üzere BlockedQueue yapısını multithreadingte
            * kullandığımızdan olası bir Interrupt durumuna karşı fırlatılacak exception'ı handle etmek zorunda olduğumuzdan
            * try/catch yapısını kullanıyoruz
            * */
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
