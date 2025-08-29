package com.bajaj.qualifier1;
    

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication
public class App {

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  @Bean RestTemplate restTemplate() { return new RestTemplate(); }

  @Bean
  ApplicationRunner run(RestTemplate http,
                        @Value("${Arya Jhawar}") String name,
                        @Value("${22BEC0023}") String regNo,
                        @Value("${aryajhawar07@gmail.com}") String email,
                        @Value("${endpoints.base}") String base,
                        @Value("${endpoints.generate}") String genPath,
                        @Value("${endpoints.submitFallback}") String submitFallback) {
    return args -> {
      // 1) Generate webhook + token
      String generateUrl = base + genPath;
      Map<String, String> payload = Map.of("Arya Jhawar", name, "22BEC0023", regNo, "aryajhawar07@gmail.com", email);
      ResponseEntity<Map> genResp = http.postForEntity(generateUrl, payload, Map.class);

      if (!genResp.getStatusCode().is2xxSuccessful() || genResp.getBody() == null) {
        System.out.println("Failed to generate webhook/token. HTTP=" + genResp.getStatusCodeValue());
        return;
      }

      Map body = genResp.getBody();
      String webhook = optString(body.get("webhook"));
      String accessToken = optString(body.get("accessToken"));

      System.out.println("Webhook: " + webhook);
      System.out.println("AccessToken: " + mask(accessToken));

      // 2) Since regNo is odd → Question 1
      String finalQuery =
          "SELECT p.AMOUNT AS SALARY, " +
          "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
          "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
          "d.DEPARTMENT_NAME " +
          "FROM PAYMENTS p " +
          "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
          "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
          "WHERE DAY(p.PAYMENT_TIME) <> 1 " +
          "ORDER BY p.AMOUNT DESC LIMIT 1;";

      // 3) Save locally
      String fileName = "solution-" + regNo + ".txt";
      try (FileWriter fw = new FileWriter(fileName, StandardCharsets.UTF_8, false)) {
        fw.write("-- saved at " + LocalDateTime.now() + System.lineSeparator());
        fw.write(finalQuery + System.lineSeparator());
      }
      System.out.println("Saved solution to " + fileName);

      // 4) Submit
      String submitUrl = (webhook != null && !webhook.isBlank()) ? webhook : (base + submitFallback);
      Map<String, String> submitPayload = Map.of("finalQuery", finalQuery);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Authorization", accessToken);
      HttpEntity<Map<String, String>> req = new HttpEntity<>(submitPayload, headers);

      ResponseEntity<String> submitResp = http.postForEntity(submitUrl, req, String.class);
      System.out.println("Submit status: " + submitResp.getStatusCodeValue());
      System.out.println("Submit response: " + submitResp.getBody());
    };
  }

  private static String optString(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static String mask(String token) {
    if (token == null) return "(null)";
    if (token.length() <= 6) return "******";
    return token.substring(0, 3) + "..." + token.substring(token.length() - 3);
  }
}


