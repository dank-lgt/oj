package com.example.job.service;



import com.example.job.domain.message.Message;

import java.util.List;

public interface IMessageService {

    boolean batchInsert(List<Message> messageTextList);
}
