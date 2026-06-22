# S3 게시글 이미지 업로드를 위한 학습 가이드

이 문서는 `feat/post-image-upload` 브랜치에서 구현한 **게시글 이미지 S3 업로드** 코드를 이해하고 리뷰/면접에서 설명할 수 있도록, 필요한 개념을 공식 문서 기준으로 정리한다. 챕터 순서대로 읽고, 각 챕터의 "공식 문서"를 직접 한 번씩 열어보는 것이 핵심이다.

대상 코드:
- `global/config/S3Config.java` — S3Client 빈
- `global/storage/s3/S3Service.java` — 실제 업로드(`putObject`)
- `global/storage/s3/dto/S3UploadResult.java` — 업로드 결과(url, key)
- `postimage/**` — 도메인 / 매퍼 / 서비스 / 컨트롤러
- `post/controller/PostController.java`, `post/service/PostService.java` — 게시글 작성 시 이미지 연동

---

## 학습 순서 한눈에 보기

```
1. S3가 무엇인가 — Object Storage / Bucket / Key / Object
2. 버킷과 키(Key) 설계 — "폴더"는 없다, prefix가 있을 뿐
3. 인증 — IAM, Access Key, CredentialsProvider
4. AWS SDK for Java v2 — S3Client, PutObjectRequest, RequestBody
5. 업로드 방식 — 서버 경유(현재) vs Presigned URL
6. 객체 접근 권한 — Block Public Access / Bucket Policy / Presigned GET
7. ContentType과 메타데이터
8. 자격증명 보안 — 환경변수, 절대 커밋 금지
9. 우리 구현과의 매핑 + 개선 포인트 (총정리)
```

---

## 1. S3가 무엇인가

### 핵심
- **Object Storage** (파일 시스템도 블록 스토리지도 아님)
- 저장 단위는 **Object** = `데이터(바이트) + 메타데이터 + Key`
- Object들은 **Bucket**(전역 고유 이름의 컨테이너) 안에 들어간다
- 한 Object 최대 5TB, 단일 PUT 업로드는 5GB까지(그 이상은 Multipart Upload)

### 우리 프로젝트에서 왜 쓰는가
- 사용자가 올린 게시글 이미지를 **서버 디스크/DB에 두지 않고** S3에 저장
- DB(`post_image` 테이블)에는 **파일 자체가 아니라 URL과 key만** 저장 → DB 가벼움, 정적 파일은 S3가 서빙

### 공식 문서
- S3 개요: https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html

### 학습 체크포인트
- [ ] "S3에는 폴더가 없다"는 말의 의미를 설명할 수 있는가
- [ ] 우리가 DB에 이미지 바이트가 아니라 url/key만 저장하는 이유를 한 문장으로 말할 수 있는가

---

## 2. 버킷과 키(Key) 설계

### 핵심
- S3에 **폴더는 실제로 없다.** `posts/1/abc.jpg`에서 `posts/1/`은 폴더가 아니라 **Key 문자열의 일부(prefix)**
- 콘솔이 폴더처럼 보여주는 건 `/` 구분자 기준 UI 트릭일 뿐
- Key는 버킷 내에서 고유해야 하고, 같은 Key로 PUT하면 **덮어쓰기(overwrite)**

### 우리 프로젝트 코드
`S3Service.createImageKey()`:
```java
"posts/" + postId + "/" + UUID.randomUUID() + getExtension(originalFilename)
// 예: posts/42/3f9c....e1.jpg
```
- `postId`로 게시글별 그룹핑(prefix) → 조회/관리 편함
- `UUID`로 파일명 충돌 방지 + 원본 파일명 노출 방지
- 확장자만 원본에서 따옴(`getExtension`)

### 공식 문서
- Key 이름 가이드: https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html

### 학습 체크포인트
- [ ] 원본 파일명 대신 UUID를 쓰면 어떤 문제(2가지)가 해결되는가
- [ ] 같은 Key로 두 번 업로드하면 어떻게 되는가

---

## 3. 인증 — IAM, Access Key, CredentialsProvider

### 핵심
- AWS API 호출은 모두 **자격증명(credentials)** 으로 서명되어야 한다
- **IAM User/Role**에 권한(예: `s3:PutObject`)을 부여하고, 그 자격으로 호출
- 자격증명 = **Access Key ID + Secret Access Key**
- SDK는 `CredentialsProvider`로 자격증명을 가져온다
  - `StaticCredentialsProvider` — 코드/설정에서 직접 주입 (우리 방식)
  - `DefaultCredentialsProvider` — 환경변수 → 프로파일 → EC2/ECS 역할 순으로 자동 탐색 (운영 권장)

