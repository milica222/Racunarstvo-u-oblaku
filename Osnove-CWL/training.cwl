cwlVersion: v1.2
class: CommandLineTool

baseCommand: ["python", "app/training.py"]

hints:
    DockerRequirement:
        dockerPull: milicavasovic222/osnove-cwl

inputs:
    csv_cleaned:
        type: File
        inputBinding:
            position: 1
    target_column:
        type: string
        inputBinding:
            position: 2
    train_pct:
        type: float
        inputBinding:
            position: 3

outputs:
    metrics:
        type: stdout

stdout: metrics.txt