package com.corefi.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {
    String storeProfilePhoto(MultipartFile file);
    void deleteFile(String filename);
}
