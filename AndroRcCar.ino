
 #include <Servo.h>
//L293 Motor Sürücü Pinleri
  const int motorA1  = 5;  // Arduino'nun 5. pini L293'ün 2. pinine denk gelecek
  const int motorA2  = 6;  // Arduino'nun 6. pini L293'ün 7. pinine denk gelecek
  const int motorB1= 10; // Arduino'nun 10. pini L293'ün 10. pinine denk gelecek
  const int motorB2  = 9;  // Arduino'nun 9. pini L293'ün 14. pinine denk gelecek
//Arabanın ışıkları arduinonun 12. pininde bağlı olacak
  const int lights  = 12;
//Korna için buzzer 3. pine bağlanacak
  const int buzzer = 3 ;   
//HC-06 Bluetooth modülünden gelen serial State verisini 2. pinden okuyacağız
  const int BTState = 2;

  int i=0;//Işığın durumunu değiştirmek için kullanacağımız değişken
  int j=0;//kornanın durumunu değiştirmek için kullanacağımız değişken
  int state;//seri porttan okuduğumuz değeri atayacağımız değişken
  int vSpeed=200;     // arabanın varsayılan hızı ayrıca bu değişken pwm kontrollü pinlere gerilim uygulamak için kullanılacağından 0 ila 255 değeri arasına olmalıdır

void setup() {
    // Motor ve ışık pinlerini çıkış, Bluetooth modülünün state ucunu giriş pini olarak atadık:
    pinMode(motorA1, OUTPUT);
    pinMode(motorA2, OUTPUT);
    pinMode(motorB1, OUTPUT);
    pinMode(motorB2, OUTPUT);
    pinMode(lights, OUTPUT); 
    pinMode(BTState, INPUT);    
    // Seri haberleşmenin baudrate yani saniyede kaç bit okuyacağını belirleyerek seri haberleşmeyi başlattık:
    Serial.begin(9600);
}
 
void loop() {
  //Bağlantı kesildiğinde ya da bluetooth bağlantısı zayıfladığında aracın stabil çalışması için stateti S olarak atıyoruz yani aracı durduruyoruz
     if(digitalRead(BTState)==LOW) { state='S'; }

  //Serialden okuduğumuz veriyi state değişkenine atadık
    if(Serial.available() > 0){     
      state = Serial.read();            
    }
  
  //Serialden hızı ayarlamak için state değişkeninin durumun kontrol ederek default değeri 200 olan vSpeed'i değiştiriyoruz
     if (state == '2'){
      vSpeed=120;}
    else if (state == '3'){
      vSpeed=160;}
    else if (state == '4'){
      vSpeed=200;}
 	  
  /***********************İleri****************************/
  //eğer Bluetooth tarafından gelen veri F olursa aracın ileri gitmesi için gereken pinlere voltaj veriliyor; vSpeed ile pwm pinlerinden motorlara kontrollü gerilim uygulanıyor
    if (state == 'F') {
    	analogWrite(motorA1, vSpeed); analogWrite(motorA2, 0);
        analogWrite(motorB1, 0);      analogWrite(motorB2, 0); 
    }
  /**********************Sola doğru ileri************************/
    //eğer Bluetooth tarafından gelen veri G olursa aracın ileri doğru sola gitmesi için gereken pinlere voltaj veriliyor.

    else if (state == 'G') {
    	analogWrite(motorA1, vSpeed); analogWrite(motorA2, 0);  
        analogWrite(motorB1, 200);    analogWrite(motorB2, 0); 
    }
  /**********************Sağa doğru ileri************************/
//eğer Bluetooth tarafından gelen veri I olursa aracın ileri doğru sağa gitmesi için gereken pinlere voltaj veriliyor.
   else if (state == 'I') {
      	analogWrite(motorA1, vSpeed); analogWrite(motorA2, 0); 
        analogWrite(motorB1, 0);      analogWrite(motorB2, 200); 
    }
  /***********************Geri****************************/
  //eğer Bluetooth tarafından gelen veri B olursa aracın Geri gitmesi için gereken pinlere voltaj veriliyor;
    else if (state == 'B') {
    	analogWrite(motorA1, 0);   analogWrite(motorA2, vSpeed); 
        analogWrite(motorB1, 0);   analogWrite(motorB2, 0); 
    }
  /**********************Sola doğru geri************************/
    //eğer Bluetooth tarafından gelen veri H olursa aracın sola doğru geriye gitmesi için gereken pinlere voltaj veriliyor.
    else if (state == 'H') {
    	analogWrite(motorA1, 0);   analogWrite(motorA2, vSpeed); 
        analogWrite(motorB1, 200); analogWrite(motorB2, 0); 
    }
  /**********************Sağa doğru geri************************/
    //eğer Bluetooth tarafından gelen veri J olursa aracın sağa doğru geriye gitmesi için gereken pinlere voltaj veriliyor.
    else if (state == 'J') {
    	analogWrite(motorA1, 0);   analogWrite(motorA2, vSpeed); 
        analogWrite(motorB1, 0);   analogWrite(motorB2, 200); 
    }
  /***************************Sol*****************************/
    //eğer Bluetooth tarafından gelen veri L olursa araç dururken tekerleri sola yönlendirmek için gereken pinlere voltaj veriliyor
    else if (state == 'L') {
    	analogWrite(motorA1, 0);   analogWrite(motorA2, 0); 
        analogWrite(motorB1, 200); analogWrite(motorB2, 0); 
    }
  /***************************Sağ*****************************/
    //eğer Bluetooth tarafından gelen veri R olursa araç dururken tekerleri sağa yönlendirmek için gereken pinlere voltaj veriliyor
    else if (state == 'R') {
    	analogWrite(motorA1, 0);   analogWrite(motorA2, 0); 
        analogWrite(motorB1, 0);   analogWrite(motorB2, 200); 		
    }
  /************************Işıklar*****************************/
    //eğer Bluetooth tarafından gelen veri W olursa aracın ışıkları kapalıysa açılacak açık ise kapanacak şekilde voltaj veriliyor.
    else if (state == 'W') {
      if (i==0){  //ışığın kapalı olduğunu algılıyoruz
         digitalWrite(lights, HIGH); 
         i=1;
      }
      else if (i==1){//ışığın açık olduğunu algılıyoruz
         digitalWrite(lights, LOW); 
         i=0;
      }
      state='n';//state'i değiştiriyoruz zira serialden gelen son veri W olduğu için sürekli ışıkları açıp kapatacak bir döngüye girme ihtimali var
    }
  /**********************Korna sesi***************************/
    //eğer Bluetooth tarafından gelen veri V olursa aracın kornası kapalıysa açılacak açık ise kapanacak şekilde voltaj veriliyor.
    else if (state == 'V'){
      if (j==0){  
         tone(buzzer, 1000);//Buzzer'a ses veriyoruz
         j=1;
      }
      else if (j==1){
         noTone(buzzer);    //Buzzer'i kapatıyoruz
         j=0;
      }
      state='n';  //state'i değiştiriyoruz zira serialden gelen son veri V olduğu için sürekli kornayı açıp kapatacak bir döngüye girme ihtimali var
    }
  /************************Durdur*****************************/
    //eğer Bluetooth tarafından gelen veri S olursa aracı durduracak şekilde tüm motor pinlerinin voltajını kesiyoruz.
    else if (state == 'S'){
        analogWrite(motorA1, 0);  analogWrite(motorA2, 0); 
        analogWrite(motorB1, 0);  analogWrite(motorB2, 0);
    }
    
}
