cwlVersion: v1.2
class: CommandLineTool

baseCommand: ["python", "app/processing_data.py"]

hints:
  DockerRequirement:
    dockerPull: milicavasovic222/osnove-cwl

inputs:
  csv_input:
    type: File
    inputBinding:
      position: 1

outputs:
  csv_cleaned:
    type: stdout

stdout: csv_cleaned.csv