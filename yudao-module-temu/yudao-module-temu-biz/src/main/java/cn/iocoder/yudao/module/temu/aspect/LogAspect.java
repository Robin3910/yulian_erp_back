package cn.iocoder.yudao.module.temu.aspect;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.temu.service.operationlog.TemuOperationLogService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 操作日志切面
 * 拦截 temu 目录下的所有 controller，记录操作日志
 *
 * @author 禹链科技
 */
@Aspect
@Component
@Slf4j
public class LogAspect {

    @Resource
    private TemuOperationLogService operationLogService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ConfigApi configApi;

    /**
     * 拦截 AdminTemuCategoryController 下的 createCategory，deleteProductCategory 和 updateProductCategory 方法
     * 以及 AdminTemuOrderShippingController 下的 batchUpdateOrderStatus 方法
     * 以及 AdminTemuOrderBatchController 下的 dispatchTask、completeBatchOrderTaskByAdmin、completeBatchOrderTask、updateStatus、updateStatusByTask 方法
     * 以及 AdminTemuOrderController 下的 beatchUpdateStatus、batchSave 方法
     * 以及 PayWalletController 下的 createWalletRecharge、submitPayOrder 方法
     */
    @Around("execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuCategoryController.createCategory(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuCategoryController.updateProductCategory(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuCategoryController.deleteProductCategory(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderShippingController.batchUpdateOrderStatus(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderBatchController.dispatchTask(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderBatchController.completeBatchOrderTaskByAdmin(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderBatchController.completeBatchOrderTask(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderBatchController.updateStatus(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderBatchController.updateStatusByTask(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderController.beatchUpdateStatus(..)) || " +
            "execution(* cn.iocoder.yudao.module.temu.controller.admin.controller.AdminTemuOrderController.batchSave(..)) || " +
            "execution(* cn.iocoder.yudao.module.pay.controller.admin.wallet.PayWalletController.createWalletRecharge(..)) || " +
            "execution(* cn.iocoder.yudao.module.pay.controller.admin.wallet.PayWalletController.submitPayOrder(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查是否启用操作日志功能
        String enableLogStr = configApi.getConfigValueByKey("yulian.operation_log.enabled");
        boolean enableLog = Boolean.parseBoolean(enableLogStr);
        
        if (!enableLog) {
            log.debug("操作日志功能未启用，跳过日志记录");
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        String requestParams = "";
        String responseResult = "";
        String module = "";
        String operationType = "";
        
        try {
            // 获取请求参数（处理特殊类型）
            requestParams = getRequestParams(joinPoint.getArgs());
            
            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = method.getDeclaringClass().getSimpleName();
            String methodName = method.getName();
            
            // 获取操作模块和操作类型
            module = getModuleFromTag(method);
            operationType = getOperationType(method);
            
            // 执行原方法
            result = joinPoint.proceed();
            
            // 获取响应结果
            responseResult = JsonUtils.toJsonString(result);
            
            return result;
        } catch (Exception e) {
            // 记录异常信息
            responseResult = "异常: " + e.getMessage();
            throw e;
        } finally {
            // 异步记录操作日志
            try {
                recordOperationLog(module, operationType, requestParams, responseResult, 
                                 joinPoint, System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }

    /**
     * 获取请求参数，处理特殊类型如 MultipartFile
     */
    private String getRequestParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            Object[] processedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    processedArgs[i] = null;
                } else if (arg instanceof org.springframework.web.multipart.MultipartFile) {
                    // 处理 MultipartFile 类型
                    org.springframework.web.multipart.MultipartFile file = (org.springframework.web.multipart.MultipartFile) arg;
                    processedArgs[i] = new FileInfo(file.getOriginalFilename(), file.getSize(), file.getContentType());
                } else if (arg instanceof org.springframework.web.multipart.MultipartFile[]) {
                    // 处理 MultipartFile 数组
                    org.springframework.web.multipart.MultipartFile[] files = (org.springframework.web.multipart.MultipartFile[]) arg;
                    FileInfo[] fileInfos = new FileInfo[files.length];
                    for (int j = 0; j < files.length; j++) {
                        if (files[j] != null) {
                            fileInfos[j] = new FileInfo(files[j].getOriginalFilename(), files[j].getSize(), files[j].getContentType());
                        }
                    }
                    processedArgs[i] = fileInfos;
                } else if (arg instanceof javax.servlet.http.HttpServletRequest) {
                    // 处理 HttpServletRequest
                    processedArgs[i] = "HttpServletRequest";
                } else if (arg instanceof javax.servlet.http.HttpServletResponse) {
                    // 处理 HttpServletResponse
                    processedArgs[i] = "HttpServletResponse";
                } else if (arg instanceof java.io.InputStream) {
                    // 处理 InputStream
                    processedArgs[i] = "InputStream";
                } else if (arg instanceof java.io.OutputStream) {
                    // 处理 OutputStream
                    processedArgs[i] = "OutputStream";
                } else if (arg instanceof java.io.Reader) {
                    // 处理 Reader
                    processedArgs[i] = "Reader";
                } else if (arg instanceof java.io.Writer) {
                    // 处理 Writer
                    processedArgs[i] = "Writer";
                } else {
                    // 其他类型直接使用原对象
                    processedArgs[i] = arg;
                }
            }
            
            return JsonUtils.toJsonString(processedArgs);
        } catch (Exception e) {
            log.warn("序列化请求参数失败，使用简化信息: {}", e.getMessage());
            // 如果序列化失败，返回简化信息
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                if (args[i] == null) {
                    sb.append("null");
                } else {
                    sb.append(args[i].getClass().getSimpleName());
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * 文件信息类，用于记录文件上传信息
     */
    private static class FileInfo {
        private String originalFilename;
        private long size;
        private String contentType;
        
        public FileInfo(String originalFilename, long size, String contentType) {
            this.originalFilename = originalFilename;
            this.size = size;
            this.contentType = contentType;
        }
        
        // Getters
        public String getOriginalFilename() { return originalFilename; }
        public long getSize() { return size; }
        public String getContentType() { return contentType; }
    }

    /**
     * 从 Tag 注解获取操作模块
     */
    private String getModuleFromTag(Method method) {
        // 优先从方法上的 Tag 注解获取
        if (method.isAnnotationPresent(io.swagger.v3.oas.annotations.tags.Tag.class)) {
            io.swagger.v3.oas.annotations.tags.Tag tag = method.getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
            return tag.name();
        }
        
        // 从类上的 Tag 注解获取
        Class<?> clazz = method.getDeclaringClass();
        if (clazz.isAnnotationPresent(io.swagger.v3.oas.annotations.tags.Tag.class)) {
            io.swagger.v3.oas.annotations.tags.Tag tag = clazz.getAnnotation(io.swagger.v3.oas.annotations.tags.Tag.class);
            return tag.name();
        }
        
        // 从类名推断模块名
        String className = clazz.getSimpleName();
        if (className.endsWith("Controller")) {
            return className.substring(0, className.length() - 10);
        }
        
        return "未知模块";
    }

    /**
     * 获取操作类型
     */
    private String getOperationType(Method method) {
        // 优先从 @Operation 注解的 summary 属性获取操作类型
        if (method.isAnnotationPresent(io.swagger.v3.oas.annotations.Operation.class)) {
            io.swagger.v3.oas.annotations.Operation operation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
            String summary = operation.summary();
            if (summary != null && !summary.trim().isEmpty()) {
                return summary.trim();
            }
        }
        
        // 如果 @Operation 注解没有 summary 或为空，则从方法名推断操作类型
        String methodName = method.getName().toLowerCase();
        
        if (methodName.contains("create") || methodName.contains("add") || methodName.contains("save")) {
            return "新增";
        } else if (methodName.contains("update") || methodName.contains("modify")) {
            return "修改";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "删除";
        } else if (methodName.contains("get") || methodName.contains("query") || methodName.contains("page")) {
            return "查询";
        } else if (methodName.contains("export")) {
            return "导出";
        } else if (methodName.contains("import")) {
            return "导入";
        } else if (methodName.contains("upload")) {
            return "上传";
        } else if (methodName.contains("download")) {
            return "下载";
        }
        
        return "其他";
    }

    /**
     * 记录操作日志
     */
    private void recordOperationLog(String module, String operationType, String requestParams, 
                                   String responseResult, ProceedingJoinPoint joinPoint, long executionTime) {
        try {
            // 获取当前登录用户信息
            String userId = "";
            String userName = "";
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId != null) {
                userId = String.valueOf(loginUserId);
                // 通过 AdminUserApi 获取用户信息，关联 system_users 表
                AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
                if (user != null) {
                    userName = user.getNickname(); // 获取用户昵称
                }
            }
            
            // 获取IP地址
            String ipAddress = getClientIP();
            
            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = method.getDeclaringClass().getName();
            String methodName = method.getName();
            
            // 限制参数长度，避免数据库字段过长
            if (requestParams != null && requestParams.length() > 2000) {
                requestParams = requestParams.substring(0, 2000) + "...";
            }
            if (responseResult != null && responseResult.length() > 2000) {
                responseResult = responseResult.substring(0, 2000) + "...";
            }
            
            // 异步记录日志
            operationLogService.createOperationLogAsync(
                module, operationType, requestParams, responseResult,
                className, methodName, userId, userName, ipAddress
            );
            
        } catch (Exception e) {
            log.error("记录操作日志时发生异常", e);
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIP() {
        try {
            String ip = ServletUtils.getClientIP();
            
            // 处理 IPv6 本地回环地址
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                return "127.0.0.1";
            }
            
            // 如果获取到的IP为空或无效，尝试其他方式
            if (ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                // 尝试从请求头中获取真实IP
                javax.servlet.http.HttpServletRequest request = ServletUtils.getRequest();
                if (request != null) {
                    // 常见的代理服务器IP头
                    String[] headers = {
                        "X-Forwarded-For",
                        "X-Real-IP", 
                        "X-Client-IP",
                        "X-Forwarded",
                        "X-Cluster-Client-IP",
                        "Proxy-Client-IP",
                        "WL-Proxy-Client-IP",
                        "HTTP_X_FORWARDED_FOR",
                        "HTTP_X_FORWARDED",
                        "HTTP_X_CLUSTER_CLIENT_IP",
                        "HTTP_CLIENT_IP",
                        "HTTP_FORWARDED_FOR",
                        "HTTP_FORWARDED",
                        "HTTP_VIA",
                        "REMOTE_ADDR"
                    };
                    
                    for (String header : headers) {
                        String value = request.getHeader(header);
                        if (value != null && !value.trim().isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                            // 如果是多个IP，取第一个
                            if (value.contains(",")) {
                                value = value.split(",")[0].trim();
                            }
                            // 验证IP格式
                            if (isValidIP(value)) {
                                return value;
                            }
                        }
                    }
                }
                return "未知IP";
            }
            
            return ip;
        } catch (Exception e) {
            log.warn("获取客户端IP地址失败", e);
            return "获取失败";
        }
    }

    /**
     * 验证IP地址格式
     */
    private boolean isValidIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        // IPv4 格式验证
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (ip.matches(ipv4Pattern)) {
            return true;
        }
        
        // IPv6 格式验证（简化版）
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        if (ip.matches(ipv6Pattern)) {
            return true;
        }
        
        return false;
    }
}
