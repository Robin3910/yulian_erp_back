package cn.iocoder.yudao.module.temu.utils.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 限速输入流
 * 通过控制读取速度来实现带宽限制
 */
public class ThrottledInputStream extends InputStream {
    private final InputStream source;
    private final int bytesPerSecond;
    private long lastReadTime;
    private long bytesReadInSecond;

    public ThrottledInputStream(InputStream source, int bytesPerSecond) {
        this.source = source;
        this.bytesPerSecond = bytesPerSecond;
        this.lastReadTime = System.currentTimeMillis();
        this.bytesReadInSecond = 0;
    }

    @Override
    public int read() throws IOException {
        throttle(1);
        int data = source.read();
        if (data != -1) {
            bytesReadInSecond++;
        }
        return data;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        throttle(len);
        int bytesRead = source.read(b, off, len);
        if (bytesRead != -1) {
            bytesReadInSecond += bytesRead;
        }
        return bytesRead;
    }

    private void throttle(int requestedBytes) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastReadTime;

        // 每秒重置计数
        if (timeDiff >= 1000) {
            lastReadTime = currentTime;
            bytesReadInSecond = 0;
            return;
        }

        // 检查是否需要限速
        if (bytesReadInSecond + requestedBytes > bytesPerSecond) {
            try {
                // 计算需要等待的时间
                long sleepTime = 1000 - timeDiff;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                lastReadTime = System.currentTimeMillis();
                bytesReadInSecond = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}