import sys
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.metrics import root_mean_squared_error
import math
import numpy as np

csv_cleaned = sys.argv[1]
target_col = sys.argv[2]
fold = int(sys.argv[3])
K = int(sys.argv[4])

df = pd.read_csv(csv_cleaned)
n = len(df)

splits = np.array_split(df, K)
test_df = splits[fold]
train_df = pd.concat([s for i, s in enumerate(splits) if i != fold])

X_train = train_df.drop(columns=[target_col])
y_train = train_df[target_col]
X_test = test_df.drop(columns=[target_col])
y_test = test_df[target_col]

model = LinearRegression()
model.fit(X_train, y_train)

y_pred = model.predict(X_test)
rmse = root_mean_squared_error(y_test, y_pred)

print(rmse)
