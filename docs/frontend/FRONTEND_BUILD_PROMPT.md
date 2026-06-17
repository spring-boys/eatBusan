# 프론트엔드 구축 프롬프트 — 쩝쩝코치 (EatBusan)

> **이 문서의 용도:** Claude(또는 Claude Design)에 그대로 붙여넣어 프론트엔드 프로젝트를 시작하기 위한 자급식(self-contained) 프롬프트.
> **디자인(색/타이포/레이아웃/컴포넌트 비주얼)은 Claude Design에서 별도로 진행**하므로, 이 문서는 **API 연동·데이터·타입·상태·라우팅 등 "프론트가 백엔드 때문에 반드시 알아야 하는 기술적인 것"에만** 집중한다.
> 기준 시점의 실제 백엔드 코드(컨트롤러/DTO/필터/CORS)에서 추출한 값이다. 추측이 아니라 **현재 구현 그대로**이며, 의도적이지 않아 보이는 부분도 "백엔드가 지금 이렇게 동작한다"는 사실로 받아들이고 그에 맞춘다.

---

## 0. 너에게 (작업하는 Claude 세션에게)

너는 이 백엔드와 통신하는 **Vue 3 + TypeScript** 프론트엔드를 만든다. 비주얼 디자인 토큰은 별도(Claude Design / `DESIGN_SYSTEM.md`)에서 오므로, 너는 **API 클라이언트, 타입 정의, 인증 플로우, 상태관리, 라우팅, 기능별 폴더 구조**를 정확하게 구축하는 데 집중한다.

- API 응답 형태는 **추측하지 말고** 아래 스펙을 그대로 따른다. 스펙에 없으면 사용자에게 묻는다.
- 아래 ⚠️ 로 표시된 항목은 **백엔드의 현재 quirk**다. "고쳐서 맞추기" 전에 일단 **있는 그대로 통신되게** 구현하고, 이상한 점은 주석/TODO로 남긴다.
- "미구현"으로 표시된 기능은 백엔드에 엔드포인트가 없다 → UI는 만들되 mock/disabled 처리하고 명확히 표시한다.

---

## 1. 서비스 개요

부산광역시 **모범음식점** 데이터 기반 커뮤니티 서비스.

- **지역구(areaCode)별 모범음식점 탐색** → 식당 상세 → **후기(Post) 공유** → **댓글/좋아요**
- 로그인 사용자는 게시글 작성(이미지 업로드 포함), 댓글, 식당/게시글 좋아요 가능
- (기획상 QR 초대방 + WebSocket 실시간 기능이 언급되나 **백엔드 미구현** → 이번 범위 제외)

---

## 2. 기술 스택 (고정 — 임의 변경 금지)

| 영역 | 선택 |
|------|------|
| 프레임워크 | **Vue 3** (`<script setup>`, Composition API. Options API 금지) |
| 빌드 | **Vite** |
| 언어 | **TypeScript** (`any` 지양, 모든 API 응답은 타입 정의) |
| UI | **Vuetify** (버튼/입력/카드 등 직접 만들지 말 것) |
| 상태관리 | **Pinia** (전역만; 로컬은 `ref`/`reactive`) |
| 라우팅 | **Vue Router** |
| HTTP | **Axios** (컴포넌트 직접 호출 금지 → `shared/api` 인스턴스 + feature `api/` 경유) |

**폴더 구조 (feature-based):**
```
src/
  app/            # 부트스트랩 (router, pinia, vuetify 등록)
  router/
  shared/
    api/          # axios 인스턴스 + 인터셉터 (토큰 주입/리프레시/에러)
    types/        # 공통 타입 (SpringPage<T>, ApiError 등)
    composables/  # usePagination 등
    components/
  features/
    auth/  place/  placeLike/  post/  postImage/  comment/  postLike/
      api/ store/ components/ views/ types/
```
새 기능은 항상 `features/<name>/{api,store,components,views,types}` 5종 구조로 만든다.

---

## 3. 백엔드 연결 기본

- **Base URL (dev):** `http://localhost:8081`
- **공통 prefix:** `/api`
- **개발 시 Vite proxy** 권장: `/api` → `http://localhost:8081`
- **CORS:** 백엔드가 `allowCredentials(true)` + `exposedHeaders("Authorization")` 설정 → **axios에 반드시 `withCredentials: true`**. 허용 origin은 백엔드 `spring.front.domain` 값(보통 `http://localhost:5173`)과 일치해야 함.

