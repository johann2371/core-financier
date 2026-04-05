package com.corefi.service.impl;

import com.corefi.exception.WorkflowException;
import com.corefi.service.interfaces.IFileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements IFileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new WorkflowException("Impossible de créer le répertoire racine de stockage.", ex);
        }
    }

    @Override
    public String storeProfilePhoto(MultipartFile file) {
        return storeFile(file, "profiles");
    }

    @Override
    public String storeFile(MultipartFile file, String subDir) {
        // Normaliser le nom du fichier
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            if (originalFileName.contains("..")) {
                throw new WorkflowException("Nom de fichier invalide : " + originalFileName);
            }

            // Créer le sous-répertoire s'il n'existe pas
            Path targetDir = this.fileStorageLocation.resolve(subDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Générer un nom unique
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Copier le fichier
            Path targetLocation = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return subDir + "/" + fileName;
        } catch (IOException ex) {
            throw new WorkflowException("Impossible de stocker le fichier " + originalFileName, ex);
        }
    }

    @Override
    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            // On log l'erreur mais on ne bloque pas si le fichier n'existe pas ou ne peut pas être supprimé
            System.err.println("Erreur lors de la suppression du fichier : " + filename);
        }
    }
}
