package service.dto;

/**
 * @author Created by lyl57 on 2017/6/16.
 * 请求相应体
 */
public class BaseResponse {
    private int statusCode;//状态码
    private String result;//响应内容

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
