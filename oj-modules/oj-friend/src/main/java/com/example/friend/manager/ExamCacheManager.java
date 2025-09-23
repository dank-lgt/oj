package com.example.friend.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.commom.core.constans.CacheConstants;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.enums.ExamListType;
import com.example.commom.core.enums.ResultCode;
import com.example.common.redis.service.RedisService;
import com.example.common.security.exception.ServiceException;
import com.example.friend.domain.exam.Exam;
import com.example.friend.domain.exam.ExamQuestion;
import com.example.friend.domain.exam.dto.ExamQueryDTO;
import com.example.friend.domain.exam.dto.ExamRankDTO;
import com.example.friend.domain.exam.vo.ExamRankVO;
import com.example.friend.domain.exam.vo.ExamVO;
import com.example.friend.domain.user.UserExam;
import com.example.friend.mapper.exam.ExamMapper;
import com.example.friend.mapper.exam.ExamQuestionMapper;
import com.example.friend.mapper.user.UserExamMapper;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 考试缓存管理器，负责处理考试相关的缓存操作
 */
@Component
public class ExamCacheManager {

    @Autowired
    private ExamMapper examMapper; // 考试数据访问层

    @Autowired
    private ExamQuestionMapper examQuestionMapper; // 考试题数据访问层

    @Autowired
    private UserExamMapper userExamMapper; // 用户考试数据访问层

    @Autowired
    private RedisService redisService; // Redis服务层

    /**
     * 获取考试列表的大小
     *
     * @param examListType 考试列表类型
     * @param userId       用户ID
     * @return 列表大小
     */
    public Long getListSize(Integer examListType, Long userId) {
        String examListKey = getExamListKey(examListType, userId);
        return redisService.getListSize(examListKey);
    }

    /**
     * 获取考试题目列表的大小
     *
     * @param examId 考试ID
     * @return 题目列表大小
     */
    public Long getExamQuestionListSize(Long examId) {
        String examQuestionListKey = getExamQuestionListKey(examId);
        return redisService.getListSize(examQuestionListKey);
    }

    /**
     * 获取考试排行榜列表的大小
     *
     * @param examId 考试ID
     * @return 排行榜列表大小
     */
    public Long getRankListSize(Long examId) {
        return redisService.getListSize(getExamRankListKey(examId));
    }

    /**
     * 获取考试VO列表
     *
     * @param examQueryDTO 考试查询条件
     * @param userId       用户ID
     * @return 考试VO列表
     */
    public List<ExamVO> getExamVOList(ExamQueryDTO examQueryDTO, Long userId) {
        // 计算分页起始位置
        int start = (examQueryDTO.getPageNum() - 1) * examQueryDTO.getPageSize();
        // 计算分页结束位置（注意数组下标需要-1）
        int end = start + examQueryDTO.getPageSize() - 1; //下标需要 -1
        // 获取考试列表的缓存键
        String examListKey = getExamListKey(examQueryDTO.getType(), userId);
        // 从Redis中获取指定范围的考试ID列表
        List<Long> examIdList = redisService.getCacheListByRange(examListKey, start, end, Long.class);
        // 组装考试VO列表
        List<ExamVO> examVOList = assembleExamVOList(examIdList);
        if (CollectionUtil.isEmpty(examVOList)) {
            //说明redis中数据可能有问题 从数据库中查数据并且重新刷新缓存
            examVOList = getExamListByDB(examQueryDTO, userId); //从数据库中获取数据
            refreshCache(examQueryDTO.getType(), userId);
        }
        return examVOList;
    }


    /**
     * 获取考试排名列表
    // 计算查询的起始位置，公式：(当前页码-1) * 每页大小
    // 计算查询的结束位置，公式：起始位置 + 每页大小 - 1（因为数组下标从0开始，所以需要减1）
     * @param examRankDTO 考试排名查询条件对象，包含页码和每页大小等信息
    // 从Redis缓存中获取指定范围的考试排名列表
     * @return 返回考试排名列表，包含考生排名信息
     */
    public List<ExamRankVO> getExamRankList(ExamRankDTO examRankDTO) {
        int start = (examRankDTO.getPageNum() - 1) * examRankDTO.getPageSize();
        int end = start + examRankDTO.getPageSize() - 1; //下标需要 -1
        return redisService.getCacheListByRange(getExamRankListKey(examRankDTO.getExamId()), start, end, ExamRankVO.class);
    }

