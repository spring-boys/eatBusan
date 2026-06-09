package com.ssafy.eatBusan.postimage.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.global.storage.s3.S3Service;
import com.ssafy.eatBusan.global.storage.s3.dto.S3UploadResult;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postimage.dto.PostImageDto;
import com.ssafy.eatBusan.postimage.mapper.PostImageMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class PostImageService {

    private final PostRepository postRepository;
    private final PostImageMapper postImageMapper;
    private final S3Service s3Service;

    public List<PostImageDto> findImages(Long postId) {
        validatePost(postId);

        return postImageMapper.findByPostId(postId);
    }

    private void validatePost(Long postId) {
        postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(() -> new EBException(ErrorCode.POST_NOT_FOUND));
    }

    public List<PostImageDto> uploadImages(Long postId, List<MultipartFile> files) {
        validatePost(postId);
        for (int i = 0; i < files.size(); i++) {
            S3UploadResult result = s3Service.uploadPostImage(postId, files.get(i));

            postImageMapper.saveImage(
                postId,
                result.imageUrl(),
                result.imageKey(),
                i
            );
        }
        return postImageMapper.findByPostId(postId);
    }
}
