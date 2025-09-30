package com.example.friend.controller.question.file;


import com.example.commom.core.domain.R;
import com.example.commom.core.enums.ResultCode;
import com.example.common.file.domain.OSSResult;
import com.example.common.file.service.OSSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private OSSService ossService;

    @PostMapping("/upload")
    public R<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        OSSResult result = ossService.uploadFile(file);
        if (result.isSuccess()) {
            // 返回包含预签名URL的结果
            Map<String, String> data = new HashMap<>();
            data.put("fileName", result.getName());
            data.put("url", result.getUrl()); // 预签名URL
            data.put("fileId", result.getFileId()); // 文件ID
            return R.ok(data);
        }
        return R.fail(ResultCode.FAILED_FILE_UPLOAD);
    }

    @GetMapping("/url")
    public R<Map<String, String>> getFileUrl(@RequestParam String fileId) {
        try {
            String url = ossService.generatePresignedUrl(fileId);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail(ResultCode.FAILED_FILE_ACCESS);
        }
    }

    @GetMapping("/url/long-term")
    public R<Map<String, String>> getLongTermUrl(@RequestParam String objectKey) {
        try {
            String url = ossService.generateLongTermUrl(objectKey);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail(ResultCode.FAILED_FILE_ACCESS);
        }
    }

    @GetMapping("/url/refresh")
    public R<Map<String, String>> refreshFileUrl(@RequestParam String fileId) {
        try {
            String url = ossService.refreshResignedUrl(fileId);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail(ResultCode.FAILED_FILE_ACCESS);
        }
    }

    @GetMapping("/check")
    public R<Map<String, Object>> checkFileExists(@RequestParam String objectKey) {
        try {
            String url = ossService.getFileUrlIfExists(objectKey);
            Map<String, Object> data = new HashMap<>();
            data.put("exists", url != null);
            data.put("url", url);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail(ResultCode.FAILED_FILE_CHECK);
        }
    }
}
