package com.rec.recognizer.tool;

import com.rec.recognizer.http.DefaultHttpClientService;
import com.rec.recognizer.http.HttpClientFactory;
import com.rec.recognizer.http.HttpClientService;

public abstract class HttpService {
    private HttpClientService httpClientService;

    protected synchronized HttpClientService getHttpClientService() {
        if (httpClientService == null) {
            HttpClientFactory factory = new HttpClientFactory();
            httpClientService = new DefaultHttpClientService(factory);
        }
        return httpClientService;
    }
}