    /**
     * 获取用户所有考试列表
     *
     * @param userId 用户ID
     * @return 用户考试ID列表
     */
    public List<Long> getAllUserExamList(Long userId) {
        String examListKey = CacheConstants.USER_EXAM_LIST + userId;
        List<Long> userExamIdList = redisService.getCacheListByRange(examListKey, 0, -1, Long.class);
        if (CollectionUtil.isNotEmpty(userExamIdList)) {
            return userExamIdList;
        }
        List<UserExam> userExamList =
                userExamMapper.selectList(new LambdaQueryWrapper<UserExam>().eq(UserExam::getUserId, userId));
        if (CollectionUtil.isEmpty(userExamList)) {
            return null;
        }
        refreshCache(ExamListType.USER_EXAM_LIST.getValue(), userId);
        return userExamList.stream().map(UserExam::getExamId).collect(Collectors.toList());
    }

    /**
     * 添加用户考试缓存
     *
     * @param userId 用户ID
     * @param examId 考试ID
     */
    public void addUserExamCache(Long userId, Long examId) {
        String userExamListKey = getUserExamListKey(userId);
        redisService.leftPushForList(userExamListKey, examId);
    }

    /**
     * 获取考试的第一道题目
     *
     * @param examId 考试ID
     * @return 第一道题目的ID
     */
    public Long getFirstQuestion(Long examId) {
        return redisService.indexForList(getExamQuestionListKey(examId), 0, Long.class);
    }

    /**
     * 获取当前题目的上一道题目
     *
     * @param examId     考试ID
     * @param questionId 当前题目ID
     * @return 上一道题目的ID
     */
    public Long preQuestion(Long examId, Long questionId) {
        Long index = redisService.indexOfForList(getExamQuestionListKey(examId), questionId);
        if (index == 0) {
            throw new ServiceException(ResultCode.FAILED_FIRST_QUESTION);
        }
        return redisService.indexForList(getExamQuestionListKey(examId), index - 1, Long.class);
    }

    /**
     * 获取当前题目的下一道题目
     *
     * @param examId     考试ID
     * @param questionId 当前题目ID
     * @return 下一道题目的ID
     */
    public Long nextQuestion(Long examId, Long questionId) {
        Long index = redisService.indexOfForList(getExamQuestionListKey(examId), questionId);
        long lastIndex = getExamQuestionListSize(examId) - 1;
        if (index == lastIndex) {
            throw new ServiceException(ResultCode.FAILED_LAST_QUESTION);
        }
        return redisService.indexForList(getExamQuestionListKey(examId), index + 1, Long.class);
    }

    //刷新缓存逻辑
    public void refreshCache(Integer examListType, Long userId) {
        List<Exam> examList = new ArrayList<>();
        if (ExamListType.EXAM_UN_FINISH_LIST.getValue().equals(examListType)) {
            //查询未完赛的竞赛列表
            examList = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                    .select(Exam::getExamId, Exam::getTitle, Exam::getStartTime, Exam::getEndTime)
                    .gt(Exam::getEndTime, LocalDateTime.now())
                    .eq(Exam::getStatus, Constants.TRUE)
                    .orderByDesc(Exam::getCreateTime));
        } else if (ExamListType.EXAM_HISTORY_LIST.getValue().equals(examListType)) {
            //查询历史竞赛
            examList = examMapper.selectList(new LambdaQueryWrapper<Exam>()
                    .select(Exam::getExamId, Exam::getTitle, Exam::getStartTime, Exam::getEndTime)
                    .le(Exam::getEndTime, LocalDateTime.now())
                    .eq(Exam::getStatus, Constants.TRUE)
                    .orderByDesc(Exam::getCreateTime));
        } else if (ExamListType.USER_EXAM_LIST.getValue().equals(examListType)) {
            List<ExamVO> examVOList = userExamMapper.selectUserExamList(userId);
            examList = BeanUtil.copyToList(examVOList, Exam.class);
        }
        if (CollectionUtil.isEmpty(examList)) {
            return;
        }

        Map<String, Exam> examMap = new HashMap<>();
        List<Long> examIdList = new ArrayList<>();
        for (Exam exam : examList) {
            examMap.put(getDetailKey(exam.getExamId()), exam);
            examIdList.add(exam.getExamId());
        }
        redisService.multiSet(examMap);  //刷新详情缓存
        redisService.deleteObject(getExamListKey(examListType, userId));
        redisService.rightPushAll(getExamListKey(examListType, userId), examIdList);      //刷新列表缓存
    }

