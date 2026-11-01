package com.example.aftersales.exception;

/**
 * 带 HTTP 状态码的业务异常。
 *
 * <p>Service 只管把「为什么这件事不能做」说清楚，状态码的选择集中在这里：</p>
 * <ul>
 *     <li>400：入参或业务规则不满足（超量、超期、原因没填）；</li>
 *     <li>401：没选演示身份；</li>
 *     <li>403：身份合法但岗位/归属不对（越权）；</li>
 *     <li>404：单据不存在；</li>
 *     <li>409：单据当前状态不允许这次操作。</li>
 * </ul>
 *
 * <p>前端按状态码区分「提示重试」和「提示没权限」，Network 面板里一眼能看出区别。</p>
 */
public class ApiException extends RuntimeException {

    private final int status;

    private ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(401, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }
}
