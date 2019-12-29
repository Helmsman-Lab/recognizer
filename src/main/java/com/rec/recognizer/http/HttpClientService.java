package com.rec.recognizer.http;

/**
 * @ClassName HttpClientService
 * @Discription HttpClientService
 * @Author zhaoxianghui
 * @Date 2019/12/23 - 9:44
 **/
public interface HttpClientService {

    /**
     * http post json
     *
     * @param request
     * @return
     */
    ClientResponse jsonPost(ClientRequest request);

    ClientResponse post(ClientRequest request);

    ClientResponse postForm(ClientRequest request);
}
