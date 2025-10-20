cwlVersion: v1.2
class: Workflow

inputs:
  csv_input: File
  target_column:
    type: string
  train_pct:
    type: float

steps:
  processing_data:
    run: processing_data.cwl
    in:
      csv_input: csv_input
    out: [csv_cleaned]

  training:
    run: training.cwl
    in:
      csv_cleaned: processing_data/csv_cleaned
      target_column: target_column
      train_pct: train_pct
    out: [metrics]

outputs:
  metrics:
    type: File
    outputSource: training/metrics