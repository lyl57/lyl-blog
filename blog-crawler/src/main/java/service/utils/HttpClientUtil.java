package service.utils;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.dto.BaseResponse;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.cn;

/**
 * @author Created by lyl57 on 2017/6/15.
 */
public class HttpClientUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final String UTF8 = "UTF-8";

    private static PoolingHttpClientConnectionManager cm = null;

    private static RequestConfig defaultRequestConfig = null;

    private static int requestWarnTime = 2000;//请求时间过长警告时间

    static {
        LayeredConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException e) {
            logger.error("init SSLConnectionSocketFactory is error", e);
        }
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(50);
        defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .build();
    }

    /**
     * post请求
     *
     * @param url
     * @param headerMap
     * @param params
     * @return
     * @throws IOException
     */
    public static BaseResponse doPostRequest(String url, Map<String, String> headerMap, String params) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            //请求头
            httpPost.setHeader(entry.getKey(), entry.getValue());
        }
        // 解决中文乱码问题
        httpPost.setEntity(new StringEntity(params, UTF8));
        return doRequest(httpPost);
    }

    /**
     * get请求
     *
     * @param url
     * @param headerMap
     * @return
     * @throws IOException
     */
    public static BaseResponse doGetRequest(String url, Map<String, String> headerMap) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            //请求头
            httpGet.setHeader(entry.getKey(), entry.getValue());
        }
        return doRequest(httpGet);
    }


    /**
     * get请求
     *
     * @param url
     * @param headerMap
     * @return
     * @throws IOException
     */
    public static BaseResponse doDeleteRequest(String url, Map<String, String> headerMap) throws Exception {
        HttpDelete httpDelete = new HttpDelete(url);
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {//请求头
            httpDelete.setHeader(entry.getKey(), entry.getValue());
        }
        return doRequest(httpDelete);
    }


    private static BaseResponse doRequest(HttpRequestBase httpRequestBase) throws Exception {
        long startTime = System.currentTimeMillis();
        BaseResponse baseResponse = new BaseResponse();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(defaultRequestConfig).setConnectionManager(cm).build();
        try {
            CloseableHttpResponse response = httpClient.execute(httpRequestBase);
            baseResponse.setResult(EntityUtils.toString(response.getEntity(), UTF8));
            baseResponse.setStatusCode(response.getStatusLine().getStatusCode());
            response.close();
        } catch (Exception e) {
            try {
                CloseableHttpResponse response = httpClient.execute(httpRequestBase);
                baseResponse.setResult(EntityUtils.toString(response.getEntity(), UTF8));
                baseResponse.setStatusCode(response.getStatusLine().getStatusCode());
                response.close();
            } catch (Exception e1) {
                if (e instanceof ConnectionPoolTimeoutException) {
                    logger.error("Timeout waiting for connection from pool.maxTotal=" + cm.getTotalStats().getMax() + ";pendingTotal=" + cm.getTotalStats().getPending());
                }
                throw e1;
            }
        } finally {
            httpRequestBase.releaseConnection();
        }
        long costTime = System.currentTimeMillis() - startTime;
        if (costTime > requestWarnTime) {
            logger.warn("request [" + httpRequestBase.getURI().toString() + "] costTime:" + costTime);
        }
        return baseResponse;
    }

}
