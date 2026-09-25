# ZooKeeper demo – distribuirani ML model

## Pokretanje

```powershell
docker compose up --build
```

Serveri su na `localhost:8081`, `8082` i `8083`, a Swagger je na http://localhost:8081/swagger-ui.html.

Gašenje: `docker compose down`.

## Test primeri

Na čvorovima je uvek model koji je poslednji istreniran, pa ga pre predikcije treba istrenirati odgovarajućim CSV-om.

### Iris (`targetColumn=class`)

```powershell
curl.exe -X PUT -F "file=@samples/iris.csv" -F "targetColumn=class" http://localhost:8081/model
```

| JSON za `POST /predict`                                                          | Očekivano         |
|----------------------------------------------------------------------------------|-------------------|
| `{"sepallength": 5.1, "sepalwidth": 3.5, "petallength": 1.4, "petalwidth": 0.2}` | `Iris-setosa`     |
| `{"sepallength": 7.0, "sepalwidth": 3.2, "petallength": 4.7, "petalwidth": 1.4}` | `Iris-versicolor` |
| `{"sepallength": 6.3, "sepalwidth": 3.3, "petallength": 6.0, "petalwidth": 2.5}` | `Iris-virginica`  |

### Stanovi (`targetColumn=cena`)

```powershell
curl.exe -X PUT -F "file=@samples/stanovi.csv" -F "targetColumn=cena" http://localhost:8081/model
```

| JSON za `POST /predict`                                                       | Očekivana cena |
|-------------------------------------------------------------------------------|----------------|
| `{"kvadratura": 52, "brojSoba": 2, "starost": 5, "udaljenostOdCentra": 5.0}`  | ~93.500        |
| `{"kvadratura": 33, "brojSoba": 2, "starost": 18, "udaljenostOdCentra": 7.2}` | ~47.800        |
| `{"kvadratura": 85, "brojSoba": 3, "starost": 10, "udaljenostOdCentra": 2.5}` | ~162.000       |

Predikcija iz PowerShell-a:

```powershell
curl.exe -X POST -H "Content-Type: application/json" -d '{\"kvadratura\":85,\"brojSoba\":3,\"starost\":10,\"udaljenostOdCentra\":2.5}' http://localhost:8082/predict
```
