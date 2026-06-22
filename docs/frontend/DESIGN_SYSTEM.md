# Design System (eatBusan)

> 디자인 일관성의 단일 기준. 색·간격·폰트·컴포넌트 사용 규칙을 여기서만 정의한다.
> 코드에서 색상값/픽셀을 **하드코딩하지 말고** 여기 정의된 토큰(Vuetify 테마 변수)을 쓴다.
>
> ⚠️ 아래 값은 **초안**이다. 팀 디자인 합의 후 확정 (Figma 등 있으면 거기 값과 동기화).

---

## 1. 컬러 토큰 (Vuetify theme)

`app/`의 Vuetify 플러그인 설정에 테마로 등록하고, 컴포넌트에선 `color="primary"`처럼 **이름으로만** 참조.

```ts
// vuetify theme (초안 — 부산/음식 컨셉: 바다 블루 + 포인트 코랄)
const eatBusanTheme = {
  dark: false,
  colors: {
    primary:   '#0277BD',  // 메인 (바다 블루)
    secondary: '#FF7043',  // 포인트 (코랄)
    success:   '#43A047',
    warning:   '#FB8C00',
    error:     '#E53935',
    info:      '#039BE5',
    background:'#FAFAFA',
    surface:   '#FFFFFF',
  },
}
```

규칙: 텍스트/배경/버튼 색은 위 토큰만. `#xxxxxx` 직접 입력 ❌.

---

## 2. 간격 (Spacing)

Vuetify의 spacing 유틸리티 사용 (`pa-4`, `ma-2`, `gap` 등). 1 단위 = 4px.

| 용도 | 값 |
|------|-----|
| 컴포넌트 내부 패딩 | `pa-4` (16px) |
| 카드 간 간격 | `ma-2`~`ma-4` |
| 섹션 간 간격 | `my-6`~`my-8` |

임의 `margin: 13px` 같은 값 ❌.

---

## 3. 타이포그래피

Vuetify typography 클래스 사용.

| 용도 | 클래스 |
|------|--------|
| 페이지 제목 | `text-h4` |
| 섹션 제목 | `text-h6` |
| 본문 | `text-body-1` |
| 보조/캡션 | `text-caption text-medium-emphasis` |

폰트: 기본 Roboto (Vuetify) 또는 팀 합의 폰트 1종. 혼용 금지.

---

## 4. 컴포넌트 사용 규칙

- 버튼 → `<v-btn>` (직접 `<button>` 스타일링 ❌)
- 입력 → `<v-text-field>` / `<v-textarea>`
- 목록 카드 → `<v-card>`
- 로딩 → `<v-progress-circular>` / 스켈레톤
- 다이얼로그 → `<v-dialog>`
- 공통적으로 반복되는 조합은 `shared/components/`에 래퍼로 추출

---

## 5. 반응형 / 레이아웃

- Vuetify Grid (`<v-container> <v-row> <v-col>`) 사용
- 브레이크포인트: Vuetify 기본 (`xs sm md lg xl`)
- 모바일 우선 고려 (음식/장소 앱은 모바일 사용 많음)

---

## 6. TODO (팀 확정 필요)
- [ ] 최종 컬러 팔레트 (디자이너/Figma 값과 동기화)
- [ ] 로고/파비콘
- [ ] 다크모드 지원 여부
- [ ] 폰트 최종 결정
