cwlVersion: v1.2
class: CommandLineTool

baseCommand: ["python", "app/processing_data.py"]

hints:
  DockerRequirement:
    dockerPull: bogdandjelkovic/cc-3-cwl-model-training

inputs:
  csv_input:
    type: File
    inputBinding:
      position: 1

outputs:
  csv_cleaned:
    type: stdout

stdout: csv_cleaned.csv