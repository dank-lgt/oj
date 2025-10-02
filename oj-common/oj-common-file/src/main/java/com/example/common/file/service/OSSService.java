package com.example.common.file.service;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.ObjectId;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.example.commom.core.constans.CacheConstants;
import com.example.commom.core.constans.Constants;
import com.example.commom.core.enums.ResultCode;
import com.example.commom.core.utils.ThreadLocalUtil;
import com.example.common.file.config.OSSProperties;
import com.example.common.file.domain.OSSResult;
import com.example.common.redis.service.RedisService;
import com.example.common.security.exception.ServiceException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RefreshScope
public class OSSService {

    @Autowired
    private OSSProperties prop;

    @Autowired
    public OSSClient ossClient;

    @Autowired
    private RedisService redisService;

    @Value("${file.max-time}")
    private int maxTime;

    @Value("${file.test}")
    private boolean test;

    // 预签名URL过期时间配置
    @Value("${file.presigned-url.expiration:3600}")
    private Long UrlExpiration; // 默认1小时，单位秒

    public OSSResult uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    public OSSResult uploadFile(MultipartFile file, Set<String> allowedExtensions) {
        if (!test) {
            checkUploadCount();
        }

        validateFile(file);

        try (InputStream inputStream = file.getInputStream()) {
            String extName = getFileExtension(file);

            // 验证文件扩展名
            if (allowedExtensions != null && !allowedExtensions.contains(extName.toLowerCase())) {
                throw new ServiceException(ResultCode.FAILED_FILE_TYPE_NOT_ALLOWED);
            }
            log.info("Uploading file: {}", file.getOriginalFilename());
            return upload(extName, inputStream);
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to read file stream: {}", file.getOriginalFilename(), e);
            throw new ServiceException(ResultCode.FAILED_FILE_READ);
        } catch (Exception e) {
            log.error("OSS upload file error, filename: {}", file.getOriginalFilename(), e);
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD);
        }
    }

    /**
     * 验证文件基本属性
     */
    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD_EMPTY);
        }

        if (file.isEmpty()) {
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD_EMPTY);
        }
    }

    private void checkUploadCount() {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        Long times = redisService.getCacheMapValue(CacheConstants.USER_UPLOAD_TIMES_KEY, String.valueOf(userId), Long.class);
        if (times != null && times >= maxTime) {
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD_TIME_LIMIT);
        }
        redisService.incrementHashValue(CacheConstants.USER_UPLOAD_TIMES_KEY, String.valueOf(userId), 1);
        if (times == null || times == 0) {
            long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
            redisService.expire(CacheConstants.USER_UPLOAD_TIMES_KEY, seconds, TimeUnit.SECONDS);
        }
    }

    private OSSResult upload(String fileType, InputStream inputStream) {
        // 直接使用文件ID作为对象键，不包含路径信息
        String fileId = ObjectId.next(); // 生成文件ID
        String fileExtension = "." + fileType;
        String key = prop.getPathPrefix()+fileId + fileExtension; //
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setObjectAcl(CannedAccessControlList.PublicRead);
        PutObjectRequest request = new PutObjectRequest(prop.getBucketName(), key, inputStream, objectMetadata);
        PutObjectResult putObjectResult;

        try {
            putObjectResult = ossClient.putObject(request);
        } catch (Exception e) {
            log.error("OSS put object error: {}", ExceptionUtil.stacktraceToOneLineString(e, 500));
            throw new ServiceException(ResultCode.FAILED_FILE_UPLOAD);
        }
        return assembleOSSResult(fileId, key, putObjectResult);
    }

    private OSSResult assembleOSSResult(String fileId, String objectKey, PutObjectResult putObjectResult) {
        OSSResult ossResult = new OSSResult();
        if (putObjectResult == null || StrUtil.isBlank(putObjectResult.getRequestId())) {
            ossResult.setSuccess(false);
        } else {
            ossResult.setSuccess(true);
            ossResult.setFilename(FileUtil.getName(objectKey));
            ossResult.setFileId(fileId); // 返回文件ID
            ossResult.setUrl(generatePresignedUrl(objectKey));
        }
        return ossResult;
    }

    // 后续操作直接使用 fileId + 扩展名
    public void deleteFile(String fileId, String fileType) {
        String objectKey = fileId + "." + fileType;
        ossClient.deleteObject(prop.getBucketName(), objectKey);
    }

    public String refreshUrl(String fileId, String fileType) {
        String objectKey = fileId + "." + fileType;
        return generatePresignedUrl(objectKey);
    }

    /**
     * 生成预签名URL - 主要方法
     * @param fileName  文件名
     * @param expiration 过期时间
     * @return 预签名URL
     */
    public String generatePresignedUrl(String fileName, Duration expiration) {
        try {
            // 如果 fileId 不包含路径前缀，自动添加
            String key = fileName;
            if (!key.startsWith(prop.getPathPrefix())) {
                key = prop.getPathPrefix() + fileName;
            }
            // 将Duration转换为Date
            Date expirationDate = new Date(System.currentTimeMillis() + expiration.toMillis());
            return ossClient.generatePresignedUrl(prop.getBucketName(), key, expirationDate).toString();
        } catch (Exception e) {
            log.error("Generate presigned URL error for fileId: {}", fileName, e);
            throw new ServiceException(ResultCode.FAILED_FILE_ACCESS);
        }
    }

    /**
     * 生成预签名URL - 便捷方法（使用默认过期时间）
     */
    public String generatePresignedUrl(String fileName) {
        return generatePresignedUrl(fileName, Duration.ofSeconds(UrlExpiration));
    }

    /**
     * 生成短期预签名URL（用于敏感操作）
     */
    public String generateShortTermUrl(String fileId) {
        return generatePresignedUrl(fileId, Duration.ofMinutes(30)); // 30分钟
    }

    /**
     * 生成长期预签名URL（用于前端展示）
     */
    public String generateLongTermUrl(String fileId) {
        return generatePresignedUrl(fileId, Duration.ofDays(7)); // 7天
    }

    /**
     * 根据fileId重新生成预签名URL（用于刷新过期链接）
     */
    public String refreshResignedUrl(String fileId) {
        return generatePresignedUrl(fileId);
    }

    /**
     * 验证对象是否存在并生成URL
     */
    public String getFileUrlIfExists(String fileId) {
        try {
            boolean exists = ossClient.doesObjectExist(prop.getBucketName(), fileId);
            if (exists) {
                return generatePresignedUrl(fileId);
            }
            return null;
        } catch (Exception e) {
            log.error("检查文件是否存在失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (StrUtil.isBlank(originalFilename)) {
            return "file";
        }

        String extName = FileUtil.extName(originalFilename);

        if (StrUtil.isBlank(extName)) {
            return "file";
        }

        return extName.toLowerCase();
    }

}