import sys

rmse_files = sys.argv[1:] 
rmse_values = []

for f in rmse_files:
    with open(f) as fh:
        val = fh.read().strip()

        if val == '':
            continue

        rmse_values.append(float(val))

if not rmse_values:
    raise RuntimeError("No RMSE files found!")

avg_rmse = sum(rmse_values) / len(rmse_values)

print(avg_rmse)