package com.example.job.service;

import com.example.job.domain.message.MessageText;

import java.util.List;

public interface IMessageTextService {

    boolean batchInsert(List<MessageText> messageTextList);
}
