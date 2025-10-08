package com.example.judge.service.impl;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.constans.JudgeConstants;
import com.example.commom.core.enums.CodeRunStatus;
import com.example.judge.callback.DockerStartResultCallback;
import com.example.judge.callback.StatisticsCallback;
import com.example.judge.config.DockerSandBoxPool;
import com.example.judge.domain.CompileResult;
import com.example.judge.domain.SandBoxExecuteResult;
import com.example.judge.service.ISandboxPoolService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.StatsCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 沙箱池服务实现类，提供代码执行环境的管理和代码执行功能
 */
@Service
@Slf4j
public class SandboxPoolServiceImpl implements ISandboxPoolService {

    @Autowired
    private DockerSandBoxPool sandBoxPool; // Docker沙箱池，用于管理Docker容器

    @Autowired
    private DockerClient dockerClient; // Docker客户端，用于与Docker容器交互

    private String containerId; // 当前使用的容器ID

    private String userCodeFileName; // 用户代码文件名

    @Value("${sandbox.limit.time:5}")
    private Long timeLimit; // 代码执行时间限制，默认为5秒

    /**
     * 执行Java代码的主方法
     * @param userId 用户ID
     * @param userCode 用户提交的Java代码
     * @param inputList 输入用例列表
     * @return 执行结果对象，包含编译状态、执行状态、输出结果等信息
     */
    @Override
    public SandBoxExecuteResult exeJavaCode(Long userId, String userCode, List<String> inputList) {
        containerId = sandBoxPool.getContainer(); // 从沙箱池获取一个容器
        createUserCodeFile(userCode); // 创建用户代码文件
        //编译代码
        CompileResult compileResult = compileCodeByDocker();
        if (!compileResult.isCompiled()) {
            sandBoxPool.returnContainer(containerId); // 归还容器
            deleteUserCodeFile(); // 删除用户代码文件
            return SandBoxExecuteResult.fail(CodeRunStatus.COMPILE_FAILED, compileResult.getExeMessage());
        }
        //执行代码
        return executeJavaCodeByDocker(inputList);
    }

    //创建并返回用户代码的文件
    private void createUserCodeFile(String userCode) {
        String codeDir = sandBoxPool.getCodeDir(containerId);
        log.info("user-pool路径信息：{}", codeDir);
        userCodeFileName = codeDir + File.separator + JudgeConstants.USER_CODE_JAVA_CLASS_NAME;
        //如果文件之前存在，将之前的文件删除掉
        if (FileUtil.exist(userCodeFileName)) {
            FileUtil.del(userCodeFileName);
        }
        FileUtil.writeString(userCode, userCodeFileName, Constants.UTF8);
    }

    //编译
    //的使用docker编译
    private CompileResult compileCodeByDocker() {
        String cmdId = createExecCmd(JudgeConstants.DOCKER_JAVAC_CMD, null, containerId);
        DockerStartResultCallback resultCallback = new DockerStartResultCallback();
        CompileResult compileResult = new CompileResult();
        try {
            dockerClient.execStartCmd(cmdId)
                    .exec(resultCallback)
                    .awaitCompletion();
            if (CodeRunStatus.FAILED.equals(resultCallback.getCodeRunStatus())) {
                compileResult.setCompiled(false);
                compileResult.setExeMessage(resultCallback.getErrorMessage());
            } else {
                compileResult.setCompiled(true);
            }
            return compileResult;
        } catch (InterruptedException e) {
            //此处可以直接抛出 已做统一异常处理  也可再做定制化处理
            throw new RuntimeException(e);
        }
    }

    private SandBoxExecuteResult executeJavaCodeByDocker(List<String> inputList) {
        List<String> outList = new ArrayList<>(); //记录输出结果
        long maxMemory = 0L;  //最大占用内存
        long maxUseTime = 0L; //最大运行时间
        //执行用户代码
        for (String inputArgs : inputList) {
            String cmdId = createExecCmd(JudgeConstants.DOCKER_JAVA_EXEC_CMD, inputArgs, containerId);
            //执行代码
            StopWatch stopWatch = new StopWatch();        //执行代码后开始计时
            //执行情况监控
            StatsCmd statsCmd = dockerClient.statsCmd(containerId); //启动监控
            StatisticsCallback statisticsCallback = statsCmd.exec(new StatisticsCallback());
            stopWatch.start();
            DockerStartResultCallback resultCallback = new DockerStartResultCallback();
            try {
                dockerClient.execStartCmd(cmdId)
                        .exec(resultCallback)
                        .awaitCompletion(timeLimit, TimeUnit.SECONDS);
                if (CodeRunStatus.FAILED.equals(resultCallback.getCodeRunStatus())) {
                    //未通过所有用例返回结果
                    return SandBoxExecuteResult.fail(CodeRunStatus.NOT_ALL_PASSED);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            stopWatch.stop();  //结束时间统计
            statsCmd.close();  //结束docker容器执行统计
            long userTime = stopWatch.getLastTaskTimeMillis(); //执行耗时
            maxUseTime = Math.max(userTime, maxUseTime);       //记录最大的执行用例耗时
            Long memory = statisticsCallback.getMaxMemory();
            if (memory != null) {
                maxMemory = Math.max(maxMemory, statisticsCallback.getMaxMemory()); //记录最大的执行用例占用内存
            }
            outList.add(resultCallback.getMessage().trim());   //记录正确的输出结果
        }
        sandBoxPool.returnContainer(containerId);
        deleteUserCodeFile(); //清理文件

        return getSanBoxResult(inputList, outList, maxMemory, maxUseTime); //封装结果
    }

    private String createExecCmd(String[] javaCmdArr, String inputArgs, String containerId) {
        if (!StrUtil.isEmpty(inputArgs)) {
            //当入参不为空时拼接入参
            String[] inputArray = inputArgs.split(" "); //入参
            javaCmdArr = ArrayUtil.append(JudgeConstants.DOCKER_JAVA_EXEC_CMD, inputArray);
        }
        ExecCreateCmdResponse cmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(javaCmdArr)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .exec();
        return cmdResponse.getId();
    }

    private SandBoxExecuteResult getSanBoxResult(List<String> inputList, List<String> outList,
                                                 long maxMemory, long maxUseTime) {
        if (inputList.size() != outList.size()) {
            //输入用例数量 不等于 输出用例数量  属于执行异常
            return SandBoxExecuteResult.fail(CodeRunStatus.NOT_ALL_PASSED, outList, maxMemory, maxUseTime);
        }
        return SandBoxExecuteResult.success(CodeRunStatus.SUCCEED, outList, maxMemory, maxUseTime);
    }

    private void deleteUserCodeFile() {
        FileUtil.del(userCodeFileName);
    }
}