---

## 4. 인증 (⭐ 가장 중요 — 비표준이므로 정확히)

토큰을 "응답 바디"로 주지 않는다. **액세스 토큰은 응답 헤더, 리프레시 토큰은 httpOnly 쿠키**로 오간다.

### 동작 방식
| 항목 | 위치 / 형식 |
|------|------------|
| **Access Token (받기)** | 로그인/리프레시 **응답의 `Authorization` 헤더**, 값 = `Bearer {token}` (CORS로 노출되어 JS에서 읽을 수 있음) |
| **Access Token (보내기)** | 모든 요청의 **요청 `Authorization` 헤더**, 값 = `Bearer {token}` |
| **Refresh Token** | **httpOnly 쿠키 `EBRefreshToken`** (JS 접근 불가, `withCredentials: true`면 자동 송수신) |
| **로그인 응답 바디** | **없음** (200). 토큰은 헤더/쿠키로만 옴 |

### 인증 엔드포인트 (`/api/members`)
| 동작 | Method | Path | 요청 | 성공 응답 | 부수효과 |
|------|--------|------|------|----------|----------|
| 회원가입 | POST | `/api/members/join` | `{ email, password }` | `201` (바디 없음, Location 헤더) | 토큰 발급 안 함 |
| 로그인 | POST | `/api/members/login` | `{ email, password }` | `200` (바디 없음) | `Authorization` 헤더 + `EBRefreshToken` 쿠키 세팅 |
| 토큰 재발급 | POST | `/api/members/refresh` | 바디 없음 (쿠키 자동) | `204` | 새 `Authorization` 헤더 + 새 쿠키 |
| 로그아웃 | POST | `/api/members/logout` | 헤더 토큰 필요 | `200` | 쿠키 만료 + 서버 토큰 삭제 |
| 내 정보 | GET | `/api/members/me` | 헤더 토큰 필요 | `200` `{ email }` | — |

### 프론트 구현 규칙
- 액세스 토큰은 **메모리(Pinia store) + 필요 시 localStorage 백업**에 보관. (httpOnly 쿠키가 아니라 헤더 방식이므로 JS가 직접 관리)
- 로그인 성공 시 응답에서 `response.headers['authorization']` 읽어 `Bearer ` 떼고 저장.
- **요청 인터셉터:** 저장된 토큰이 있으면 `Authorization: Bearer {token}` 자동 주입.
- **응답 인터셉터:** 응답에 `Authorization` 헤더 있으면 토큰 갱신 저장 (재발급 대응).
- **401 처리:** `POST /api/members/refresh` 1회 시도 → 새 헤더 토큰 저장 → 원요청 재시도. 재발급도 실패하면 로그아웃 처리 + 로그인 페이지 이동. (무한루프 방지 가드 필수)

```ts
// shared/api — 골격 (디자인 아님, 동작 규약)
const api = axios.create({ baseURL: '/api', withCredentials: true }) // ⭐ withCredentials

api.interceptors.request.use((config) => {
  const token = authStore.accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => {
    const h = res.headers['authorization']
    if (h) authStore.setAccessToken(h.replace(/^Bearer /, ''))
    return res
  },
  async (error) => {
    // 401 → /members/refresh 1회 → 재시도 (재진입 가드 사용)
    // 실패 시 authStore.logout() + redirect('/login')
    return Promise.reject(error)
  }
)
```

> ⚠️ **현재 백엔드 화이트리스트가 `/api/**` 전체를 인증 면제로 열어둔 상태(개발용).** 즉 지금은 토큰 없이도 대부분 호출이 통할 수 있다. 그래도 **인증 플로우는 정상 가정하고 구현**한다(운영 시 닫힘). `@LoginMember`가 붙은 엔드포인트(아래 "인증 ✅")는 로그인 필요로 취급.

---

## 5. 전체 API 레퍼런스

> 인증 ✅ = 서버가 토큰에서 사용자 식별(`@LoginMember`). 인증 - = 비로그인 호출 가능.

