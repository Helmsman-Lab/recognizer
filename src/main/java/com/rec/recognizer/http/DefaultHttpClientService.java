package com.rec.recognizer.http;


import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;


public class DefaultHttpClientService implements HttpClientService {

    public static final String UTF_8_STR = "UTF-8";
    public static final String GBK_STR = "GBK";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final Integer TIMEOUT = 60000;
    /**
     * 超时时间 5秒
     */
    private final static RequestConfig REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(60000).build();
    /**
     * 读取时间 15秒
     */
    private final static SocketConfig SOCKET_CONFIG = SocketConfig.custom().setSoTimeout(15000).build();
    public HttpClientFactory httpClientFactory;

    public DefaultHttpClientService(HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public ClientResponse jsonPost(ClientRequest request) {
        ClientResponse response = new ClientResponse();
        StringEntity entity = new StringEntity(request.getContent(), UTF_8_STR);
        entity.setContentType(CONTENT_TYPE_JSON);
        HttpPost httpPost = new HttpPost(request.getUrl());
        httpPost.setEntity(entity);
        Map<String, String> headerMap = request.getHeaderMap();
        if (null != headerMap) {
            Set<String> headerName = headerMap.keySet();
            for (String name : headerName) {
                httpPost.addHeader(name, headerMap.get(name));
            }
        }
        execute(httpPost, response);
        return response;
    }

    @Override
    public ClientResponse post(ClientRequest request) {
        ClientResponse response = new ClientResponse();
        HttpPost httpPost = new HttpPost(request.getUrl());
        execute(httpPost, response);
        return response;
    }

    @Override
    public ClientResponse postForm(ClientRequest request){
        ClientResponse response = new ClientResponse();
        Map<String, String> params = request.getParams();
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        if (params != null && params.keySet() != null) {
            Iterator<String> keys = request.getParams().keySet().iterator();
            while (keys.hasNext()) {
                String paramKey = keys.next();
                pairs.add(new BasicNameValuePair(paramKey, params.get(paramKey)));
            }
        }
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, UTF_8_STR);
            entity.setContentType("application/x-www-form-urlencoded");
            HttpPost httpPost = new HttpPost(request.getUrl());
            httpPost.setEntity(entity);
            execute(httpPost, response);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("encoding error");
        }
        return response;
    }

    private void execute(HttpRequestBase httpRequest, ClientResponse clientResponse) {
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse response) throws IOException {
                clientResponse.setHttpStatus(response.getStatusLine().getStatusCode());
                HttpEntity entity = response.getEntity();
                Header[] headers = response.getHeaders("Content-Type");
                String charset = UTF_8_STR;
                if (null != headers && headers.length > 0) {
                    String contentType = headers[0].getValue();
                    if (StringUtils.isNotEmpty(contentType)) {
                        if (contentType.toLowerCase().contains("gbk")) {
                            charset = GBK_STR;
                        }
                    }
                }
                return entity != null ? EntityUtils.toString(entity, charset) : null;
            }
        };
        CloseableHttpClient httpClient = null;
        if (null == TIMEOUT) {
            httpRequest.setConfig(REQUEST_CONFIG);
            httpClient = getClient(REQUEST_CONFIG, SOCKET_CONFIG);
        } else {
            RequestConfig build = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(TIMEOUT).build();
            httpRequest.setConfig(build);
            httpClient = getClient(build, SOCKET_CONFIG);
        }

        String result;
        try {
            result = httpClient.execute(httpRequest, responseHandler);
        } catch (Exception e) {
            throw new RuntimeException("网络连接异常：协议异常", e);
        }  finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException("网络连接异常:未能正确关闭连接", e);
            }
        }
        // 后置处理 todo
        clientResponse.setContent(result);
    }

    private CloseableHttpClient getClient(RequestConfig requestConfig, SocketConfig socketConfig) {
        return this.httpClientFactory.getClient(requestConfig, socketConfig);
    }
}
