# 개발 변경사항 기록

## 2026-05-18 (feat/post)

### Member 도메인 버그 수정

원래 Member 담당자 작업이나, Post 개발을 위해 불가피하게 수정. 담당자 확인 필요.

| 파일 | 수정 내용 |
|------|-----------|
| `MemberController.java` | `@RestController` 누락으로 인한 404 수정 |
| `MemberService.java` | `join()` 로직 반전 버그 수정 (`orElseThrow` → `ifPresent`), `save()` 누락 추가 |
| `ErrorCode.java` | `MEMBER_NOT_FOUND` 추가 |

---

### application.properties 수정

WSL2 환경에서 MySQL 8 연결 시 발생하는 인증 오류 수정.

```properties
# 추가된 파라미터
createDatabaseIfNotExist=true
allowPublicKeyRetrieval=true
```

---

### Post 도메인 신규 구현

| 파일 | 내용 |
|------|------|
| `post/domain/Post.java` | Post 엔티티 (id, user_id FK, title, content, view/like/comment count, soft delete) |
| `post/dto/PostRequireDto.java` | 게시글 작성 요청 DTO (userId, email, title, content) |
| `post/dto/PostResponseDto.java` | 게시글 응답 DTO (from() 정적 팩토리 포함) |
| `post/repository/PostRepository.java` | `findAllByDeletedFalse()` soft delete 필터링 |
| `post/service/PostService.java` | `getAllPost()`, `writePost()` 구현. 클래스 레벨 `@Transactional(readOnly=true)` |
| `post/controller/PostController.java` | `GET /api/posts`, `POST /api/posts/regist` |
| `src/test/http/post.http` | HTTP 테스트 파일 (회원가입, 게시글 등록, 목록 조회) |

#### 현재 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/posts` | 게시글 목록 조회 |
| POST | `/api/posts/regist` | 게시글 등록 |

#### 미구현 (다음 작업)

- `GET /api/v1/posts/{postId}` 상세 조회 + viewCount 증가
- `PATCH /api/v1/posts/{postId}` 수정
- `DELETE /api/v1/posts/{postId}` 소프트 삭제
- 인증: 세션 담당자 완성 전까지 `@RequestHeader("X-User-Id") Long userId` 임시 사용 예정

---

### PostPlace 엔티티 신규 구현

게시글(Post) ↔ 식당(Place) N:M 관계를 위한 중간 엔티티.

| 파일 | 내용 |
|------|------|
| `postplace/domain/PostPlace.java` | post_place 테이블 매핑. post_id(FK), place_id(FK), soft delete 포함 |

```
post (1) ──── post_place ──── (1) place
```

---

### 주요 설계 결정

- **인증 임시 처리**: 세션 구현 담당자와 병렬 작업을 위해 `@RequestHeader("X-User-Id")`로 임시 처리, 세션 완성 후 교체
- **카테고리 제거**: PostCategory enum 및 category 필드 기획에서 제외
- **소프트 삭제**: 모든 도메인에서 `deleted = b'0'` 원칙 준수, DELETE 쿼리 사용 금지