### 5.1 Post (후기) — `/api/posts`
| 동작 | Method | Path | 인증 | 요청 | 응답 |
|------|--------|------|------|------|------|
| 목록 | GET | `/api/posts` | - | — | `PostResponse[]` (배열, 페이지네이션 없음) |
| 단건 | GET | `/api/posts/{postId}` | - | — | `PostResponse` |
| 작성(JSON) | POST | `/api/posts` | - ⚠️ | `PostRequest` (JSON) | `PostResponse` (200) |
| 작성(이미지 포함) | POST | `/api/posts` | - ⚠️ | **multipart** | `PostResponse` (200) |
| 수정 | PATCH | `/api/posts/{postId}` | - ⚠️ | `PostRequest` (JSON) | `PostResponse` |
| 삭제 | DELETE | `/api/posts/{postId}` | - ⚠️ | — | `204` |

⚠️ **작성/수정 quirk:** Post 엔드포인트에는 `@LoginMember`가 **없다.** 즉 서버가 토큰으로 작성자를 식별하지 않고, **요청 바디 `PostRequest`의 `userId`/`email`을 그대로 신뢰**한다. → 프론트는 작성/수정 시 로그인한 회원의 `userId`·`email`을 바디에 직접 넣어 보내야 한다. (auth store에서 가져옴. `/api/members/me`는 email만 주므로 userId 확보 경로를 사용자와 확인 필요 — ❓)

**multipart 작성(이미지 포함) — part 이름 정확히:**
- `post` : `PostRequest` JSON (Content-Type `application/json` part) — **필수**
- `files` : 이미지 파일 배열(`MultipartFile[]`) — 선택
```ts
const fd = new FormData()
fd.append('post', new Blob([JSON.stringify(postReq)], { type: 'application/json' }))
files.forEach(f => fd.append('files', f))
// axios가 boundary 포함 Content-Type 자동 설정 → 직접 지정 금지
```

### 5.2 Post Image — `/api/posts/{postId}/images`
| 동작 | Method | Path | 인증 | 요청 | 응답 |
|------|--------|------|------|------|------|
| 목록 | GET | `/api/posts/{postId}/images` | - | — | `PostImage[]` |
| 업로드 | POST | `/api/posts/{postId}/images` | - ⚠️ | multipart, part 이름 `files` (필수) | `PostImage[]` (200) |

- 저장: S3 업로드 → `post_image`(1:N), 응답에 `imageUrl`(S3 public URL) 포함.
- 이미지 검증 실패 코드: `NOT_IMAGE_FILE`(400), `EMPTY_IMAGE_FILE`(400), `IMAGE_UPLOAD_FAILURE`(500).

### 5.3 Comment (댓글) — `/api/posts/{postId}/comments`
| 동작 | Method | Path | 인증 | 요청 | 응답 |
|------|--------|------|------|------|------|
| 목록(커서) | GET | `/api/posts/{postId}/comments?cursor={id}&size=10` | - | — | `CommentPageResponse` |
| 작성 | POST | `/api/posts/{postId}/comments` | ✅ | `{ content }` | `201` (바디 없음) |
| 수정 | PATCH | `/api/posts/{postId}/comments/{commentId}` | ✅ | `{ content }` | `200` (바디 없음) |
| 삭제 | DELETE | `/api/posts/{postId}/comments/{commentId}` | ✅ | — | `200` (바디 없음) |

- **커서 페이지네이션:** 첫 요청은 `cursor` 생략. 응답 `nextCursor`를 다음 요청 `cursor`로, `hasNext`가 false면 끝. (page/size 아님 ⚠️)
- `content`는 **공백 불가**(`@NotBlank`) — 위반 시 검증 에러.
- 작성/수정/삭제 후 바디가 없으므로 **목록 재조회** 또는 낙관적 갱신.
- ⚠️ 현재 `CommentPageResponse.items`(=`PostCommentDto`)에 **작성자(닉네임/프로필) 필드가 없다.** 작성자 표시가 필요하면 백엔드 DTO 보강 필요 → 일단 작성자 없는 UI로.

### 5.4 Post Like — `/api/posts/{postId}/likes`
| 동작 | Method | Path | 인증 | 응답 |
|------|--------|------|------|------|
| 좋아요 토글 | POST | `/api/posts/{postId}/likes` | ✅ | `200` `{ liked, likeCount }` |

- 토글(켜고/끄고 동일 엔드포인트). 응답의 `liked`/`likeCount`로 즉시 UI 갱신.

### 5.5 Place (식당) — `/api/places`
| 동작 | Method | Path | 인증 | 파라미터 | 응답 |
|------|--------|------|------|----------|------|
| 지역구별 목록 | GET | `/api/places/{areaCode}` | - | `size`(기본 10), `page`(기본 **0**) | `SpringPage<PlaceResponse>` |
| 검색 | GET | `/api/places/search` | - | — | ⚠️ **미구현(void 반환)** — 사용 금지 |

