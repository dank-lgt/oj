package com.example.friend.rabbit;

import com.example.api.domain.dto.JudgeSubmitDTO;
import com.example.commom.core.constans.RabbitMQConstants;
import com.example.commom.core.enums.ResultCode;
import com.example.common.security.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JudgeProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void produceMsg(JudgeSubmitDTO judgeSubmitDTO) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConstants.OJ_WORK_QUEUE, judgeSubmitDTO);
        } catch (Exception e) {
            log.error("生产者发送消息异常", e);
            throw new ServiceException(ResultCode.FAILED_RABBIT_PRODUCE);
        }
    }
}
