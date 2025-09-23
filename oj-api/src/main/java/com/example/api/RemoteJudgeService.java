package com.example.api;



import com.example.api.domain.dto.JudgeSubmitDTO;
import com.example.api.domain.vo.UserQuestionResultVO;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "RemoteJudgeService", value = Constants.JUDGE_SERVICE)
public interface RemoteJudgeService {

    @PostMapping("/judge/doJudgeJavaCode")
    R<UserQuestionResultVO> doJudgeJavaCode(@RequestBody JudgeSubmitDTO judgeSubmitDTO);
}
