package com.rec.recognizer.tool;
import com.alibaba.fastjson.JSONObject;

import com.alibaba.fastjson.JSON;
import com.rec.recognizer.http.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.List;

/**
 * @ClassName TokenService
 * @Discription TokenService
 * @Author zhaoxianghui
 * @Date 2019/12/26 - 15:48
 **/
@Service
@Slf4j
public class TokenService extends HttpService {

    private static final String TOKEN_REQUEST_URL = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials";

    @Value("${app.config.clientId}")
    private String clientId;
    @Value("${app.config.clientSecret}")
    private String clientSecret;

    private TokenBean tokenBean;

    private static final String TOKEN_FILE = "./access_token";



    @PostConstruct
    public void init() {
        try {
            File accessTokenFile = new File(TOKEN_FILE);
            if (!accessTokenFile.exists()) {
                return;
            }
            List<String> tokenBeans = IOUtils.readLines(new FileInputStream(accessTokenFile));
            if (CollectionUtils.isEmpty(tokenBeans)) {
                return;
            }
            tokenBean = JSON.parseObject(tokenBeans.get(0), TokenBean.class);
        } catch (IOException e) {
            log.warn("获取token异常");
            throw new RuntimeException("获取token异常");
        }
    }

    private void reWriteToken() {
        try {
            File accessTokenFile = new File(TOKEN_FILE);
            if (!accessTokenFile.exists()) {
                accessTokenFile.createNewFile();
            }
            IOUtils.write(JSON.toJSONString(tokenBean), new FileOutputStream(accessTokenFile));
        } catch (Exception e) {
            throw new RuntimeException("更新token文件失败");
        }

    }

    /**
     * 获取token
     * @return
     */
    public String getAccessToken() {
        if (needReGet()) {
            getRemoteAccessToken();
        }
        log.info("token: " + JSON.toJSONString(tokenBean));
        return tokenBean.getAccessToken();
    }

    private boolean needReGet() {
        if (tokenBean == null) {
            return true;
        }
        if (System.currentTimeMillis()/1000 - tokenBean.getExpireDateSeconds() >= 0) {
            return true;
        }
        return false;
    }

    private void getRemoteAccessToken() {
        tokenBean = new TokenBean();
        String requestUrl = TOKEN_REQUEST_URL.concat("&client_id=").concat(clientId).concat("&client_secret=").concat(clientSecret);
        ClientRequest request =  new ClientRequest();
        request.setUrl(requestUrl);
        ClientResponse response = getHttpClientService().post(request);
        JSONObject jsonObject = JSON.parseObject(response.getContent());
        tokenBean.setAccessToken(jsonObject.getString("access_token"));
        tokenBean.setExpireDateSeconds(calc(Long.parseLong(jsonObject.getString("expires_in"))));
        reWriteToken();
    }

    private long calc(long expireIn) {
        return System.currentTimeMillis()/1000 + expireIn;
    }



}
