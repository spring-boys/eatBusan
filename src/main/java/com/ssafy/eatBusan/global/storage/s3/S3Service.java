package com.ssafy.eatBusan.global.storage.s3;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.global.storage.s3.dto.S3UploadResult;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public S3UploadResult uploadPostImage(Long postId, MultipartFile file) {
        validateImageFile(file);

        String imageKey = createImageKey(postId, file.getOriginalFilename());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(imageKey)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

        try {
            s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new EBException(ErrorCode.IMAGE_UPLOAD_FAILURE);
        }

        return new S3UploadResult(createImageUrl(imageKey), imageKey);
    }

    public void deletePostImage(String imageKey) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .build()
        );
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EBException(ErrorCode.EMPTY_IMAGE_FILE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new EBException(ErrorCode.NOT_IMAGE_FILE);
        }
    }

    private String createImageKey(Long postId, String originalFilename) {
        return "posts/" + postId + "/" + UUID.randomUUID() + getExtension(originalFilename);
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }

        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
    }

    private String createImageUrl(String imageKey) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + imageKey;
    }
}