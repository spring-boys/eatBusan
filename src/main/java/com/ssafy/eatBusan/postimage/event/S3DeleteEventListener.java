package com.ssafy.eatBusan.postimage.event;

import com.ssafy.eatBusan.global.storage.s3.S3Service;
import com.ssafy.eatBusan.postimage.dto.S3DeleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3DeleteEventListener {

    private final S3Service s3Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(S3DeleteEvent event) {
        for (String key : event.keys()) {
            try {
                s3Service.deletePostImage(key);
            } catch (Exception e) {
                log.warn("S3 삭제 실패 key={}", key, e);
            }
        }
    }
}
