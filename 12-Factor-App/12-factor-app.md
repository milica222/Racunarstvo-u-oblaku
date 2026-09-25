# 12 Factor App na primeru chatbot servisa

Primer je chatbot aplikacija koju čine React frontend, Python backend i middleware sloj, a korisniku omogućava razgovor, povezivanje na MCP servere i obradu dokumenata. Podaci same aplikacije čuvaju se u PostgreSQL bazi, istorija poruka u Cosmos DB-u, dok obrada dokumenata radi kao zaseban servis sa svojim Dockerfile-om. Aplikacija je podignuta u tri okruženja, odnosno dev, stejdž i produkciju, a izmene do njih stižu kroz CI/CD pipeline.

## Faktor 1: Codebase

Ceo projekat stoji u jednom Git repozitorijumu u kojem su zajedno frontend, backend i middleware, pa se sve tri celine verzionišu na istom mestu. Iz te iste kodne baze podižu se tri instance aplikacije, odnosno razvojna, stejdž i produkciona. Instance se razlikuju isključivo po konfiguraciji, dok je kod svuda isti, pa ono što je provereno na stejdžu radi jednako i u produkciji.

## Faktor 2: Dependencies

Zavisnosti backend-a navedene su u requirements.txt, dok se paketi React frontend-a vode u package.json fajlu. Instalacija se izvršava u Dockerfile-u komandom pip install, pa image sadrži tačno ono što je deklarisano i ne oslanja se na pakete zatečene na serveru. Zbog toga novi član tima klonira repozitorijum, pokrene build i dobije isto okruženje kao i svi ostali. Lock fajlovi se zasad ne koriste, odnosno verzije nisu do kraja zaključane, što je prva stvar koju bi vredelo dodati da bi svaki build bio potpuno identičan.

## Faktor 3: Config

Podešavanja koja se razlikuju po okruženjima stoje u konfiguracionom fajlu koji je deo repozitorijuma, dok tajne vrednosti, odnosno lozinke i API ključevi, idu u poseban secrets fajl koji se ne komituje. Zahvaljujući tome nijedna tajna ne završava u Git istoriji. Secrets fajlovi se čuvaju na Rancher-u i kače se na instancu kao fajl, pa svako okruženje dobija svoje vrednosti, a aplikacija ih pročita kad se pokrene. Faktor kaže da konfiguracija treba da ide kroz promenljive okruženja, a ne kroz fajlove. Na ovaj nacin, tajne nisu u kodu i menjaju se bez novog build-a. Ako bi se išlo do kraja, iste vrednosti bi mogle da se proslede kao env promenljive, a i običan konfiguracioni fajl bi mogao da pređe na Rancher umesto da stoji u repozitorijumu.

## Faktor 4: Backing services

Aplikacija koristi dve baze, odnosno PostgreSQL za podatke same aplikacije i Cosmos DB za istoriju poruka i slične zapise. Obe su za aplikaciju samo prikačeni resursi, jer joj adrese i kredencijali stižu iz secrets fajla, pa se zamena instance svodi na promenu tih vrednosti bez ijedne izmene u kodu. Lokalni razvoj radi nad dev instancom baze, dok stejdž i produkcija koriste produkcionu bazu. Razdvajanje bi bilo čistije kada bi i stejdž dobio svoju instancu, jer bi tada testiranje bilo potpuno odvojeno od stvarnih podataka.

## Faktor 5: Build, release, run

Build i objavljivanje idu kroz podešen CI/CD pipeline, koji nad svakom izmenom prvo pokrene testove i tek ako oni prođu nastavi ka release-u. Svaki release nosi svoj broj, pa se tačno zna koja verzija koda radi na kom okruženju, a povratak na prethodno stanje svodi se na objavljivanje starijeg broja. Kod se nikad ne menja ručno direktno na serveru, jer bi se ta izmena izgubila na sledećem deploy-u, a i na serveru bi radio kod kog nema u repozitorijumu.

## Faktor 6: Processes

