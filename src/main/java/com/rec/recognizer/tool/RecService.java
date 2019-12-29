package com.rec.recognizer.tool;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;

import com.rec.recognizer.http.ClientRequest;
import com.rec.recognizer.http.ClientResponse;
import com.rec.recognizer.http.HttpDownload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RecService extends HttpService {

    @Autowired
    private TokenService tokenService;

    private static final String REQUEST_URL = "https://aip.baidubce.com/rest/2.0/solution/v1/form_ocr/request";
    private static final String RESULT_URL = "https://aip.baidubce.com/rest/2.0/solution/v1/form_ocr/get_request_result";

    private Map<String, String> headerMap;

    public void recognize(String filepath) throws IOException {
        String requestId = submitRequest(filepath);
        String url = getResult(requestId);
        System.out.println(url);
        HttpDownload.download(url, filepath+".xlsx");
    }

    private String submitRequest(String filepath) throws IOException {
        String requestUrl = REQUEST_URL.concat("?access_token=").concat(tokenService.getAccessToken());
        ClientRequest request = new ClientRequest();
        request.setUrl(requestUrl);
        request.setHeaderMap(getHeaderMap());
        Map<String, String> params = new HashMap<>();
        params.put("image", base64UrlEncode(filepath));
        params.put("is_sync", "false");
        params.put("request_type", "excel");
        System.out.println(JSON.toJSONString(params));
        request.setParams(params);
        ClientResponse response = getHttpClientService().postForm(request);
        log.info(String.format("submit response for %s,%s", filepath, response.getContent()));
        JSONObject jsonObject = JSON.parseObject(response.getContent());
        return jsonObject.getJSONArray("result").getJSONObject(0).getString("request_id");
    }

    private String base64UrlEncode(String filepath) throws IOException {
        byte[] imgData = readFileByBytes(filepath);
        return Base64Util.encode(imgData);
    }

    private String getResult(String requestId) {
        String requestUrl = RESULT_URL.concat("?access_token=").concat(tokenService.getAccessToken());
        ClientRequest request = new ClientRequest();
        request.setUrl(requestUrl);
        request.setHeaderMap(getHeaderMap());
        Map<String, String> params = new HashMap<>();
        params.put("request_id", requestId);
        request.setParams(params);
        ClientResponse response = getHttpClientService().postForm(request);
        log.info(String.format(" result for requestId for %s,%s", requestId, response.getContent()));
        JSONObject jsonObject = JSON.parseObject(response.getContent());

        String retCode = jsonObject.getJSONObject("result").getString("ret_code");
        while (!retCode.equals("3")) {
            try {
                Thread.sleep(2010);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            response = getHttpClientService().postForm(request);
            log.info(String.format(" result for requestId for %s,%s", requestId, response.getContent()));
            jsonObject = JSON.parseObject(response.getContent());
            retCode = jsonObject.getJSONObject("result").getString("ret_code");
        }

        return jsonObject.getJSONObject("result").getString("result_data");
    }

    private Map<String, String> getHeaderMap() {
        if (headerMap == null) {
            headerMap = new HashMap<>();
            headerMap.put("Content-Type", "application/x-www-form-urlencoded");
        }
        return headerMap;
    }

    public static byte[] readFileByBytes(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException(filePath);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream(((int) file.length()));
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            int bufSize = 1024;
            byte[] buffer = new byte[bufSize];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, bufSize))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            bos.close();
        }
    }



}
