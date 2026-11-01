package com.example.aftersales.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * 全局异常处理：把 Service 抛出的 ApiException 变成统一 JSON。
 *
 * <p>Controller 里因此不用写 try-catch，业务规则校验的失败路径集中在一个地方，
 * 前端也只需要读 {@code {"message": "..."}} 就能显示中文原因。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatus());
        return ResponseEntity.status(status).body(Map.of(
                "message", exception.getMessage(),
                "status", String.valueOf(exception.getStatus())
        ));
    }

    /**
     * 请求体 JSON 写错、或枚举值不在允许范围内时，Jackson 会抛这个异常。
     * 转成 400 并给出人能看懂的提示，而不是默认的 400 空白页。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleBadBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "请求体格式不正确：请检查字段名与取值是否在允许的范围内"
        ));
    }

    /**
     * 重复处理兜底：两个岗位同时点同一个动作时，先到的事务已经写过仓库记录，
     * 后到的会被 warehouse_record.after_sale_id 的唯一索引拦住。
     * 这里翻译成了 409，页面提示「已处理过」并让前端重新拉取真实状态，
     * 保证「重复处理不会额外产生结果」。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateKey(DuplicateKeyException exception) {
        log.warn("唯一约束拦下重复操作：{}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", "这条单据已经被处理过了，数据库里没有产生第二份结果，请刷新查看最新状态",
                "status", "409"
        ));
    }

    /**
     * 静态资源不存在（例如浏览器还缓存着旧版 app.js 的引用）应当是 404，
     * 不能被下面的兜底逻辑说成「服务器处理失败」。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleMissingResource(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "message", "资源不存在：" + exception.getResourcePath(),
                "status", "404"
        ));
    }

    /**
     * 兜底：不认识的异常一律 500，并把完整堆栈打进日志方便定位，
     * 返回给页面的只保留异常类型，不泄露 SQL 细节。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception exception) {
        log.error("未预期的服务端异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "服务器处理失败：" + exception.getClass().getSimpleName()
                        + "（请确认已执行 init.sql 建好 aftersales_demo 库，且后端是在建库之后启动的）"
        ));
    }
}
