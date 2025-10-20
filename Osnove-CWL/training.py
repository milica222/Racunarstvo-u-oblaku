import sys
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression
from sklearn.metrics import root_mean_squared_error

dataset_path = sys.argv[1]
target_col = sys.argv[2]
train_pct = float(sys.argv[3])

df = pd.read_csv(dataset_path)

X = df.drop(columns=[target_col])
y = df[target_col]

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=1 - train_pct)

model = LinearRegression()
model.fit(X_train, y_train)

predictions = model.predict(X_test)
rmse = root_mean_squared_error(y_test, predictions)

print(f"RMSE: {rmse:.2f}")