- ⚠️ 이 목록만 **Spring `Page` 래퍼**로 온다(아래 타입). `page`는 **0부터** 시작.
- `areaCode` = 부산 지역구 코드(문자열). 가능한 값 목록은 백엔드/데이터셋에서 받아야 함 → ❓ (현재 코드에 상수 없음). UI는 지역구 선택을 구성하되 실제 코드 값은 사용자와 확인.

### 5.6 Place Like — `/api/places`
| 동작 | Method | Path | 인증 | 파라미터 | 응답 |
|------|--------|------|------|----------|------|
| 식당 좋아요 | POST | `/api/places/{placeId}/likes` | ✅ | — | `201` (바디 없음) |
| 좋아요 취소 | DELETE | `/api/places/{placeId}/likes` | ✅ | — | `204` |
| 내 좋아요 목록 | GET | `/api/places/likes/my` | ✅ | `lastId`(선택), `size`(기본 10) | `PlaceLikeDetailResponse[]` |

- ⚠️ "좋아요/취소"는 **토글이 아니라 분리된 POST/DELETE**. 프론트가 현재 상태를 알고 호출해야 함. 중복 좋아요 시 `PLACE_LIKE_DUPLICATE`(400).
- 내 목록은 **`lastId` 커서**(반환은 래퍼 없는 순수 배열). `hasNext` 필드 없음 → **반환 개수 == 요청 size**면 다음 페이지 있다고 추정, 마지막 항목 `placeLikeId`(또는 placeId)를 다음 `lastId`로. (커서 기준 필드 동작은 ❓ 확인 권장)

---

## 6. 타입 정의 (TypeScript — 백엔드 DTO 1:1)

```ts
// ===== 공통 =====
export interface ApiError { message: string }          // 모든 에러 바디

export interface SpringPage<T> {                        // /api/places/{areaCode} 전용
  content: T[]
  number: number          // 현재 페이지 (0-based)
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

// ===== Auth / Member =====
export interface LoginRequest  { email: string; password: string }
export interface SignupRequest { email: string; password: string }   // MemberRequestDto
export interface MemberInfo    { email: string }                      // GET /me (MemberInfoDto)
// 로그인/리프레시는 바디 없음 — 토큰은 Authorization 헤더 + EBRefreshToken 쿠키

// ===== Post =====
export interface PostRequest {                          // PostRequireDto (작성/수정)
  userId: number          // ⚠️ 토큰이 아닌 바디로 작성자 전달
  placeId: number
  email: string           // ⚠️ 마찬가지
  title: string
  content: string
}
export interface PostResponse {
  postId: number          // ⚠️ 'id' 아님, 'postId'
  userId: number
  placeId: number
  email: string
  title: string
  content: string
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string       // ISO LocalDateTime "2026-06-11T10:00:00"
  updatedAt: string
  images: PostImage[]
}

// ===== Post Image =====
export interface PostImage {
  id: number
  postId: number
  imageUrl: string        // S3 public URL — <img :src>에 바로 사용
  imageKey: string
  sortOrder: number
}

// ===== Comment =====
export interface CommentRequest { content: string }     // @NotBlank
export interface Comment {                              // PostCommentDto
  id: number
  content: string
  createdAt: string       // ⚠️ String 타입 (Post의 createdAt과 직렬화가 다를 수 있음)
  // ⚠️ 작성자 정보 없음
}
export interface CommentPageResponse {
  items: Comment[]
  nextCursor: number | null
  hasNext: boolean
}

// ===== Post Like =====
export interface PostLikeResponse { liked: boolean; likeCount: number }

// ===== Place =====
export interface PlaceResponse {
  id: number
  name: string
  address: string
  area_cde: string        // ⚠️ 백엔드 필드명 오타(snake_case 'area_cde') — 그대로 매핑
  phone: string
  url: string
}

// ===== Place Like =====
export interface PlaceLikeDetailResponse {              // GET /places/likes/my
  placeLikeId: number
  placeId: number
  code: string
  name: string
  address: string
  areaCode: string        // ⚠️ 여긴 camelCase 'areaCode' (Place의 'area_cde'와 불일치)
  phone: string
  url: string
  likeCnt: number
}
```

---

## 7. 에러 처리

