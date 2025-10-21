cwlVersion: v1.2
class: CommandLineTool

baseCommand: ["python", "app/training.py"]

hints:
    DockerRequirement:
        dockerPull: milicavasovic222/cwl-scatter

inputs:
    csv_cleaned:
        type: File
        inputBinding:
            position: 1
    target_column:
        type: string
        inputBinding:
            position: 2
    fold:
        type: int
        inputBinding:
            position: 3

    K:
        type: int
        inputBinding:
            position: 4

outputs:
    rmse_file:
        type: stdout

stdout: rmse_file.txt