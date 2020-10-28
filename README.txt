Obsah
	1. �trukt�ra adres�ra
	2. Aplik�cia
		2a. In�tal�cia do zariadenia
		2b. Import do Android Studio pre v�voj
	3. Server


	
1. �trukt�ra adres�ra
	
	doc         (textov� �as� v zdrojovom form�te a v PDF)
	executables (in�tala�n� s�bor pre aplik�ciu)
	other       (video o aplik�cii, ktor� je s��as�ou zadania)
	src         (zdrojov� k�d pre aplik�ciu aj server)

	
2. Aplik�cia

	2a. In�tal�cia do zariadenia

		Aplik�cia nie je podp�san� ako aplik�cie v Google Play, preto je potrebn� v zariaden� povoli� in�tal�ciu z nezn�mych zdrojov.
		Toto nastavenie sa na ka�dom zariaden� nach�dza inde, v��inou pod ozna�en�m "Security". Po povolen� nastavenia je potrebn�
		umiestni� s�bor AudioRecord.apk do pam�te zariadenia, cez prehliada� s�borov ho v zariaden� lokalizova� a spusti�. 
		pozn. Pri Android verzie 6.0+ pri prvom spusten� nebude fungova� ukazate� �rovne audia, preto�e pri spusten� aplik�cie nebolo
			  povolenie pre nahr�vanie audia, je to o�ak�van� chovanie.
	  
	2b. Import do Android Studio pre v�voj
		
		Link pre stiahnutie Android Studio: https://developer.android.com/studio
		Pre sp���anie projektu v Android Studio je potrebn� ma� virtu�lne zariadenie alebo pripojen� fyzick� zariadenie,
		ktor� mus� ma� povolen� "Developer options" a v nich povolen� nastavenia "USB Debugging" a "Install via USB".
		
		1. Pri �tarte zvoli� mo�nos� "Import project (Gradle Eclipse ADT, etc.)"
		2. Zvoli� zlo�ku s aplik�ciou (src/application)
		3. V hornom menu zvoli� Run -> Edit Configuration
		4. V konfigur�cii kliknut�m na + zvoli� vytvorenie novej konfigur�cie typu "Android App"
		5. Zvoli� modul "app", zada� n�zov konfigur�cie napr�klad "main" a potvrdi�
		6. Build -> Clean Project
		7. Build -> Rebuild Project
		8. Run -> Run "main"
		9. Zvoli� pripojen� alebo virtu�lne zariadenie
		10. Spustenie


3. Server

	Server pre svoju funkciu potrebuje iba PHP 7.2
	
	1. Obsah zlo�ky src/server umiestni� na server
	2. Spusti� pomocou pr�kazu sudo php -S 0.0.0.0:80 server.php