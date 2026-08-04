package com.example.matjib.service;

import com.example.matjib.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 업로드된 이미지를 로컬 폴더에 저장하고, 웹에서 접근할 경로("/uploads/파일명")를 반환.
     * 파일이 없으면 null 반환.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "이미지 파일만 업로드할 수 있습니다.");
        }

        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            // 원본 확장자 유지, 파일명은 UUID로 (중복/한글 문제 방지)
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String savedName = UUID.randomUUID() + ext;

            Path target = dir.resolve(savedName);
            file.transferTo(target.toFile());
            log.info("파일 저장: {}", target);

            return "/uploads/" + savedName;
        } catch (IOException e) {
            log.error("파일 저장 실패", e);
            throw new BusinessException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 저장 중 오류가 발생했습니다.");
        }
    }
}
