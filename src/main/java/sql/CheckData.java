package sql;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;


public class CheckData {

  public static void main(String[] args) throws IOException{
    //pullDataByUsers_id("d:\\sheet2.txt");
    //pullUsers();
    String startTime = "2019-05-01 00:00:00";
    String endTime = "2019-05-05 00:00:00";
    writeToFile(QuerySql.searchCTR(startTime, endTime), "d:\\search_ctr.csv");
  }


  private static void writeToFile(String sql, String fileName) throws IOException {
    CloseableHttpClient client = HttpClients.createDefault();
    Properties properties = new Properties();
    File file= new File("src/main/resources/druid");
    InputStream in = new FileInputStream(file);
    properties.load(in);
    HttpPost httpPost = new HttpPost(properties.getProperty("urlSql"));
    Map<String, String> hashMap = new HashMap<String, String>();
    hashMap.put("query", sql);
    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(hashMap);
    StringEntity entity = new StringEntity(json);
    httpPost.setEntity(entity);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("Content-type", "application/json");

    CloseableHttpResponse response = client.execute(httpPost);
    HttpEntity entity1 = response.getEntity();
    String responseString = EntityUtils.toString(entity1, "UTF-8");

    //System.out.println(responseString);
    String content = Json2Csv(responseString);
    if (content == null) {
      System.out.println("null");
      return;
    }
    writeToCsv(content, fileName);
    //System.out.println(content);


  }
  private static void writeToCsv(String content, String fileName) {
    try {
      File csv = new File(fileName);//CSV文件
      BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
      bw.write(content);
      bw.flush();
      bw.close();
    } catch (FileNotFoundException e) {
      //捕获File对象生成时的异常
      e.printStackTrace();
    } catch (IOException e) {
      //捕获BufferedWriter对象关闭时的异常
      e.printStackTrace();
    }
  }

  public static String Json2Csv(String json) throws JSONException {

    System.out.println(json);
    JSONArray jsonArray = new JSONArray(json);
    String csv = CDL.toString(jsonArray);
    return csv;
  }


}
