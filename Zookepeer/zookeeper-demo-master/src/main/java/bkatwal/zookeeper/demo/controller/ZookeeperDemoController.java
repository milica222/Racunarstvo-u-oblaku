package bkatwal.zookeeper.demo.controller;

import static bkatwal.zookeeper.demo.util.ZkDemoUtil.getHostPostOfServer;
// import static bkatwal.zookeeper.demo.util.ZkDemoUtil.isEmpty;

// import bkatwal.zookeeper.demo.model.Person;
import bkatwal.zookeeper.demo.util.ClusterInfo;
// import bkatwal.zookeeper.demo.util.DataStorage;
import bkatwal.zookeeper.demo.util.MlModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
// import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
// import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/** @author "Bikas Katwal" 26/03/19 */
@RestController
public class ZookeeperDemoController {

  private RestTemplate restTemplate = new RestTemplate();

  /*
  @PutMapping("/person/{id}/{name}")
  public ResponseEntity<String> savePerson(
      HttpServletRequest request,
      @PathVariable("id") Integer id,
      @PathVariable("name") String name) {

    String requestFrom = request.getHeader("request_from");
    String leader = ClusterInfo.getClusterInfo().getMaster();
    if (!isEmpty(requestFrom) && requestFrom.equalsIgnoreCase(leader)) {
      Person person = new Person(id, name);
      DataStorage.setPerson(person);
      return ResponseEntity.ok("SUCCESS");
    }
    // If I am leader I will broadcast data to all live node, else forward request to leader
    if (amILeader()) {
      List<String> liveNodes = ClusterInfo.getClusterInfo().getLiveNodes();

      int successCount = 0;
      for (String node : liveNodes) {

        if (getHostPostOfServer().equals(node)) {
          Person person = new Person(id, name);
          DataStorage.setPerson(person);
          successCount++;
        } else {
          String requestUrl =
              "http://"
                  .concat(node)
                  .concat("/person")
                  .concat("/")
                  .concat(String.valueOf(id))
                  .concat("/")
                  .concat(name);
          HttpHeaders headers = new HttpHeaders();
          headers.add("request_from", leader);
          headers.setContentType(MediaType.APPLICATION_JSON);

          HttpEntity<String> entity = new HttpEntity<>(headers);
          restTemplate.exchange(requestUrl, HttpMethod.PUT, entity, String.class).getBody();
          successCount++;
        }
      }

      return ResponseEntity.ok()
          .body("Successfully update ".concat(String.valueOf(successCount)).concat(" nodes"));
    } else {
      String requestUrl =
          "http://"
              .concat(leader)
              .concat("/person")
              .concat("/")
              .concat(String.valueOf(id))
              .concat("/")
              .concat(name);
      HttpHeaders headers = new HttpHeaders();

      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> entity = new HttpEntity<>(headers);
      return restTemplate.exchange(requestUrl, HttpMethod.PUT, entity, String.class);
    }
  }

  @GetMapping("/persons")
  public ResponseEntity<List<Person>> getPerson() {

    return ResponseEntity.ok(DataStorage.getPersonListFromStorage());
  }
  */

  private boolean amILeader() {
    String leader = ClusterInfo.getClusterInfo().getMaster();
    return getHostPostOfServer().equals(leader);
  }

  @GetMapping("/clusterInfo")
  public ResponseEntity<ClusterInfo> getClusterinfo() {

    return ResponseEntity.ok(ClusterInfo.getClusterInfo());
  }

  @PutMapping(value = "/model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> updateModel(
      HttpServletRequest request,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "targetColumn", required = false) String targetColumn)
      throws IOException {

    byte[] csv = file.getBytes();
    String leader = ClusterInfo.getClusterInfo().getMaster();

    if (leader != null && leader.equals(request.getHeader("request_from"))) {
      return ResponseEntity.ok(MlModel.train(csv, targetColumn));
    }

    if (!amILeader()) {
      return ResponseEntity.ok(sendModel(leader, csv, targetColumn));
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("node", getHostPostOfServer());
    result.put("model", MlModel.train(csv, targetColumn));

    List<String> updatedNodes = new ArrayList<>();
    updatedNodes.add(getHostPostOfServer());
    for (String node : ClusterInfo.getClusterInfo().getLiveNodes()) {
      if (node.equals(getHostPostOfServer())) {
        continue;
      }
      try {
        sendModel(node, csv, targetColumn);
        updatedNodes.add(node);
      } catch (Exception e) {
      }
    }
    result.put("updatedNodes", updatedNodes);
    return ResponseEntity.ok(result);
  }

  private Map<String, Object> sendModel(String node, byte[] csv, String targetColumn) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    if (amILeader()) {
      headers.add("request_from", getHostPostOfServer());
    }

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file",
        new ByteArrayResource(csv) {
          @Override
          public String getFilename() {
            return "training.csv";
          }
        });
    if (targetColumn != null) {
      body.add("targetColumn", targetColumn);
    }

    return restTemplate
        .exchange(
            "http://" + node + "/model",
            HttpMethod.PUT,
            new HttpEntity<>(body, headers),
            Map.class)
        .getBody();
  }

  @PostMapping("/predict")
  public ResponseEntity<Map<String, Object>> predict(@RequestBody Map<String, Object> features)
      throws Exception {
    Map<String, Object> result = MlModel.predict(features);
    result.put("servedBy", getHostPostOfServer());
    return ResponseEntity.ok(result);
  }

  @GetMapping("/model")
  public ResponseEntity<Map<String, Object>> getModelInfo() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("node", getHostPostOfServer());
    result.put("master", ClusterInfo.getClusterInfo().getMaster());
    result.put("model", MlModel.getInfo());
    return ResponseEntity.ok(result);
  }

  @GetMapping(value = "/model/trainingData", produces = "text/csv")
  public ResponseEntity<byte[]> getTrainingData() {
    byte[] csv = MlModel.getTrainingCsv();
    return csv == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(csv);
  }
}
