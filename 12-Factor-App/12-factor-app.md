# 12 Factor App na primeru chatbot servisa

Primer aplikacije je ChatBot servis, odnosno REST API u Flask-u koji prima poruke korisnika i vraća odgovore. Istoriju razgovora čuva u PostgreSQL bazi, aktivne sesije u Redis-u, a odgovore dobija pozivanjem eksternog LLM API-ja. Servis se pakuje u Docker image i pokreće na Kubernetes klasteru u više instanci.

## Faktor 1: Codebase

Ceo chatbot servis stoji u jednom Git repozitorijumu iz kojeg nastaju sva okruženja, to jest razvojno, test i produkciono. Isti commit se raspoređuje na sve instance, pa se okruženja razlikuju samo po konfiguraciji, a nikako po kodu. Ako bi se kasnije dodao servis za analitiku razgovora, on bi dobio svoj repozitorijum jer je reč o zasebnoj aplikaciji.

## Faktor 2: Dependencies

Sve biblioteke su izričito navedene u requirements.txt, gde stoje flask, psycopg2, redis i klijent za LLM API. Aplikacija se nikada ne oslanja na pakete koji slučajno postoje na sistemu, pa Dockerfile instalira tačno ono što je deklarisano. Novi član tima klonira repozitorijum, pokrene instalaciju zavisnosti i dobije isto okruženje kao i svi ostali.

## Faktor 3: Config

API ključ za LLM, adresa baze i adresa Redis-a stoje u promenljivama okruženja, a ne u kodu. Isti image se zbog toga pokreće i lokalno i u produkciji, gde vrednosti stižu iz ConfigMap-a i Secret-a. Kada bi ključ bio upisan u kod, svaka izmena bi tražila novi build, uz stalan rizik da tajna završi u repozitorijumu.

## Faktor 4: Backing services

PostgreSQL, Redis i LLM API su za chatbot obični resursi kojima pristupa preko adrese iz konfiguracije. Zamena lokalne baze upravljanom bazom u oblaku svodi se na promenu jedne promenljive, bez ijedne izmene u kodu. Isto važi i za jezički model, odnosno prelazak na drugog provajdera menja konfiguraciju i klijentski adapter, dok logika razgovora ostaje ista.

## Faktor 5: Izgradnja, objavljivanje, izvršavanje (Build, release, run)

Build faza od koda i zavisnosti pravi Docker image, release faza tom image-u pridružuje konfiguraciju okruženja, a run faza pokreće kontejnere. Svaki release nosi svoju oznaku verzije, pa je povratak na prethodno stanje samo vraćanje na stariji tag. Kod se nikada ne menja u toku rada, jer bi takva izmena nestala pri sledećem pokretanju kontejnera.

## Faktor 6: Procesi (Processes)

Svaka instanca chatbota radi bez stanja, to jest istorija razgovora i podaci o sesiji čuvaju se u bazi i Redis-u, a ne u memoriji procesa. Zahvaljujući tome dva uzastopna pitanja istog korisnika mogu da završe na različitim instancama, a odgovor će biti jednak. Sve što se upiše u lokalni fajl sistem smatra se privremenim jer nestaje kada se kontejner ugasi.

## Faktor 7: Povezivanje preko porta (Port binding)

Servis sam diže HTTP server i osluškuje na portu koji dobija iz promenljive okruženja, pa mu nije potreban spoljni aplikacioni server. U kontejneru se izlaže port 5000, a Kubernetes Service ga dalje objavljuje ostatku klastera. Na taj način i sam chatbot može da bude prateći servis nekoj drugoj aplikaciji, koja ga poziva preko njegove adrese.

## Faktor 8: Konkurentnost (Concurrency)

Veći broj poruka rešava se dodavanjem instanci, odnosno povećanjem broja replika, umesto kupovinom jačeg servera. Web proces opslužuje HTTP zahteve, dok poseban worker proces radi sporije poslove poput indeksiranja razgovora i slanja izveštaja. Pošto procesi nemaju stanje, load balancer slobodno šalje zahtev bilo kojoj instanci.

## Faktor 9: Jednokratnost (Disposability)

Instanca se podiže za nekoliko sekundi i odmah je spremna da prima saobraćaj, što je važno kada broj korisnika naglo poraste. Na signal za gašenje servis prestaje da prima nove zahteve, završi one koji su u toku i tek onda se ugasi, pa nijedna poruka ne ostane bez odgovora. Ako proces ipak padne usred obrade, posao ostaje u redu i biće ponovo preuzet.

## Faktor 10: Podudarnost razvoja i produkcije (Dev/prod parity)

Lokalno se preko docker compose diže ista kombinacija servisa koja radi i u produkciji, to jest PostgreSQL i Redis istih verzija. Tako se izbegava situacija da nešto radi na laptopu, a puca na klasteru zbog druge verzije baze. Razmak u vremenu je takođe mali, jer se izmene objavljuju često, po nekoliko puta nedeljno.

## Faktor 11: Logovi (Logs)

Chatbot ne piše logove u fajlove, nego ih šalje na standardni izlaz, odakle ih preuzima platforma. U Kubernetes-u se čitaju komandom kubectl logs, a odatle se prosleđuju alatu za pretragu i analizu. Zahvaljujući tome ceo jedan razgovor može da se isprati kroz sve instance koristeći identifikator sesije.

## Faktor 12: Administrativni procesi (Admin processes)

Migracija baze, čišćenje starih razgovora i probno slanje poruke pokreću se kao jednokratni procesi iz istog image-a i sa istom konfiguracijom kao i sam servis. U Kubernetes-u je to Job ili izvršavanje komande u već pokrenutom pod-u, gde skripta stoji u repozitorijumu zajedno sa kodom. Na taj način administrativni zadatak uvek radi nad istom verzijom koda koja je trenutno u produkciji.
