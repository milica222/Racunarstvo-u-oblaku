import sys
import pandas as pd

df = pd.read_csv(sys.argv[1])

q1 = df.quantile(0.25)
q3 = df.quantile(0.75)
iqr = q3 - q1
mask = ~((df < (q1 - 1.5 * iqr)) | (df > (q3 + 1.5 * iqr))).any(axis=1)
df = df[mask]

df = df.fillna(df.mean())

sys.stdout.write(df.to_csv(index=False))