### 우리 프로젝트 코드
`S3Config.java`:
```java
AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
S3Client.builder()
    .region(Region.of(region))
    .credentialsProvider(StaticCredentialsProvider.create(credentials))
    .build();
```
- `accessKey`/`secretKey`는 `@Value("${AWS_ACCESS_KEY_ID}")` 등 **환경변수**에서 주입
- region은 `application.properties`의 `aws.s3.region`(기본 `ap-northeast-2` 서울)

> 개선 여지: EC2/ECS에 배포한다면 키를 직접 들고 다니는 대신 **IAM Role + DefaultCredentialsProvider**가 더 안전하다. 키 노출/회전 부담이 사라진다.

### 공식 문서
- SDK 자격증명: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html
- 최소 권한 IAM 정책 예: `s3:PutObject`, `s3:GetObject` on `arn:aws:s3:::버킷/*`

### 학습 체크포인트
- [ ] StaticCredentialsProvider와 DefaultCredentialsProvider의 차이를 말할 수 있는가
- [ ] 우리 코드에서 secret key가 소스에 하드코딩되어 있지 않은 이유를 설명할 수 있는가

---

## 4. AWS SDK for Java v2 — 핵심 3종

### 핵심
업로드 한 번에 세 가지가 등장한다:
- **`S3Client`** — API 호출 진입점(빈으로 등록해 재사용)
- **`PutObjectRequest`** — "무엇을 어디에" (bucket, key, contentType, contentLength)
- **`RequestBody`** — "실제 바이트" (InputStream / byte[] / File)

### 우리 프로젝트 코드
`S3Service.uploadPostImage()`:
```java
PutObjectRequest req = PutObjectRequest.builder()
    .bucket(bucket)
    .key(imageKey)
    .contentType(file.getContentType())
    .contentLength(file.getSize())
    .build();

s3Client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
```
- `MultipartFile`(스프링이 받은 업로드 파일) → `getInputStream()`으로 바이트 스트림 전달
- `contentLength`를 명시 → S3가 스트림 길이를 알 수 있음

### 공식 문서
- S3 객체 업로드(SDK v2): https://docs.aws.amazon.com/AmazonS3/latest/userguide/upload-objects.html
- SDK v1 vs v2 차이: v2는 `S3Client`/builder 패턴, non-blocking 지원

### 학습 체크포인트
- [ ] `PutObjectRequest`와 `RequestBody`의 역할 분담을 설명할 수 있는가
- [ ] `MultipartFile`은 누가(어느 레이어) 만들어 주는가

---

## 5. 업로드 방식 — 서버 경유 vs Presigned URL  ⭐리뷰 단골 질문

### 핵심
| | 서버 경유 (현재 구현) | Presigned URL |
|---|---|---|
| 경로 | 클라 → **우리 서버** → S3 | 클라 → **S3 직접** |
| 서버가 하는 일 | 파일 바이트를 받아 `putObject` | 업로드용 URL만 발급 |
| 서버 부하/대역폭 | 파일이 서버를 통과 | 거의 없음 |
| 검증 | 서버에서 자유롭게(타입/크기) | URL 발급 조건으로 제한 |
| 클라 구현 | 단순(한 번 전송) | 2단계(URL 요청 → S3로 PUT) |

### 우리 프로젝트 코드
**서버 경유 방식**이다.
- `PostController`/`PostImageController`가 `multipart/form-data`로 `MultipartFile` 수신
- `S3Service.putObject`로 서버가 직접 S3에 올림
- 흐름: `클라이언트 → Spring 서버 → S3`

### Presigned URL이란
- 서버가 SDK로 "이 key에 N분간 PUT 가능"한 **서명된 임시 URL**을 만들어 클라에 줌
- 클라는 그 URL로 S3에 직접 업로드 → **대용량/대량 트래픽에 유리**

### 공식 문서
- Presigned URL: https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html

### 학습 체크포인트
- [ ] "왜 presigned 안 썼어요?"에 답할 수 있는가 (힌트: 서버 검증 일괄 처리 + 구현 단순. 트래픽/대용량 이슈 시 전환 고려)
- [ ] presigned 방식의 업로드 흐름을 2단계로 그릴 수 있는가

---

## 6. 객체 접근 권한 — 이미지를 어떻게 "보이게" 하나

### 핵심
- 기본적으로 S3 객체는 **비공개**
- 공개 방법:
  1. **Bucket Policy / Block Public Access 해제** → 누구나 GET (정적 이미지 호스팅)
  2. **Presigned GET URL** → 일정 시간만 유효한 조회 URL
  3. **CloudFront(CDN) + OAC** → 권장 운영 패턴(캐싱 + 버킷은 비공개 유지)
- ⚠️ ACL(객체별 public-read)은 AWS가 더 이상 권장하지 않음

### 우리 프로젝트 코드
`S3Service.createImageUrl()`:
```java
"https://" + bucket + ".s3." + region + ".amazonaws.com/" + imageKey
```
- **가상 호스팅 스타일의 정적 객체 URL**을 그대로 DB(`image_url`)에 저장
- 즉 이 URL이 브라우저에서 열리려면 **버킷/객체가 public-read거나 CloudFront로 노출**되어 있어야 한다 → 인프라 설정 전제

