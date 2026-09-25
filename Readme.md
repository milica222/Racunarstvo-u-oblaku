# Prvi domaći

Komande za pokretanje aplikacije pomoću Docker-a:

```bash
docker build -t fastapi-example .
docker run -d -p 8000:8000 fastapi-example
```

Nakon što izvršite komande navedene iznad, aplikaciji možete pristupiti na sledećim linkovima:

1. http://localhost:8000
2. http://localhost:8000/docs 

# Drugi domaći

Komande za pokretanje aplikacije pomoću Docker-a:

```bash
docker-compose up
```

Nakon što izvršite komande navedene iznad, aplikaciji možete pristupiti na sledećim linkovima:

1. http://localhost:8000
2. http://localhost:8000/docs 
# Treći domaći (Osnove CWL)

Komande za pokretanje CWL workflow-a:

```bash
cd Osnove-CWL

docker build -t milicavasovic222/osnove-cwl .
docker push milicavasovic222/osnove-cwl

pip install cwltool

cwltool --outdir ./results workflow.cwl inputs.yaml
```

Workflow ima dva koraka: `processing_data` čisti `HousingData.csv` (izbacuje outlier-e i popunjava prazna polja prosekom kolone), a `training` taj skup deli na train i test (80/20, kolona `MEDV` je target), trenira linearnu regresiju i upisuje RMSE u `metrics.txt`.

Ako ne želite da se koristi Docker image, workflow se može pokrenuti i lokalno, pod uslovom da su `pandas` i `scikit-learn` instalirani:

```bash
cwltool --no-container --outdir ./results workflow.cwl inputs.yaml
```

Rezultati se upisuju u folder `Osnove-CWL/results`.

# Četvrti domaći (CWL Scatter)

Komande za pokretanje CWL workflow-a:

```bash
cd CWL-Scatter

docker build -t milicavasovic222/cwl-scatter .
docker push milicavasovic222/cwl-scatter

pip install cwltool

cwltool --outdir ./results workflow.cwl inputs.yaml
```

Za razliku od trećeg domaćeg, ovde se korak treniranja pokreće pomoću `scatter`-a — jednom po foldu (broj foldova se podešava poljem `folds` u `inputs.yaml`, podrazumevano 3). Korak `calculate_avg_rmse` na kraju računa prosečan RMSE svih foldova.

Pokretanje bez Docker image-a (potrebni `pandas`, `scikit-learn` i `numpy`):

```bash
cwltool --no-container --outdir ./results workflow.cwl inputs.yaml
```

Rezultati se upisuju u folder `CWL-Scatter/results`.

# Peti domaći (Kubernetes Job)

Komanda za pokretanje unakrsne validacije na Kubernetes klasteru:

```bash
cd Kubernetes-Jobs
bash scripts/run.sh
```

Skripta radi sledeće:

1. Iz `k8s/configmap.yaml` čita broj foldova (`K_FOLDS`, podrazumevano 5).
2. Pravi ConfigMap-ove sa kodom (`cv-code`) i podacima (`cv-data`).
3. Pokreće Job `cross-validation` u `Indexed` režimu, gde se svi foldovi izvršavaju paralelno — svaki u svom Pod-u.
4. Čeka da svi foldovi završe, skuplja rezultate iz logova Pod-ova u `results/folds.jsonl`.
5. Pokreće `scripts/aggregate_results.py`, koji ispisuje metrike po foldovima i konačan prosek (MAE, RMSE, R²) i upisuje ih u `results/final_results.json`.

Pre pokretanja, u `scripts/run.sh` podesite promenljive `NAMESPACE` i `KUBECTL` prema svom klasteru:

```bash
NAMESPACE="milicav"
KUBECTL="microk8s.kubectl"
```

Broj foldova se menja u `k8s/configmap.yaml`:

```yaml
K_FOLDS: "5"
```

# Šesti domaći (Kubernetes ML)

Komande za build i deploy REST API-ja na Kubernetes klaster:

```bash
cd Kubernetes-ML

docker build -t milicavasovic222/wine-ml-api:latest app
docker push milicavasovic222/wine-ml-api:latest

bash scripts/run.sh
```

Skripta postavlja ConfigMap, PVC, Deployment i Service, sačeka da se Deployment podigne, a zatim kopira `data/wine.csv` u Pod, na deljeni volume `/data`.

Kao i kod petog domaćeg, promenljive `NAMESPACE`, `KUBECTL` i `IMAGE` se podešavaju na vrhu `scripts/run.sh`.

Aplikaciji se pristupa preko NodePort-a `30081`:

```bash
curl http://<NODE_IP>:30081/health

curl -X POST http://<NODE_IP>:30081/train \
  -H "Content-Type: application/json" \
  -d '{"target": "type"}'

curl -X POST http://<NODE_IP>:30081/predict \
  -H "Content-Type: application/json" \
  -d '{
        "features": {
          "fixed acidity": 7.0,
          "volatile acidity": 0.27,
          "citric acid": 0.36,
          "residual sugar": 20.7,
          "chlorides": 0.045,
          "free sulfur dioxide": 45,
          "total sulfur dioxide": 170,
          "density": 1.001,
          "pH": 3.0,
          "sulphates": 0.45,
          "alcohol": 8.8,
          "quality": 6
        }
      }'
```

Poziv `/train` trenira `MLPClassifier` i upisuje model na `/data/model.joblib`, pa ga zato treba pozvati pre `/predict`. Uz `target`, mogu se proslediti i `test_size`, `hidden_layer_sizes`, `max_iter` i `random_state`.

# Sedmi domaći (12 Factor App)

Analiza jedne stvarne aplikacije (chatbot servis) kroz svih 12 faktora nalazi se u fajlu [`12-Factor-App/12-factor-app.md`](12-Factor-App/12-factor-app.md). Za svaki faktor je opisano kako je trenutno rešen i šta bi moglo da se poboljša.

# Osmi domaći (ZooKeeper)

Tri Spring Boot servera se povezuju na ZooKeeper, koji bira lidera među njima. Kada se na bilo koji server pošalje CSV fajl (`PUT /model`), zahtev ide lideru, lider trenira model i šalje ga svim ostalim živim serverima, pa svaki server može da vrati predikciju (`POST /predict`).

Komande za pokretanje:

```bash
cd Zookepeer/zookeeper-demo-master

docker compose up --build
```

Serveri su dostupni na portovima `8081`, `8082` i `8083`, a Swagger na http://localhost:8081/swagger-ui.html.

Primeri za treniranje i predikciju, kao i CSV fajlovi za testiranje (`samples/`), opisani su u [`Zookepeer/zookeeper-demo-master/README.md`](Zookepeer/zookeeper-demo-master/README.md).