모든 에러 응답 바디는 **`{ "message": "한글 메시지" }`** (단일 필드, status/code 없음).
→ `shared/api` 응답 인터셉터에서 `error.response?.status` + `error.response?.data?.message`로 토스트/스낵바 처리.

**주요 코드 (HTTP status / 의미):**
| 상황 | status | 메시지 |
|------|--------|--------|
| 로그인 실패 | 401 | ID/PW가 틀렸습니다. |
| 토큰 없음/무효 | 401 | 토큰이 없습니다 / 유효하지 않은 토큰입니다. |
| 리프레시 토큰 무효/불일치 | 401 | 유효하지 않은/일치하지 않는 Refresh 토큰… |
| 회원 중복 | 409 | 이미 등록된 회원입니다 |
| 회원/후기/식당/댓글 없음 | 404 | 없는 … 입니다 |
| 식당 좋아요 중복 | 400 | 이미 좋아요한 장소입니다. |
| 댓글 공백 | 400 | 댓글의 본문은 공백이 될 수 없습니다. |
| 페이지 size ≤ 0 | 400 | 페이지 사이즈는 0보다 커야 합니다. |
| 이미지 아님/빈 파일 | 400 | 이미지 파일만/빈 이미지… |
| 이미지 업로드 실패 | 500 | S3 이미지 업로드에 실패했습니다. |

> **401 메시지를 사용자에게 그대로 노출하기보다**, 인터셉터가 토큰 재발급/로그인 유도로 변환. 그 외 4xx는 메시지 토스트, 5xx는 "잠시 후 다시 시도" 류 일반 메시지 권장.

---

## 8. 페이지네이션 — 3가지가 섞여 있음 (주의)

| 기능 | 방식 | 파라미터 | 응답 |
|------|------|----------|------|
| 식당 목록 (`/places/{areaCode}`) | offset | `page`(0부터), `size` | `SpringPage<T>` 래퍼 |
| 댓글 (`/posts/{id}/comments`) | 커서 | `cursor`, `size` | `{ items, nextCursor, hasNext }` |
| 내 식당좋아요 (`/places/likes/my`) | 커서 | `lastId`, `size` | 순수 배열 (hasNext 추론) |
| 후기 목록 (`/posts`) | **없음** | — | 전체 배열 |

→ `shared/composables`에 **방식별 컴포저블**(`useOffsetPagination`, `useCursorPagination`)을 두고 기능별로 골라 쓴다. 하나로 억지 통합하지 말 것.

---

## 9. 구현 상태 요약 (UI 분기용)

| 기능 | 상태 |
|------|------|
| 회원가입/로그인/로그아웃/내정보/토큰재발급 | 🟢 구현 |
| 후기 CRUD (JSON + multipart) | 🟢 구현 |
| 후기 이미지 업로드/조회 (S3) | 🟢 구현 |
| 댓글 CRUD (커서 목록) | 🟢 구현 |
| 게시글 좋아요 토글 | 🟢 구현 |
| 식당 지역구별 목록 | 🟢 구현 |
| 식당 좋아요/취소/내목록 | 🟢 구현 |
| 식당 키워드 검색 (`/places/search`) | 🔴 미구현(void) — 비활성 처리 |
| QR 초대방 / WebSocket 실시간 | 🔴 미구현 — 범위 제외 |
| 댓글/후기 작성자 표시(닉네임/프로필) | ⚠️ DTO에 필드 없음 — email만 가용 |

---

## 10. 먼저 사용자에게 확인할 것 (❓)

1. `areaCode`(부산 지역구) 코드 값의 실제 목록/매핑 (UI 지역 선택용).
2. 후기 작성 시 `userId` 확보 경로 — `/me`는 email만 주는데 작성 바디는 `userId` 필요. (별도 조회? 토큰? 백엔드 보강?)
3. 댓글/후기 작성자 표시 필요 여부 (필요 시 백엔드 DTO 보강 선행).
4. 액세스 토큰 저장 위치 정책 (메모리 vs localStorage — 보안/UX 트레이드오프).

---

### 시작 지시
위 스펙대로 `shared/api`(axios+인터셉터), `shared/types`(위 타입 전체), `features/auth`(로그인/회원가입/토큰 플로우)부터 구축해라. 비주얼은 Vuetify 기본 + 별도 디자인 토큰을 따르되, **데이터/타입/인증 정확성**을 최우선으로 한다. 불명확하면 추측 말고 위 §10을 사용자에게 물어라.
