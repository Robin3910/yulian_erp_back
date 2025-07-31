package cn.iocoder.yudao.module.temu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class TemuAsyncConfiguration {
    
    /**
     * 物流校验专用线程池
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                 // 核心线程数
        executor.setMaxPoolSize(5);                  // 最大线程数
        executor.setQueueCapacity(100);             // 队列容量
        executor.setKeepAliveSeconds(60);           // 线程空闲时间
        executor.setThreadNamePrefix("logistics-validation-");  // 线程名前缀
        // 拒绝策略：由调用线程处理（一般是主线程）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
} 