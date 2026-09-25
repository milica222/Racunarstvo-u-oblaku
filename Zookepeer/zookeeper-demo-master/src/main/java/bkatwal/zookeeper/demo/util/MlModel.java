package bkatwal.zookeeper.demo.util;

import static bkatwal.zookeeper.demo.util.ZkDemoUtil.getHostPostOfServer;
import static bkatwal.zookeeper.demo.util.ZkDemoUtil.isEmpty;

import java.io.ByteArrayInputStream;
// import java.math.BigInteger;
// import java.security.MessageDigest;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Random;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
// import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
// import weka.classifiers.AbstractClassifier;
// import weka.classifiers.Evaluation;
// import weka.classifiers.functions.Logistic;
// import weka.classifiers.trees.RandomForest;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

@Slf4j
public final class MlModel {

  private static Classifier classifier;
  private static Instances header;
  private static byte[] trainingCsv;
  private static Map<String, Object> info;

  public static synchronized Map<String, Object> getInfo() {
    return info;
  }

  public static synchronized byte[] getTrainingCsv() {
    return trainingCsv;
  }

  public static synchronized Map<String, Object> train(byte[] csv, String targetColumn) {
    Instances data;
    try {
      CSVLoader loader = new CSVLoader();
      loader.setSource(new ByteArrayInputStream(csv));
      data = loader.getDataSet();
    } catch (Exception e) {
      throw new IllegalArgumentException("CSV fajl nije moguce procitati: " + e.getMessage());
    }

    Attribute target =
        isEmpty(targetColumn)
            ? data.attribute(data.numAttributes() - 1)
            : data.attribute(targetColumn);
    if (target == null) {
      throw new IllegalArgumentException("Kolona '" + targetColumn + "' ne postoji u CSV-u.");
    }
    data.setClassIndex(target.index());

    Classifier newClassifier = target.isNominal() ? new J48() : new LinearRegression();
    try {
      newClassifier.buildClassifier(data);
    } catch (Exception e) {
      throw new IllegalArgumentException("Treniranje nije uspelo: " + e.getMessage());
    }

    classifier = newClassifier;
    header = new Instances(data, 0);
    trainingCsv = csv;

    info = new LinkedHashMap<>();
    info.put("version", Arrays.hashCode(csv));
    info.put("targetColumn", target.name());
    info.put("algorithm", newClassifier.getClass().getSimpleName());
    info.put("trainedAt", new Date().toString());
    return info;
  }

  public static synchronized Map<String, Object> predict(Map<String, Object> features)
      throws Exception {
    if (classifier == null) {
      throw new IllegalStateException("Na ovom cvoru jos nema modela - prvo pozovite PUT /model.");
    }

    Instance row = new DenseInstance(header.numAttributes());
    row.setDataset(header);
    for (int i = 0; i < header.numAttributes(); i++) {
      Object value = features.get(header.attribute(i).name());
      if (i == header.classIndex() || value == null) {
        row.setMissing(i);
      } else {
        row.setValue(i, Double.parseDouble(value.toString()));
      }
    }

    double predicted = classifier.classifyInstance(row);
    Attribute target = header.classAttribute();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "prediction", target.isNominal() ? target.value((int) predicted) : Math.round(predicted));
    result.put("modelVersion", info.get("version"));
    return result;
  }

  public static void syncFromMaster() {
    String master = ClusterInfo.getClusterInfo().getMaster();
    if (isEmpty(master) || master.equals(getHostPostOfServer())) {
      return;
    }
    try {
      RestTemplate restTemplate = new RestTemplate();
      Map masterInfo = restTemplate.getForObject("http://" + master + "/model", Map.class);
      Map masterModel = (Map) masterInfo.get("model");
      if (masterModel == null) {
        return;
      }
      byte[] csv =
          restTemplate.getForObject("http://" + master + "/model/trainingData", byte[].class);
      train(csv, (String) masterModel.get("targetColumn"));
      log.info("model preuzet od lidera {}", master);
    } catch (Exception e) {
      log.warn("preuzimanje modela od lidera {} nije uspelo: {}", master, e.getMessage());
    }
  }

  private MlModel() {}
}
