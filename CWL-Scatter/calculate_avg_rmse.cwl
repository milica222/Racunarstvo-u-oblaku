cwlVersion: v1.2
class: CommandLineTool

baseCommand: ["python", "app/calculate_avg_rmse.py"]

hints:
  DockerRequirement:
    dockerPull: milicavasovic222/cwl-scatter

inputs:
  rmse_files:
    type:
      type: array
      items: File
    inputBinding:
      prefix: ""

outputs:
  aggregated_rmse:
    type: stdout
      
stdout: aggregated_rmse.txt