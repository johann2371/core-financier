package com.corefi.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {
    String storeProfilePhoto(MultipartFile file);
    String storeFile(MultipartFile file, String subDir);
    void deleteFile(String filename);
}
