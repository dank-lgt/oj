package com.example.judge.callback;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Statistics;
import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;
import java.io.IOException;

/**
 * 统计回调类，用于处理和统计内存使用情况
 * 该类实现了ResultCallback接口，用于接收和处理Statistics类型的统计信息
 * 使用了Lombok的@Getter和@Setter注解，自动为所有字段生成getter和setter方法
 */
@Getter
@Setter
public class StatisticsCallback implements ResultCallback<Statistics> {

    /**
     * 记录程序运行过程中的最大内存使用量
     * 初始化值为0L，单位为字节
     */
    private Long maxMemory = 0L;

    /**
     * 当开始接收统计信息时被调用
     * @param closeable 可关闭的资源，用于在需要时关闭连接
     */
    @Override
    //这个方法在开始接收统计信息时会被调用
    public void onStart(Closeable closeable) {

    }

    /**
     * 每次接收到新的统计信息时被调用
     * 该方法从Statistics对象中提取当前的最大内存使用量，并与现有的maxMemory值比较，
     * 保留较大的那个值。这样可以跟踪整个监控期间的最大内存使用情况。
     * @param statistics 包含内存统计信息的Statistics对象
     */
//    这个方法在每次接收到新的统计信息时被调用。
//    它从 Statistics 对象中提取当前的最大内存使用量，并与现有的 maxMemory 值比较，
//    保留较大的那个值。这样可以跟踪整个监控期间的最大内存使用情况。
    @Override
    public void onNext(Statistics statistics) {
        Long usage = statistics.getMemoryStats().getMaxUsage();//程序运行到某个时间点上的内存使用的最大值
        if (usage != null) {
            maxMemory = Math.max(usage, maxMemory);
        }
    }

    /**
     * 在获取统计信息过程中遇到错误时会被调用
     * @param throwable 捕获到的异常对象
     */
    //这个方法在获取统计信息过程中遇到错误时会被调用
    @Override
    public void onError(Throwable throwable) {

    }

    /**
     * 在所有的统计信息都被接收完毕后调用
     * 表示统计信息接收过程已完成
     */
    //这个方法在所有的统计信息都被接收完毕后调用
    @Override
    public void onComplete() {

    }

    /**
     * 用于清理资源的方法
     * 在不需要使用回调时释放相关资源
     * @throws IOException 如果关闭资源时发生I/O错误
     */
    //这个方法用于清理资源
    @Override
    public void close() throws IOException {

    }
}
