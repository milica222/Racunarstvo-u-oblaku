cwlVersion: v1.2
class: Workflow

requirements:
  ScatterFeatureRequirement: {}
  StepInputExpressionRequirement: {}
  InlineJavascriptRequirement: {}

inputs:
  csv_input: File
  target_column: string
  folds: int

steps:
  processing_data:
    run: processing_data.cwl
    in:
      csv_input: csv_input
    out: [csv_cleaned]

  generate_folds:
    run:
      class: ExpressionTool
      inputs:
        K: int
      outputs:
        folds_list:
          type:
            type: array
            items: int
      expression: |
        ${
          return { "folds_list": Array.from({length: inputs.K}, (_, i) => i) };
        }
    in:
      K: folds
    out: [folds_list]

  cross_validation:
    run: training.cwl
    scatter: [fold]
    scatterMethod: flat_crossproduct
    in:
      csv_cleaned: processing_data/csv_cleaned
      target_column: target_column
      fold: generate_folds/folds_list
      K: folds
    out: [rmse_file]

  calculate_avg_rmse:
    run: calculate_avg_rmse.cwl
    in:
      rmse_files: cross_validation/rmse_file
    out: [aggregated_rmse]

outputs:
  aggregated_rmse:
    type: File
    outputSource: calculate_avg_rmse/aggregated_rmse
