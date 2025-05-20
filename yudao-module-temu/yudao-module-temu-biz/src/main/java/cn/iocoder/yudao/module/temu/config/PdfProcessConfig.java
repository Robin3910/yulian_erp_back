package cn.iocoder.yudao.module.temu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * PDF处理线程池配置类
 * @author ykh
 */
@Configuration
@EnableAsync // 启用Spring异步任务支持
@Slf4j
public class PdfProcessConfig {

    /**
     * 创建PDF处理专用线程池
     * 线程池行为规则：
     * - 当核心线程全忙时，新任务进入队列（最多500个待处理任务）
     * - 当队列满后，扩容线程数至maxPoolSize（最大3线程）
     * - 使用CallerRunsPolicy，在调用者线程中执行任务，当队列满时，任务会在调用者线程中执行，
     * 而不是直接拒绝，这样可以确保任务不会丢失，只是执行会变慢
     * @return 线程池执行器实例
     */
    @Bean(name = "pdfProcessExecutor")
    public Executor pdfProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：考虑到2核CPU，设置为3个线程（2个工作线程+1个接收线程）
        executor.setCorePoolSize(2);
        // 最大线程数：与核心线程数保持一致，避免创建过多线程
        executor.setMaxPoolSize(2);
        // 队列容量：设置较大的队列，确保任务不丢失
        executor.setQueueCapacity(5000);
        // 线程名前缀
        executor.setThreadNamePrefix("pdf-process-");
        // 拒绝策略：使用CallerRunsPolicy，在调用者线程中执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（设置较长的等待时间，确保任务能够完成）
        executor.setAwaitTerminationSeconds(7200);
        // 初始化
        executor.initialize();
        return executor;
    }
}