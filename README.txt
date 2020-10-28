Obsah
	1. Štruktúra adresára
	2. Aplikácia
		2a. Inštalácia do zariadenia
		2b. Import do Android Studio pre vıvoj
	3. Server


	
1. Štruktúra adresára
	
	doc         (textová èas v zdrojovom formáte a v PDF)
	executables (inštalaènı súbor pre aplikáciu)
	other       (video o aplikácii, ktoré je súèasou zadania)
	src         (zdrojovı kód pre aplikáciu aj server)

	
2. Aplikácia

	2a. Inštalácia do zariadenia

		Aplikácia nie je podpísaná ako aplikácie v Google Play, preto je potrebné v zariadení povoli inštaláciu z neznámych zdrojov.
		Toto nastavenie sa na kadom zariadení nachádza inde, väèšinou pod oznaèením "Security". Po povolení nastavenia je potrebné
		umiestni súbor AudioRecord.apk do pamäte zariadenia, cez prehliadaè súborov ho v zariadení lokalizova a spusti. 
		pozn. Pri Android verzie 6.0+ pri prvom spustení nebude fungova ukazate¾ úrovne audia, pretoe pri spustení aplikácie nebolo
			  povolenie pre nahrávanie audia, je to oèakávané chovanie.
	  
	2b. Import do Android Studio pre vıvoj
		
		Link pre stiahnutie Android Studio: https://developer.android.com/studio
		Pre spúšanie projektu v Android Studio je potrebné ma virtuálne zariadenie alebo pripojené fyzické zariadenie,
		ktoré musí ma povolené "Developer options" a v nich povolené nastavenia "USB Debugging" a "Install via USB".
		
		1. Pri štarte zvoli monos "Import project (Gradle Eclipse ADT, etc.)"
		2. Zvoli zloku s aplikáciou (src/application)
		3. V hornom menu zvoli Run -> Edit Configuration
		4. V konfigurácii kliknutím na + zvoli vytvorenie novej konfigurácie typu "Android App"
		5. Zvoli modul "app", zada názov konfigurácie napríklad "main" a potvrdi
		6. Build -> Clean Project
		7. Build -> Rebuild Project
		8. Run -> Run "main"
		9. Zvoli pripojené alebo virtuálne zariadenie
		10. Spustenie


3. Server

	Server pre svoju funkciu potrebuje iba PHP 7.2
	
	1. Obsah zloky src/server umiestni na server
	2. Spusti pomocou príkazu sudo php -S 0.0.0.0:80 server.php