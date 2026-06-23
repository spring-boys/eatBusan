package com.ssafy.eatBusan.voteroom.dto;

import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.voteroom.domain.VoteCandidate;

// 프론트 리스팅 카드 레이아웃용 후보 정보.
// placeName은 방 생성 시점 스냅샷(VoteCandidate), 나머지 place 정보는 place 테이블 join 결과.
// place 행이 없으면(삭제 등) place 관련 필드는 모두 null.
public record CandidateResponse(
        Long candidateId,
        Long placeId,
        String placeName,
        String address,
        String category,
        String phone,
        String url,
        Double x,
        Double y
) {

    // place 정보 없이(스냅샷만). place join 불가/불필요 시 사용.
    public static CandidateResponse from(VoteCandidate candidate) {
        return new CandidateResponse(
                candidate.getId(), candidate.getPlaceId(), candidate.getPlaceName(),
                null, null, null, null, null, null);
    }

    // place 테이블 join 결과를 포함한다. place가 null이면 from(candidate)와 동일.
    public static CandidateResponse from(VoteCandidate candidate, Place place) {
        if (place == null) {
            return from(candidate);
        }
        // category 컬럼은 place 테이블에 없으므로 null. (필드는 프론트 호환을 위해 유지)
        return new CandidateResponse(
                candidate.getId(), candidate.getPlaceId(), candidate.getPlaceName(),
                place.getAddress(), null, place.getPhone(), place.getUrl(),
                place.getX(), place.getY());
    }
}
