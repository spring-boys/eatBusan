package com.ssafy.eatBusan.postimage.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.global.storage.s3.S3Service;
import com.ssafy.eatBusan.global.storage.s3.dto.S3UploadResult;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postimage.dto.PostImageDto;
import com.ssafy.eatBusan.postimage.dto.S3DeleteEvent;
import com.ssafy.eatBusan.postimage.mapper.PostImageMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostImageService {

    private final PostRepository postRepository;
    private final PostImageMapper postImageMapper;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    public List<PostImageDto> findImages(Long postId) {
        validatePost(postId);

        return postImageMapper.findByPostId(postId);
    }

    private void validatePost(Long postId) {
        postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(() -> new EBException(ErrorCode.POST_NOT_FOUND));
    }


    @Transactional
    public List<PostImageDto> uploadImages(Long postId, List<MultipartFile> files) {
        validatePost(postId);
        for (int i = 0; i < files.size(); i++) {
            S3UploadResult result = s3Service.uploadPostImage(postId, files.get(i));

            postImageMapper.saveImage(postId, result.imageUrl(), result.imageKey(), i);
        }
        return postImageMapper.findByPostId(postId);
    }


    @Transactional
    public void deleteImage(Long postId, Long imageId) {
        validatePost(postId);
        PostImageDto image = postImageMapper.findByPostIdAndImageId(postId,
            imageId);

        if (image == null) {
            throw new EBException(ErrorCode.POST_IMAGE_NOT_FOUND);
        }

        postImageMapper.deleteImage(imageId);
        eventPublisher.publishEvent(new S3DeleteEvent(List.of(image.imageKey())));
    }
}
