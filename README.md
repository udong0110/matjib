# BUSEAT

Spring Boot + MyBatis + Thymeleaf 기반 맛집 리뷰 게시판.
리뷰를 작성하면 포인트를 적립하고, 3000점을 모으면 환급 신청할 수 있는 리워드형 서비스.

## 기술 스택
- Java 17 / Spring Boot 3.3
- Spring Security (세션 로그인, BCrypt 암호화)
- MyBatis (동적쿼리 + 3테이블 조인)
- Thymeleaf (화면)
- MySQL 8.x
- SpringDoc OpenAPI (Swagger)

## 주요 기능
- 회원가입 / 로그인 / 로그아웃 (Spring Security, 비밀번호 BCrypt)
- 맛집 리뷰 목록 (검색: 키워드/카테고리/지역/별점 + 페이징)
- 리뷰 상세 / 작성 / 수정 / 삭제 (작성자 본인만)
- **포인트 리워드**: 리뷰 1건당 100P 적립
- **환급 신청**: 3000P 이상 시 3000원 환급 신청 (차감 + 내역 기록)
- 마이페이지: 보유 포인트, 환급 진행바, 포인트 내역
- 가게 데이터: 더미 5개 또는 카카오 API로 실제 데이터 시딩(선택)

## 과제 요구사항 매핑
| 요구사항 | 구현 위치 |
|---|---|
| ERD / DB 설계 | `schema.sql` |
| 회원가입 | `MemberService`, `/signup` |
| Spring Security 로그인 | `SecurityConfig`, `CustomUserDetailsService` |
| 비밀번호 암호화 | `SecurityConfig.passwordEncoder()` (BCrypt) |
| 페이징 + 검색 | `ReviewMapper.xml`, `PageResponse` |
| MyBatis Join + 동적쿼리 | `ReviewMapper.xml` (`<where>`,`<if>` + 3테이블 조인) |
| ControllerAdvice 예외 처리 | `GlobalExceptionHandler` |
| Swagger + log | `SwaggerConfig`, 각 서비스 `@Slf4j` |
| MVC 화면(View) | Thymeleaf `templates/` |

## 실행 방법
1. MySQL에 DB 생성
   ```sql
   CREATE DATABASE matjib DEFAULT CHARACTER SET utf8mb4;
   ```
2. `application.yml`에서 DB username/password 수정
3. `./gradlew bootRun` 또는 IntelliJ에서 `MatjibApplication` 실행
   - 시작 시 `schema.sql`이 자동 실행 (테이블 생성 + 더미 가게 5개)
4. 브라우저에서 접속
   - 화면: http://localhost:8080/reviews
   - Swagger: http://localhost:8080/swagger-ui/index.html

## 사용 흐름 (화면)
1. 회원가입 → 로그인
2. "리뷰 쓰기" → 가게 선택 + 별점 + 내용 작성 → 등록 (자동으로 100P 적립)
3. 마이페이지에서 포인트 확인, 3000P 모이면 환급 신청

## 카카오 API로 실제 가게 채우기 (선택)
> 실제 가게 데이터는 초기 시딩용으로만 사용, 조회는 전부 DB에서 MyBatis로 수행.
1. 카카오 디벨로퍼스에서 REST API 키 발급 + 카카오맵 활성화
2. `application.yml`:
   ```yaml
   kakao:
     api-key: 발급받은_키
     seed-enabled: true
   ```
3. 재실행하면 `StoreDataSeeder`가 부산 맛집을 store 테이블에 저장

## 패키지 구조
```
com.example.matjib
├── config       # Security, Swagger
├── controller   # PageController(화면), REST 컨트롤러
├── service      # 비즈니스 로직 (포인트 적립/환급 포함)
├── mapper       # MyBatis 매퍼
├── domain       # 엔티티
├── dto          # 요청/응답 객체
├── exception    # 예외 + 글로벌 핸들러
└── init         # 카카오 API 시더
```

## 향후 계획
`FUTURE_PLANS.md` 참고 (AI 리뷰 요약, 배너 광고 수익모델 등)

## 관리자 계정
앱 시작 시 자동 생성됩니다.
- 아이디: `admin`
- 비밀번호: `admin1234`

관리자로 로그인하면 가게 상세 페이지에서 "가게 대표사진 등록" 기능이 보입니다.
(일반 사용자에게는 보이지 않음 — Spring Security 권한 분리)

## BUSEAT의 특색 기능
- **지역별 맛집 코스**: 지역을 선택하면 아침(카페)/점심(식사류+카페)/저녁(식사류, 카페 제외) 코스를 자동 추천.
  - 선정 방식: 리뷰 1개 이상·별점 3.0 이상인 후보 중 랜덤 선택 (유명 맛집만 반복 노출되지 않도록 상권을 고루 노출).
  - 프랜차이즈/체인점은 제외하고 로컬 가게를 우선.
  - 리뷰가 아직 없는 콜드스타트 상황에서는 조건을 단계적으로 완화해 그 지역 가게 중 랜덤으로 채움 (아침/점심/저녁 3슬롯이 항상 채워지도록 폴백).
