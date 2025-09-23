package com.example.friend.service.user.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.domain.TableDataInfo;
import com.example.commom.core.enums.ExamListType;
import com.example.commom.core.enums.ResultCode;
import com.example.commom.core.utils.ThreadLocalUtil;
import com.example.common.security.exception.ServiceException;
import com.example.common.security.service.TokenService;
import com.example.friend.domain.exam.Exam;
import com.example.friend.domain.exam.dto.ExamQueryDTO;
import com.example.friend.domain.exam.vo.ExamVO;
import com.example.friend.domain.user.UserExam;
import com.example.friend.manager.ExamCacheManager;
import com.example.friend.manager.UserCacheManager;
import com.example.friend.mapper.exam.ExamMapper;
import com.example.friend.mapper.user.UserExamMapper;
import com.example.friend.service.user.IUserExamService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UserExamService implements IUserExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserExamMapper userExamMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ExamCacheManager examCacheManager;

    @Value("${jwt.secret}")
    private String secret;

    private UserCacheManager userCacheManager;



    /**
     * 处理用户进入考试的方法
     * @param token 用户认证令牌
     * @param examId 考试ID
     * @return int 返回插入用户考试记录的结果
     */
    @Override
    public int enter(String token, Long examId) {
        //获取当前用户的信息   status
//        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
//        UserVO user = userCacheManager.getUserById(userId);
//        if (user.getStatus() == 0) {
//            throw new ServiceException(ResultCode.FAILED_USER_BANNED);
//        }

        // 检查考试是否存在
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new ServiceException(ResultCode.FAILED_NOT_EXISTS);
        }

        // 检查考试是否已经开始
        if(exam.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ResultCode.EXAM_STARTED);
        }

//        Long userId = tokenService.getUserId(token, secret);
        // 从线程本地变量中获取用户ID
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);

        // 检查用户是否已经进入过该考试
        UserExam userExam = userExamMapper.selectOne(new LambdaQueryWrapper<UserExam>()
                .eq(UserExam::getExamId, examId)
                .eq(UserExam::getUserId, userId));
        if (userExam != null) {
            throw new ServiceException(ResultCode.USER_EXAM_HAS_ENTER);
        }

        // 将用户考试关系添加到缓存
        examCacheManager.addUserExamCache(userId, examId);

        // 创建并插入用户考试记录
        userExam = new UserExam();
        userExam.setExamId(examId);
        userExam.setUserId(userId);
        return userExamMapper.insert(userExam);
    }


    //先查询缓存（u:e:l:用户id）  如果缓存能够查询到
    //如果查询不到   数据库当中再去查询  并且将数据库中的数据同步给redis
    @Override
    public TableDataInfo list(ExamQueryDTO examQueryDTO) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        examQueryDTO.setType(ExamListType.USER_EXAM_LIST.getValue());
        Long total = examCacheManager.getListSize(ExamListType.USER_EXAM_LIST.getValue(), userId);
        List<ExamVO> examVOList;
        if (total == null || total <= 0) {
            //从数据库中查询我的竞赛列表
            PageHelper.startPage(examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
            examVOList = userExamMapper.selectUserExamList(userId);
            examCacheManager.refreshCache(ExamListType.USER_EXAM_LIST.getValue(), userId);
            total = new PageInfo<>(examVOList).getTotal();
        } else {
            examVOList = examCacheManager.getExamVOList(examQueryDTO, userId);
            total = examCacheManager.getListSize(examQueryDTO.getType(), userId);
        }
        if (CollectionUtil.isEmpty(examVOList)) {
            return TableDataInfo.empty();
        }
        return TableDataInfo.success(examVOList, total);
    }
}
