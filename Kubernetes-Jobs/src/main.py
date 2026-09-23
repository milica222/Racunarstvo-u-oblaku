import json
import os

import numpy as np
import pandas as pd

from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import KFold


data_file = os.environ["DATA_FILE"]

features = [feature.strip() for feature in os.environ["FEATURES"].split(",")]

target = os.environ["TARGET"]

k_folds = int(os.environ["K_FOLDS"])
random_state = int(os.environ["RANDOM_STATE"])

fold_index = int(os.environ["JOB_COMPLETION_INDEX"])

df = pd.read_csv(data_file)
df = df[features + [target]]
df = df.dropna()

X = df[features]
y = df[target]

cv = KFold(
    n_splits=k_folds,
    shuffle=True,
    random_state=random_state
)

folds = list(cv.split(X))

train_indices, test_indices = folds[fold_index]

X_train = X.iloc[train_indices]
X_test = X.iloc[test_indices]
y_train = y.iloc[train_indices]
y_test = y.iloc[test_indices]

model = LinearRegression()
model.fit(X_train, y_train)
predictions = model.predict(X_test)

mae = mean_absolute_error(y_test, predictions)
rmse = np.sqrt(mean_squared_error(y_test, predictions))
r2 = r2_score(y_test, predictions)

result = {
    "fold": fold_index,
    "mae": float(mae),
    "rmse": float(rmse),
    "r2": float(r2)
}

print("RESULT_JSON=" + json.dumps(result))