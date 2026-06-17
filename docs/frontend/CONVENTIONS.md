# Frontend Conventions (eatBusan)

> 이 문서는 **팀 전체의 프론트엔드 규칙**이다. 모든 팀원과 모든 Claude 세션은 코드를 작성하기 전에 이 문서를 따른다.
> 프론트 레포가 분리되면 이 내용을 그 레포 루트의 `CLAUDE.md`로 옮긴다 (Claude Code가 자동으로 읽음).

---

## 1. 기술 스택 (변경 금지 — 바꾸려면 팀 합의)

| 영역 | 선택 | 비고 |
|------|------|------|
| 프레임워크 | **Vue 3** (`<script setup>` Composition API) | Options API 금지 |
| 빌드 | **Vite** | |
| 언어 | **TypeScript** | `any` 지양, API 응답은 반드시 타입 정의 |
| UI 라이브러리 | **Vuetify** | 버튼/입력/카드 등은 직접 만들지 말고 Vuetify 컴포넌트 사용 |
| 상태관리 | **Pinia** | 전역 상태만. 컴포넌트 로컬 상태는 `ref`/`reactive` |
| 라우팅 | **Vue Router** | |
| HTTP | **Axios** | **직접 호출 금지.** 반드시 `shared/api` 인스턴스 + feature `api/` 모듈 경유 |
| 포맷/린트 | **ESLint + Prettier** | 커밋 전 자동 포맷. 규칙은 팀 공유 설정 사용 |

백엔드: Spring Boot, base URL `http://localhost:8081` (개발 시 Vite proxy로 `/api` → 8081).

---

## 2. 폴더 구조 (feature-based)

```
src/
  app/                  # 앱 부트스트랩 (router, pinia, vuetify 플러그인 등록)
  router/               # 라우트 정의
  shared/
    api/                # axios 인스턴스 + 인터셉터 (토큰 주입, 에러 처리)
    components/         # 공통 컴포넌트 (BaseButton 래퍼 등)
    composables/        # 공통 컴포저블 (usePagination 등)
    types/              # 공통 타입 (ApiResponse, Page 등)
  features/
    <feature>/          # 예: post, comment, postLike, auth
      api/              # 이 기능의 API 호출 함수
      store/            # 이 기능의 pinia store
      components/       # 이 기능 전용 컴포넌트
      views/            # 라우터에 연결되는 페이지
      types/            # 이 기능의 타입 (요청/응답 DTO)
  assets/               # 이미지/폰트
```

**규칙: 새 기능은 항상 `features/<name>/` 하위에 위 5개 폴더 구조로 만든다.** 레퍼런스는 `features/comment/`(템플릿).

---

## 3. 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 컴포넌트 파일/이름 | PascalCase | `CommentItem.vue` |
| 페이지(view) | `XxxView.vue` 또는 `XxxPage.vue` (택1, 통일) | `CommentListView.vue` |
| composable | `useXxx` | `usePagination.ts` |
| store | `useXxxStore` | `useCommentStore` |
| api 함수 | 동사 시작 | `fetchComments`, `createComment`, `deleteComment` |
| 타입 | PascalCase, 응답=`XxxResponse`, 요청=`XxxRequest` | `CommentResponse` |
| 변수/함수 | camelCase | |
| 상수 | UPPER_SNAKE_CASE | |

---

## 4. 핵심 규칙 (Do / Don't)

**Do**
- API 호출은 `features/<f>/api/`의 함수로만. 컴포넌트에서 axios 직접 호출 ❌
- 색/간격/폰트는 [DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)의 토큰/Vuetify 테마로만. 하드코딩 ❌
- 응답/요청 데이터는 `types/`에 타입 정의 후 사용
- 컴포넌트는 작게. 한 파일이 200줄 넘으면 분리 고민
- 비동기 호출은 로딩/에러 상태를 반드시 처리

**Don't**
- 인라인 스타일(`style="..."`) 남발 금지 (Vuetify props/클래스 우선)
- `any` 타입 금지 (불가피하면 주석으로 이유)
- 컴포넌트 안에서 직접 fetch/axios 금지
- 전역 상태에 안 넣어도 되는 것까지 Pinia에 넣지 말 것

---

## 5. API 연동 규약

- 모든 요청은 `shared/api`의 axios 인스턴스 사용 (baseURL, 인터셉터 공통)
- 인증: `Authorization` 헤더에 access token (인터셉터에서 자동 주입)
- 요청/응답 형태는 [API_CONTRACT.md](./API_CONTRACT.md)가 **유일한 기준**. 백엔드와 형태가 다르면 코드가 아니라 계약 문서를 먼저 고친다.

---

## 6. Git 협업

- 브랜치: `feat/<기능>-ui`, `fix/<...>` (백엔드 컨벤션과 동일)
- 작은 단위 PR + 팀원 리뷰 후 머지
- PR 전 `npm run lint` / 포맷 통과
- **새 기능 시작 전**: 이 문서 + `features/comment/` 템플릿을 먼저 본다

---

## 7. Claude에게 (이 레포에서 작업하는 모든 세션)

- 새 컴포넌트/기능을 만들 땐 **반드시 `features/comment/` 템플릿의 구조·패턴을 모방**한다.
- 스택을 임의로 바꾸지 않는다 (위 1번 표 고정).
- API 형태가 필요하면 추측하지 말고 [API_CONTRACT.md](./API_CONTRACT.md)를 본다. 없으면 사용자에게 확인.
- 스타일 값은 [DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)에서 가져온다.