Svaka instanca aplikacije radi nezavisno i ne drži razgovor u svojoj memoriji, nego se svaki chat upisuje u bazu i odatle ponovo čita. Zahvaljujući tome dva uzastopna pitanja istog korisnika može da opsluži bilo koja instanca, jer sve gledaju u iste podatke u Postgres-u i Cosmos DB-u. Sve što bi se upisalo u lokalni fajl sistem kontejnera smatra se privremenim, jer nestaje čim se instanca ugasi ili bude zamenjena novom. 

## Faktor 7: Povezivanje preko porta (Port binding)

Backend se pokreće komandom python main.py i sam diže svoj HTTP server, pa mu nije potreban spoljni aplikacioni server ispred sebe. Lokalno osluškuje na portu 7000, dok podignute instance imaju svoj domen preko kojeg su dostupne. Frontend u svojoj konfiguraciji drži podatak o tome koju instancu backend-a poziva, odnosno lokalnu, dev, stejdž ili produkcionu, pa se prebacivanje svodi na promenu te vrednosti.

## Faktor 8: Konkurentnost (Concurrency)

Backend opslužuje zahteve sa tri radna toka, odnosno tri workera unutar servisa, pa se veći broj korisnika prihvata paralelno umesto da sve ide jedan za drugim. Obrada dokumenata je izdvojena u zaseban servis sa svojim Dockerfile-om i radi nezavisno, tako da spori poslovi ne usporavaju odgovaranje na poruke. Kada saobraćaj poraste, kapacitet se podiže dodavanjem radnih procesa i instanci, a ne prelaskom na jači server. Pošto instance ne čuvaju stanje razgovora, zahtev slobodno može da završi na bilo kojoj od njih.

## Faktor 9: Jednokratnost (Disposability)

Podizanje instance traje oko pet do deset minuta, a i posle toga je potrebno još vremena da se učitaju podaci za MCP servere, pa korisnik koji ih pozove prerano dobija grešku dok se sve ne podesi. To je najveće odstupanje od ovog faktora, jer 12 faktora traži da se instanca digne brzo kako bi restart i skaliranje bili bezbolni. Dobra strana je što posao nije izgubljen kada obrada dokumenta pukne, odnosno korisnik dobija grešku i mogućnost da pokuša ponovo. Sledeći korak bio bi skraćivanje starta i uvođenje provere spremnosti, tako da instanca počne da prima zahteve tek kada su podaci za MCP učitani.

## Faktor 10: Podudarnost razvoja i produkcije (Dev/prod parity)

Ovaj faktor traži da okruženje u kojem se kod piše i testira bude što sličnije produkciji, da ne bi bilo iznenađenja kad izmena stigne do korisnika. Kod nas postoji dev instanca aplikacije koja se sama ažurira čim se kod push-uje na dev granu, pa se izmena posle par minuta može isprobati kroz browser, bez pokretanja bilo čega lokalno. Dev instanca radi na isti način kao stejdž i produkcija, samo nad svojom, dev bazom, pa se već tu vidi kako će se izmena ponašati. Nova verzija ide u produkciju otprilike na svake dve nedelje, tako da ne prođe mnogo vremena od pisanja koda do trenutka kad ga korisnici koriste. Jedina veća razlika je što stejdž i produkcija koriste istu bazu, pa bi bilo bolje da i stejdž ima svoju.

## Faktor 11: Logovi (Logs)

Logovi se ne čuvaju u fajlovima na serveru, nego se skupljaju u Elastic-u, gde se beleži svaki poziv ka aplikaciji i svaki API poziv zajedno sa rezultatom ili stack trace-om greške. Kada se nešto istražuje, pretražuju se najskoriji logovi po identifikatoru korisnika, pa se ceo tok jednog zahteva vidi na jednom mestu i bez ulaska na server. Aplikacija se tako prema logovima ponaša kao prema toku događaja koji samo ispiše, dok se skupljanje, čuvanje i pretraga rešavaju izvan nje.

## Faktor 12: Administrativni procesi (Admin processes)

Za svaku izmenu baze pravi se migraciona skripta sa SQL upitima, koja stoji u repozitorijumu zajedno sa kodom koji tu izmenu koristi. Sve migracione skripte se pokreću pre podizanja nove verzije na produkciju, pa su baza i kod uvek usklađeni. Pošto migracije idu iz istog repozitorijuma i kroz isti pipeline kao i sam release, administrativni posao se izvršava nad istom verzijom koda koja se objavljuje.
