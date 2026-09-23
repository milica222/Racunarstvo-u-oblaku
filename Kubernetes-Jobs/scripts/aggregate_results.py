import json
import os
import statistics


k_folds = int(os.environ["K_FOLDS"])


with open("results/folds.jsonl") as file:
    results = [
        json.loads(line)
        for line in file
        if line.strip()
    ]


results_by_fold = {
    result["fold"]: result
    for result in results
}


results = [
    results_by_fold[index]
    for index in sorted(results_by_fold)
]


if len(results) != k_folds:
    raise RuntimeError(
        f"Ocekivano {k_folds} foldova, "
        f"ali pronadjeno {len(results)} rezultata."
    )


mae_values = [
    result["mae"]
    for result in results
]

rmse_values = [
    result["rmse"]
    for result in results
]

r2_values = [
    result["r2"]
    for result in results
]


final_result = {
    "k_folds": k_folds,
    "fold_results": results,

    "mean_mae": statistics.mean(mae_values),
    "std_mae": statistics.stdev(mae_values) if k_folds > 1 else 0,

    "mean_rmse": statistics.mean(rmse_values),
    "std_rmse": statistics.stdev(rmse_values) if k_folds > 1 else 0,

    "mean_r2": statistics.mean(r2_values),
    "std_r2": statistics.stdev(r2_values) if k_folds > 1 else 0
}


with open("results/final_results.json", "w") as file:
    json.dump(final_result, file, indent=4)


print()
print("================================")
print("REZULTATI PO FOLDOVIMA")
print("================================")

for result in results:
    print(
        f"Fold {result['fold']}: "
        f"MAE={result['mae']:.4f}, "
        f"RMSE={result['rmse']:.4f}, "
        f"R2={result['r2']:.4f}"
    )


print()
print("================================")
print("KONACNI REZULTATI")
print("================================")

print(f"Mean MAE:  {final_result['mean_mae']:.4f}")
print(f"Mean RMSE: {final_result['mean_rmse']:.4f}")
print(f"Mean R2:   {final_result['mean_r2']:.4f}")

print("================================")