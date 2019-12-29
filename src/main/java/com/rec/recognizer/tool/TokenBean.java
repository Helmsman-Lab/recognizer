package com.rec.recognizer.tool;

/**
 * @ClassName TokenBean
 * @Discription TokenBean
 * @Author zhaoxianghui
 * @Date 2019/12/26 - 16:03
 **/
public class TokenBean {
    private String accessToken;
    private long expireDateSeconds;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpireDateSeconds() {
        return expireDateSeconds;
    }

    public void setExpireDateSeconds(long expireDateSeconds) {
        this.expireDateSeconds = expireDateSeconds;
    }
}
