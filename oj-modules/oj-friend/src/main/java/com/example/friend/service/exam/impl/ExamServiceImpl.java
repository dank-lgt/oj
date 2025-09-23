package com.example.friend.service.exam.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.domain.TableDataInfo;
import com.example.commom.core.utils.ThreadLocalUtil;
import com.example.friend.domain.exam.dto.ExamQueryDTO;
import com.example.friend.domain.exam.dto.ExamRankDTO;
import com.example.friend.domain.exam.vo.ExamRankVO;
import com.example.friend.domain.exam.vo.ExamVO;
import com.example.friend.domain.user.vo.UserVO;
import com.example.friend.manager.ExamCacheManager;
import com.example.friend.manager.UserCacheManager;
import com.example.friend.mapper.exam.ExamMapper;
import com.example.friend.mapper.user.UserExamMapper;
import com.example.friend.service.exam.IExamService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 竞赛服务实现类
 * 实现了IExamService接口，提供竞赛相关的业务逻辑
 */
@Service
@Slf4j
public class ExamServiceImpl implements IExamService {

    /** 竞赛数据访问层 */
    @Autowired
    private ExamMapper examMapper;

    /** 竞赛缓存管理器 */
    @Autowired
    private ExamCacheManager examCacheManager;

    /** 用户缓存管理器 */
    @Autowired
    private UserCacheManager userCacheManager;

    /** 用户竞赛数据访问层 */
    @Autowired
    private UserExamMapper userExamMapper;

    /**
     * 获取竞赛列表
     * @param examQueryDTO 竞赛查询条件
     * @return 竞赛列表
     */
    @Override
    public List<ExamVO> list(ExamQueryDTO examQueryDTO) {
        PageHelper.startPage(examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
        return examMapper.selectExamList(examQueryDTO);
    }

    /**
     * 从Redis中获取竞赛列表数据
     * @param examQueryDTO 竞赛查询条件DTO
     * @return TableDataInfo 包含竞赛列表和总数的数据结构
     */
    @Override
    public TableDataInfo redisList(ExamQueryDTO examQueryDTO) {
        //从redis当中获取  竞赛列表的数据
        Long total = examCacheManager.getListSize(examQueryDTO.getType(), null); // 从缓存中获取列表总数
        List<ExamVO> examVOList; // 声明竞赛VO列表变量
        if (total == null || total <= 0) { // 判断缓存中是否有数据
            examVOList = list(examQueryDTO); // 如果缓存中没有数据，则从数据库获取
            examCacheManager.refreshCache(examQueryDTO.getType(), null); // 刷新缓存
            total = new PageInfo<>(examVOList).getTotal(); // 获取总数
        } else { // 如果缓存中有数据
            examVOList = examCacheManager.getExamVOList(examQueryDTO, null); // 从缓存中获取竞赛列表
            total = examCacheManager.getListSize(examQueryDTO.getType(), null); // 获取缓存中的总数
        }
        if (CollectionUtil.isEmpty(examVOList)) { // 判断列表是否为空
            return TableDataInfo.empty(); // 如果为空，返回空数据结构
        }
        assembleExamVOList(examVOList); // 组装竞赛VO列表数据
        return TableDataInfo.success(examVOList, total); // 返回成功响应，包含竞赛列表和总数
    }


    /**
     * 获取考试排名列表
     * @param examRankDTO 考试排名查询条件对象，包含考试ID、页码、每页大小等信息
     * @return TableDataInfo 包含排名列表和总数的数据对象
     */
    @Override
    public TableDataInfo rankList(ExamRankDTO examRankDTO) {
        // 从缓存中获取排名列表的总数
        Long total = examCacheManager.getRankListSize(examRankDTO.getExamId());
        List<ExamRankVO> examRankVOList;
        // 判断缓存中是否存在数据或数据是否有效
        if (total == null || total <= 0) {
            // 如果缓存中没有数据，则从数据库查询
            PageHelper.startPage(examRankDTO.getPageNum(), examRankDTO.getPageSize());
            examRankVOList = userExamMapper.selectExamRankList(examRankDTO.getExamId());
            // 刷新考试排名缓存
            examCacheManager.refreshExamRankCache(examRankDTO.getExamId());
            // 获取查询结果的总数
            total = new PageInfo<>(examRankVOList).getTotal();
        } else {
            // 如果缓存中有数据，则直接从缓存获取
            examRankVOList = examCacheManager.getExamRankList(examRankDTO);
        }
        // 判断查询结果是否为空
        if (CollectionUtil.isEmpty(examRankVOList)) {
            return TableDataInfo.empty();
        }
        // 处理排名列表数据
        assembleExamRankVOList(examRankVOList);
        // 返回成功响应，包含排名列表和总数
        return TableDataInfo.success(examRankVOList, total);
    }

    @Override
    public String getFirstQuestion(Long examId) {
        checkAndRefresh(examId);
        return examCacheManager.getFirstQuestion(examId).toString();
    }

    @Override
    public String preQuestion(Long examId, Long questionId) {
        checkAndRefresh(examId);
        return examCacheManager.preQuestion(examId, questionId).toString();
    }

    @Override
    public String nextQuestion(Long examId, Long questionId) {
        checkAndRefresh(examId);
        return examCacheManager.nextQuestion(examId, questionId).toString();
    }

    private void assembleExamVOList(List<ExamVO> examVOList) {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        List<Long> userExamIdList = examCacheManager.getAllUserExamList(userId);
        if (CollectionUtil.isEmpty(userExamIdList)) {
            return;
        }
        for (ExamVO examVO : examVOList) {
            if (userExamIdList.contains(examVO.getExamId())) {
                examVO.setEnter(true);
            }
        }
    }

    private void assembleExamRankVOList(List<ExamRankVO> examRankVOList) {
        if (CollectionUtil.isEmpty(examRankVOList)) {
            return;
        }
        for (ExamRankVO examRankVO : examRankVOList) {
            Long userId = examRankVO.getUserId();
            UserVO user = userCacheManager.getUserById(userId);
            examRankVO.setNickName(user.getNickName());
        }
    }

    private void checkAndRefresh(Long examId) {
        Long listSize = examCacheManager.getExamQuestionListSize(examId);
        if (listSize == null || listSize <= 0) {
            examCacheManager.refreshExamQuestionCache(examId);
        }
    }
}
