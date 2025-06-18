# Prvi domaći

Komande za pokretanje aplikacije pomoću Docker-a:

```bash
docker build -t fastapi-example .
docker run -d -p 8000:8000 fastapi-example
```

Nakon što izvršite komande navedene iznad, aplikaciji možete pristupiti na sledećim linkovima:

1. http://localhost:8000
2. http://localhost:8000/docs 