### 공식 문서
- 퍼블릭 액세스 차단: https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html

### 학습 체크포인트
- [ ] 우리가 저장하는 URL이 브라우저에서 열리려면 무엇이 전제되어야 하는가
- [ ] 비공개 이미지를 보여줘야 한다면(예: 인증된 사용자만) 어떤 방식이 맞는가

---

## 7. ContentType과 메타데이터

### 핵심
- 업로드 시 `Content-Type`을 지정하지 않으면 브라우저가 이미지를 **다운로드**해버리거나 잘못 렌더링할 수 있다
- `contentType(file.getContentType())`로 `image/png` 등을 박아주면 브라우저가 인라인으로 표시

### 우리 프로젝트 코드
- `S3Service.validateImageFile()`가 `contentType`이 `image/`로 시작하는지 검증 → 이미지만 허용
- 같은 값을 `PutObjectRequest.contentType()`에 사용

> 주의: `getContentType()`은 클라가 보낸 헤더라 **신뢰할 수 없다.** 더 엄격히 하려면 매직 넘버(파일 시그니처) 검사가 정석.

### 학습 체크포인트
- [ ] ContentType을 안 박으면 어떤 문제가 생기는가
- [ ] 클라가 보낸 ContentType을 100% 믿으면 안 되는 이유는

---

## 8. 자격증명 보안

### 핵심
- Access Key/Secret을 **소스에 커밋하면 사고.** GitHub에 올라가는 순간 봇이 수초 내 스캔
- 우리 코드는 `@Value("${AWS_ACCESS_KEY_ID}")`로 **환경변수 주입** → 소스에 비밀 없음 ✅
- 운영에선 키 대신 **IAM Role**, 비밀 관리엔 **AWS Secrets Manager / SSM Parameter Store**

### 학습 체크포인트
- [ ] `application.properties`에 `${AWS_S3_BUCKET}`처럼 쓰는 이유는
- [ ] 만약 secret key가 깃에 한번 올라갔다면 해야 할 1순위 조치는 (힌트: 회전/폐기)

---

## 9. 우리 구현과의 매핑 + 개선 포인트 (총정리)

### 전체 흐름 (멀티파트 게시글 작성)
```
POST /api/posts (multipart: post + files)
  └ PostController.createPostWithImages
     └ PostService.writePostWithImages
        ├ savePost()                 : JPA로 post 저장
        └ uploadImagesIfPresent()
           └ PostImageService.uploadImages
              ├ for each file:
              │   ├ S3Service.uploadPostImage  → putObject → S3UploadResult(url, key)
              │   └ PostImageMapper.saveImage  → MyBatis INSERT into post_image
              └ findByPostId() 로 결과 반환
```
- 단독 업로드 경로도 있음: `POST /api/posts/{postId}/images` (`PostImageController`)
- **JPA로 쓰고(Post/PostImage 도메인) MyBatis로 읽고/넣는(PostImageMapper)** 혼용 구조 → 학습 포인트

### 알아두면 좋은 개선 포인트 (리뷰에서 나올 수 있음)
1. **트랜잭션과 S3의 불일치**: `@Transactional` 안에서 S3 업로드 후 DB INSERT가 터지면, DB는 롤백돼도 **S3 객체는 남는다(orphan)**. 외부 I/O는 트랜잭션이 롤백해주지 못함 → 보상 트랜잭션/정리 배치 고려.
2. **부분 실패**: 파일 3개 중 2번째에서 예외 → 이미 올라간 1개는 어떻게? 현재는 정리 로직 없음.
3. **검증 강화**: 파일 크기 상한, 확장자/매직넘버 검증, 개수 제한.
4. **자격증명**: 배포 환경에선 IAM Role 기반으로 전환 검토.
5. **접근 제어**: public 버킷 직접 노출 대신 CloudFront(+OAC) 고려.

### 최종 체크포인트 (이걸 다 말할 수 있으면 PR 설명 준비 완료)
- [ ] 업로드 요청 한 건의 전체 흐름을 레이어별로 설명할 수 있다
- [ ] 서버 경유 방식인 이유와 presigned와의 트레이드오프를 말할 수 있다
- [ ] DB에는 무엇이 저장되고(why url+key), 파일 자체는 어디 있는지 안다
- [ ] 트랜잭션 롤백 시 S3 orphan 문제를 인지하고 있다
- [ ] 자격증명이 소스에 없고 환경변수로 주입됨을 설명할 수 있다

---

## 참고 링크 모음
- S3 사용자 가이드: https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html
- AWS SDK for Java 2.x: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html
- Presigned URL: https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html
- IAM 최소 권한: https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html