/**
 * 刷新考试题目缓存的方法
 * @param examId 考试ID，用于标识需要刷新哪个考试的题目缓存
 */
    public void refreshExamQuestionCache(Long examId) {
    // 从数据库中查询指定考试的所有题目ID列表，并按照题目顺序排序
        List<ExamQuestion> examQuestionList = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .select(ExamQuestion::getQuestionId)  // 只查询题目ID字段
                .eq(ExamQuestion::getExamId, examId)  // 筛选指定考试的题目
                .orderByAsc(ExamQuestion::getQuestionOrder));  // 按题目顺序升序排列
    // 如果查询结果为空，则直接返回
        if (CollectionUtil.isEmpty(examQuestionList)) {
            return;
        }
    // 将题目ID列表转换为Long类型列表
        List<Long> examQuestionIdList = examQuestionList.stream().map(ExamQuestion::getQuestionId).toList();
    // 将题目ID列表存入Redis列表的右侧
        redisService.rightPushAll(getExamQuestionListKey(examId), examQuestionIdList);
        //节省 redis缓存资源
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),
                LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        redisService.expire(getExamQuestionListKey(examId), seconds, TimeUnit.SECONDS);
    }

    /**
     * 刷新考试排名缓存
     *
     * @param examId 考试ID，用于查询考试排名数据并作为缓存key的一部分
     */
    public void refreshExamRankCache(Long examId) {
        // 查询考试排名列表数据
        List<ExamRankVO> examRankVOList = userExamMapper.selectExamRankList(examId);
        // 如果查询结果为空，则直接返回，不进行缓存操作
        if (CollectionUtil.isEmpty(examRankVOList)) {
            return;
        }
        // 将考试排名数据批量推入Redis缓存中
        redisService.rightPushAll(getExamRankListKey(examId), examRankVOList);
    }


    private List<ExamVO> getExamListByDB(ExamQueryDTO examQueryDTO, Long userId) {
        PageHelper.startPage(examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
        if (ExamListType.USER_EXAM_LIST.getValue().equals(examQueryDTO.getType())) {
            //查询我的竞赛列表
            return userExamMapper.selectUserExamList(userId);
        } else {
            //查询C端的竞赛列表
            return examMapper.selectExamList(examQueryDTO);
        }
    }

    private List<ExamVO> assembleExamVOList(List<Long> examIdList) {
        if (CollectionUtil.isEmpty(examIdList)) {
            //说明redis当中没数据 从数据库中查数据并且重新刷新缓存
            return null;
        }
        //拼接redis当中key的方法 并且将拼接好的key存储到一个list中
        List<String> detailKeyList = new ArrayList<>();
        for (Long examId : examIdList) {
            detailKeyList.add(getDetailKey(examId));
        }
        List<ExamVO> examVOList = redisService.multiGet(detailKeyList, ExamVO.class);
        CollUtil.removeNull(examVOList);
        if (CollectionUtil.isEmpty(examVOList) || examVOList.size() != examIdList.size()) {
            //说明redis中数据有问题 从数据库中查数据并且重新刷新缓存
            return null;
        }
        return examVOList;
    }

    private String getExamListKey(Integer examListType, Long userId) {
        if (ExamListType.EXAM_UN_FINISH_LIST.getValue().equals(examListType)) {
            return CacheConstants.EXAM_UNFINISHED_LIST;
        } else if (ExamListType.EXAM_HISTORY_LIST.getValue().equals(examListType)) {
            return CacheConstants.EXAM_HISTORY_LIST;
        } else {
            return CacheConstants.USER_EXAM_LIST + userId;
        }
    }

    private String getDetailKey(Long examId) {
        return CacheConstants.EXAM_DETAIL + examId;
    }

    private String getUserExamListKey(Long userId) {
        return CacheConstants.USER_EXAM_LIST + userId;
    }

    private String getExamQuestionListKey(Long examId) {
        return CacheConstants.EXAM_QUESTION_LIST + examId;
    }

    private String getExamRankListKey(Long examId) {
        return CacheConstants.EXAM_RANK_LIST + examId;
    }
}