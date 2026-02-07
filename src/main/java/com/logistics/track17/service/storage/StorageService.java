package com.logistics.track17.service.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件存储服务接口
 * 支持本地存储和MinIO对象存储
 */
public interface StorageService {

    /**
     * 上传文件
     * 
     * @param file        文件输入流
     * @param fileName    文件名
     * @param contentType 内容类型
     * @return 存储路径
     */
    String upload(InputStream file, String fileName, String contentType) throws Exception;

    /**
     * 下载文件到临时目录
     * 
     * @param storagePath 存储路径
     * @return 本地临时文件路径
     */
    Path download(String storagePath) throws Exception;

    /**
     * 删除文件
     * 
     * @param storagePath 存储路径
     */
    void delete(String storagePath) throws Exception;

    /**
     * 获取文件URL（预签名）
     * 
     * @param storagePath   存储路径
     * @param expireSeconds 过期时间（秒）
     * @return 可访问的URL
     */
    String getPresignedUrl(String storagePath, int expireSeconds) throws Exception;

    /**
     * 检查文件是否存在
     */
    boolean exists(String storagePath) throws Exception;
}
