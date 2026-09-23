import os,joblib,pandas as pd
from flask import Flask,request,jsonify
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import precision_score,recall_score,f1_score

app=Flask(__name__)
DATA_PATH=os.environ.get("DATA_PATH","/data/wine.csv")
MODEL_PATH=os.environ.get("MODEL_PATH","/data/model.joblib")

@app.get("/health")
def health():
    return {"status":"ok"}

@app.post("/train")
def train():
    params=request.get_json() or {}
    target=params.get("target")
    if not target:return jsonify({"error":"target parametar je obavezan"}),400

    test_size=float(params.get("test_size",0.2))
    hidden_layers=tuple(params.get("hidden_layer_sizes",[100,50]))
    max_iter=int(params.get("max_iter",1000))
    random_state=int(params.get("random_state",42))

    df=pd.read_csv(DATA_PATH).dropna()
    if target not in df.columns:return jsonify({"error":f"Kolona '{target}' ne postoji."}),400

    X=df.drop(columns=[target])
    y=df[target]

    X_train,X_test,y_train,y_test=train_test_split(X,y,test_size=test_size,random_state=random_state,stratify=y)

    model=Pipeline([
        ("scaler",StandardScaler()),
        ("classifier",MLPClassifier(hidden_layer_sizes=hidden_layers,max_iter=max_iter,random_state=random_state))
    ])

    model.fit(X_train,y_train)
    predictions=model.predict(X_test)

    precision=precision_score(y_test,predictions,average="weighted",zero_division=0)
    recall=recall_score(y_test,predictions,average="weighted",zero_division=0)
    f1=f1_score(y_test,predictions,average="weighted",zero_division=0)

    joblib.dump({"model":model,"features":list(X.columns),"target":target},MODEL_PATH)

    return jsonify({"precision":precision,"recall":recall,"f1_score":f1})

@app.post("/predict")
def predict():
    if not os.path.exists(MODEL_PATH):return jsonify({"error":"Model nije treniran."}),400

    saved=joblib.load(MODEL_PATH)
    model=saved["model"]
    feature_names=saved["features"]

    body=request.get_json() or {}
    values=body.get("features")
    if not values:return jsonify({"error":"Polje 'features' je obavezno."}),400

    missing=[name for name in feature_names if name not in values]
    if missing:return jsonify({"error":"Nedostaju feature-i.","missing":missing}),400

    row=pd.DataFrame([[values[name] for name in feature_names]],columns=feature_names)
    prediction=model.predict(row)[0]

    if hasattr(prediction,"item"):prediction=prediction.item()

    return jsonify({"prediction":prediction})

if __name__=="__main__":
    app.run(host="0.0.0.0",port=5000)