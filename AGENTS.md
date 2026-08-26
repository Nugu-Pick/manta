# 누구픽 백엔드 작업 지침

## 문서 기준

- 이 파일은 `manta` 백엔드의 작업 경계를 설명한다.
- `docs/`는 개인 로컬 문서 보관 영역이며, 이 파일에 그 문서의 내용이나 절차를 복제하지 않는다.
- 저장소에 공유할 구현 규칙은 이 파일과 코드·migration에 명시한다.
- 기능 구현을 진행하면 관련 plan/spec 문서에 기준일과 함께 `완료`, `진행 중`, `미착수` 상태를 기록하고, 실제 경로·구현과 계획을 일치시킨다.
- 설명·문서·주석은 한글로 작성한다. 클래스명, 메서드명, 환경변수명, SQL 식별자, CLI 명령과 고유명사는 원문을 유지한다.
- 작업을 진행할 때 로컬뿐만 아니라 운영에서도 발생할 수 있는 변경을 고려한다.

## 프로젝트 구조

- 애플리케이션 진입점은 `src/main/java/com/chaean/manta`다.
- 도메인 우선 Application Module은 `member`, `content`, `community`를 사용한다.
- `common`은 HTTP 계약·예외·인증 principal·영속성 감사·시간·직렬화·QueryDSL처럼 여러 모듈이 공유하는 기술 공통 코드만 둔다. 도메인 Entity·Repository·비즈니스 Service는 두지 않는다.
- `common` 내부는 성격별 하위 디렉터리로 나눈다. 예를 들어 `common/web/response`, `common/web/error`, `common/web/filter`, `common/config`, `common/persistence`, `common/serialization`을 사용하며 서로 다른 성격의 클래스를 한 디렉터리에 섞지 않는다.
- `common`은 `spring.modulith.detection-strategy: explicitly-annotated` 설정에서 `@ApplicationModule`을 선언하지 않는 기술 공통 패키지로 유지한다. 도메인 모듈처럼 별도 공개 API·Repository·Service 경계를 만들지 않는다.
- 공통 `layer` 패키지나 사용처 하나뿐인 추상화 패키지를 먼저 만들지 않는다.
- 변경 전 기존 클래스와 함수의 사용처를 먼저 확인하고 재사용한다. 요청하지 않은 추상화·모듈·기능은 추가하지 않으며, 필요한 변경은 최소 범위로 수행한다.
- 모듈 간 공개 호출은 명시한 API 인터페이스를 사용하고, 다른 모듈의 `internal` 패키지와 Repository를 직접 참조하지 않는다.
- Spring Modulith 경계를 유지하며 cross-module 양방향 JPA 연관관계와 cascade를 만들지 않는다.
- 다른 모듈에서 Entity가 필요한 경우에는 소유 모듈의 `<module>.entity` Named Interface를 통해서만 참조한다.
- 회원 권한 조회 계약은 `member/api`에 두고, DB 조회 구현은 `member/internal/application`의 Query Service가 담당한다. 공개 계약은 `MemberRole` 같은 도메인 값만 반환하며 `GrantedAuthority`나 HTTP DTO를 노출하지 않는다.
- JWT를 `Authentication`으로 변환하고 `MemberRole`을 `ROLE_*`로 매핑하는 코드는 `member/internal/security`에 둔다. `common/security`에는 공통 SecurityFilterChain과 인증·인가 오류 처리만 둔다.
- `SupabaseJwtAuthenticationConverter`의 현재 위치와 계층 책임을 임의로 `convert`, `authentication` 등으로 이동하지 않는다. 패키지 이동은 의존성 방향을 확인하고 설계 합의 후 수행한다.
- 아키텍처 질문에는 현재 구현과 제안 구조를 구분해 설명하며, 구현·변경 요청이 없는 토론 단계에서는 코드를 수정하지 않는다.

## 브랜치와 Pull Request

- `main`은 운영 배포 기준이자 기본 브랜치로 사용하고, `dev`는 개발 통합 브랜치로 사용한다.
- 모든 구현 작업은 반드시 `Issue 생성 → Issue 번호 확인 → Issue 번호를 포함한 브랜치 생성 → 구현 → 검증 → PR 생성` 순서로 진행한다. Issue 없이 먼저 브랜치를 만들거나, 검증 전에 PR을 생성하지 않는다.
- 기능·버그 브랜치는 `dev`에서 생성해 `dev`로 병합한다. 운영 반영은 `dev`에서 `main`으로 Release PR을 만든다.
- 브랜치명은 `feat/123-oauth-login`, `fix/145-jwks-validation`처럼 유형·Issue 번호·짧은 설명을 lowercase kebab-case로 작성한다. 괄호는 사용하지 않고 `/`, `-`처럼 GitHub에서 안전한 구분자만 사용한다.
- 구현 완료 후 `./gradlew clean check` 등 작업에 맞는 검증을 성공시키고 결과를 확인한 뒤 PR을 생성한다.
- PR 본문에서 `feat/*`, `fix/* → dev`는 Issue 번호만 참조하고, `dev → main` Release PR에서 필요한 Issue에 `Closes #123`을 사용한다.

## 버전과 배포

- Release 버전은 `vMAJOR.MINOR.PATCH` 형식을 사용한다. `v`는 Git tag 접두사이며, SemVer 숫자는 호환성을 깨는 변경·하위 호환 기능 추가·하위 호환 버그 수정을 각각 의미한다. 기준은 <https://semver.org/>를 따른다.
- `MAJOR`는 기존 API·동작과 호환되지 않는 변경, `MINOR`는 기존 사용자를 깨지 않는 기능 추가, `PATCH`는 하위 호환 버그 수정에 올린다.
- PR merge는 Git 브랜치에 변경을 반영하는 작업이며 자동 배포와 동일하지 않다. 자동 배포는 `main` push를 감시하는 CI/CD workflow가 별도로 구성된 경우에만 수행된다.
- GitHub Actions 테스트 CI는 `pull_request` 이벤트의 `dev` 대상 feature/fix PR과 `dev → main` Release PR, 그리고 `push` 이벤트의 `dev` 반영에서 실행한다. 각 검증은 `./gradlew clean openapi3`로 단위·통합 테스트와 OpenAPI 생성을 함께 확인한다. Release PR이 병합된 뒤 발생하는 `main` push에서는 테스트 CI를 중복 실행하지 않는다.
- Release PR에는 CI/CD가 수행하는 검증·배포 절차를 중복 체크리스트로 작성하지 않는다. DB migration이 포함된 변경은 해당 migration의 호환성 규칙과 배포 순서를 코드·설계 문서에 기록한다.

## DTO·Entity·레이어 책임

- API 요청·응답 DTO는 Java `record`로 통일한다. DTO는 transport 데이터와 validation 선언만 담당하며 Entity, QueryDSL, HTTP 응답 생성 로직을 포함하지 않는다.
- HTTP DTO는 `<module>.web.dto.request`와 `<module>.web.dto.response`로 구분한다. 요청 DTO에는 입력 validation을, 응답 DTO에는 외부 응답 필드만 둔다. 현재 사용하지 않는 DTO를 미리 만들지 않는다.
- Entity는 JPA·QueryDSL·명시된 외부 모듈에서 접근할 수 있도록 `public`으로 선언하고, 모든 모듈의 `<module>.entity`를 Named Interface로 공개한다. 이는 이 프로젝트에서 JPA 연관관계를 활용하기 위한 예외적인 공개이며, 다른 모듈은 Entity를 조회·연관관계 참조용으로만 사용한다. Entity 상태 변경 메서드 호출, 소유 모듈 Repository 직접 참조, cross-module cascade·orphanRemoval은 금지한다.
- Controller에서 Entity를 직접 반환하지 않으며, 다른 모듈의 상태 변경은 소유 모듈의 API·Service를 통해서만 수행한다.
- Entity는 식별자·상태·도메인 불변식과 상태 전이 메서드를 소유한다. 무분별한 public setter, HTTP·provider·응답 포맷 의존을 만들지 않는다.
- Controller는 입력 validation, 인증 주체 추출, Service 호출, DTO 매핑과 HTTP status만 담당한다.
- 상태를 변경하는 유스케이스 Service는 `*CommandService`, 조회 전용 유스케이스 Service는 `*QueryService`로 명명한다. `CommandService`는 생성·수정·삭제와 transaction 경계를, `QueryService`는 조회 조합과 read model 변환을 담당하며 서로의 책임을 섞지 않는다.
- Service는 유스케이스 orchestration과 transaction 경계를 담당한다. 저장 방식, JSON 직렬화, Controller 전용 타입을 소유하지 않는다. Query·Command Service는 `web.dto`를 반환하지 않고 application read model 또는 도메인 결과를 반환하며, Controller가 HTTP DTO로 변환한다.
- `internal/application`의 `*CommandService`, `*QueryService`, application read model은 같은 application 패키지에 둔다. command/query 기준으로 하위 디렉터리를 나누지 않으며, 기능별 하위 디렉터리가 실제로 필요해질 때만 추가한다.
- 모듈 간 동기 호출 계약은 `<module>.api`에 두고, 모듈 간 비동기 이벤트 계약은 `<module>.api.event`에 둔다. 이벤트 record는 `*Event` 접미사를 사용해 동기 API와 구분한다.
- Repository는 저장·조회·QueryDSL/native query와 persistence projection만 담당한다. 권한 판단·상태 전이·HTTP 응답 조립을 Repository에 넣지 않는다.
- 공통 `ApiErrorCode`는 HTTP 변환 계약을 제공하고, 공통·기능별 오류 코드는 `common.web.error.ErrorCode`에 정의한다. enum 내부에 `// Common`, `// Member`처럼 기능 범위를 주석으로 구분하며 별도의 `MemberErrorCode` enum을 만들지 않는다.
- 외부 API 오류 코드는 Manta 전용 `M` 접두사와 세 자리 숫자(`M001`, `M002`)를 사용한다. enum 상수명은 의미를 표현하고 `code()`는 명시적인 외부 코드를 반환한다. 공통 오류는 `M001`부터 `M099`, `member`는 `M100`부터 `M199`, `content`는 `M200`부터 `M299`, `community`는 `M300`부터 `M399` 범위에서 할당한다.
- DTO·응답 record의 생성자 인자 검증처럼 호출자 입력의 기본 불변식 검증에는 `IllegalArgumentException`을 사용한다. `BusinessException`은 도메인·애플리케이션 규칙 위반처럼 안정적인 API `ApiErrorCode`가 필요한 경우에만 사용한다.
- 반복되는 생성·변환 규칙은 `of`, `from`, `create` 같은 정적 팩토리를 우선 사용한다. 선택지가 실제로 여러 개 생길 때만 Factory·Strategy·Specification·Adapter를 도입하며, 한 구현만 있는 추상화나 패턴 이름을 위한 패턴은 만들지 않는다.
- Lombok으로 생성자·getter·builder 등 반복 보일러플레이트를 줄인다. 다만 Entity에는 `@Data`와 무분별한 `@Setter`를 사용하지 않고, 필요한 접근자와 상태 전이 메서드만 공개한다.
- Lombok annotation processor는 이미 활성화되어 있으므로 동일한 코드를 수동으로 반복하지 않는다. Lombok이 도메인 규칙을 숨기거나 Entity의 상태 변경 경계를 흐리면 명시적인 Java 코드를 우선한다.
- 설정 클래스는 기본적으로 `@Configuration`을 사용한다. Bean 간 의존성은 `@Bean` 메서드 파라미터로 주입하고, Bean 메서드를 직접 호출해야 하는 설정에만 `proxyBeanMethods = true`를 명시한다.

## 확정 기반 기술

- 기준 패키지는 `com.chaean.manta`다. 문서 예시에도 `com.nugupick`을 사용하지 않는다.
- Spring Modulith 의존성은 `spring-modulith-starter-jpa`를 사용한다. 이벤트 publication 테이블은 JPA 모듈의 공식 매핑을 확인한 뒤 해당 기능 구현 시 migration으로 추가한다.
- Migration은 현재 구현하는 기능의 테이블만 포함한다. 미래 기능의 테이블(예: `legal_document`, `member_agreement`)을 선행 생성하지 않고 해당 기능 구현 시 별도 versioned migration으로 추가한다.
- API의 전역 prefix는 `/api/v1`이다. API 문서 화면은 `/scalar`, OpenAPI 파일은 `/openapi3.yaml` 경로를 사용하며 API prefix와 분리한다.
- API 문서는 Spring REST Docs 테스트를 source of truth로 사용하고, `restdocs-api-spec` adapter로 OpenAPI를 생성하며, Scalar로 표시한다. API path에는 `/api/v1`을 유지하고 OpenAPI `servers` 값은 현재 호스트를 따르는 `/`로 두며 `localhost`를 운영 문서에 넣지 않는다. Springdoc annotation 기반 문서는 사용하지 않는다.
- OpenAPI 산출물은 `build/api-spec`에 생성하고 Git에 커밋하지 않는다. CI/CD 구현 시 검증된 산출물을 Docker image에 포함해 Scalar가 읽도록 한다.
- 실행 환경은 local과 production만 사용하며 `dev`는 배포 환경이 아닌 통합 브랜치다. local과 production 모두 Scalar를 사용할 수 있지만 production의 `/scalar`와 `/openapi3.yaml`은 Caddy Basic Auth로 보호한다. 문서 계정과 비밀번호 해시는 Lightsail runtime 환경 파일에 두고 Git과 image에 넣지 않는다.
- `common.persistence.BaseEntity`, `common.persistence.BaseDeletedEntity`, `common.config.JpaAuditingConfig`, `common.config.JacksonConfig`, `common.serialization.InstantSerializer`, `common.config.QuerydslConfig`는 공통 기반으로 유지한다.
- `BaseEntity`는 `createdAt`, `updatedAt`만 제공하고, `deleted_at`을 사용하는 Entity만 `BaseDeletedEntity`를 상속한다. `status`로 생명주기를 관리하는 Entity에 `deleted_at`을 강제로 추가하지 않는다.
- 공통 기반에 `@SQLRestriction`이나 `@SoftDelete`를 적용하지 않는다. `deleted_at`은 Entity별 삭제·탈퇴·관계 해제 시각으로 명시적으로 관리하고, 관리자 운영 상태가 필요한 Entity는 별도의 `status`로 관리한다. 조회 조건은 Repository Query에서 목적에 맞게 명시한다.
- 애플리케이션의 날짜·시각 값은 Java `Instant`로 통일한다. `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`을 Entity·DTO·Service의 시각 타입으로 사용하지 않는다.
- JPA auditing 시각은 `common.config.JpaAuditingConfig`에서 `Instant`로 제공한다. 업무 기능에 전역 `Clock` Bean을 추가하지 않는다.
- DB 시간 컬럼은 PostgreSQL `timestamptz`를 사용한다. API JSON은 Java `Instant` 값을 `Asia/Seoul` 오프셋(`+09:00`)이 포함된 ISO-8601 문자열로 전역 직렬화하며, Controller나 DTO에서 개별 시간 변환을 작성하지 않는다.
- `content`, `community` 모듈은 기능이 필요해질 때 생성한다. OAuth와 `member` 구현은 공통 기반 완료 후 별도 작업으로 시작하며, 해당 작업을 시작할 때 인증·회원 프로비저닝 범위를 다시 확인한다.

## 코드 포맷

- Java 코드는 150자 이내에서 한 줄 작성을 우선한다. 150자를 넘을 때만 다음 줄로 나누며, enum 상수·생성자 인자를 세로로 정렬하는 형식은 사용하지 않는다.
- local variable은 명시적 타입을 우선하고 `var`는 사용하지 않는다. 타입이 코드 이해에 중요한 Java 백엔드 코드의 가독성을 유지한다.

## 데이터베이스와 Flyway

- 운영 데이터베이스는 Supabase PostgreSQL이다.
- 애플리케이션 전용 스키마 이름은 `orca`다. `app`을 새 SQL, Entity 설정, 문서에 사용하지 않는다.
- PostgreSQL 확장은 `extensions` 스키마에 둔다. 현재 기반 확장은 `postgis`, `pg_trgm`이다.
- 애플리케이션 DB 변경은 `src/main/resources/db/migration/`의 versioned Flyway migration으로만 추가한다.
- 로컬과 운영 모두 Flyway를 사용한다. 로컬도 Hibernate `ddl-auto=validate`로 두며 Hibernate가 테이블을 자동 생성·변경하지 않게 한다.
- Supabase 로컬 DB는 CLI 기본 `postgres/postgres` role을 사용한다. `supabase/seed.sql`은 테스트 데이터만 두며 role 생성·`ALTER ROLE`·schema·extension 설정을 넣지 않는다.
- Flyway migration은 database role을 생성하거나 변경하지 않는다. role provisioning과 권한 관리는 DB 관리자·인프라 영역이며, 애플리케이션 migration은 schema·extension·table 변경만 담당한다.
- migration은 재실행 가능하도록 필요한 경우 `IF NOT EXISTS`를 사용하되, 이미 배포한 migration 파일을 수정하지 않는다.
- 식별자는 lowercase `snake_case`, 시간은 `timestamptz`, 서비스 PK는 `bigint generated always as identity`를 기본으로 한다.
- 닉네임은 trim·Unicode NFKC·대소문자 비구분 정규화 후 canonical 값을 `nickname` 하나에 저장하고, 활성 회원 partial UNIQUE index로 중복을 보장한다. 별도 `nickname_normalized` 컬럼을 추가하지 않는다.
- 운영 schema 변경은 호환 가능한 expand-and-contract 순서로 진행한다. 파괴적 변경은 이전 애플리케이션이 호환되는 단계가 끝난 뒤 별도 migration으로 수행한다.
- 현재 애플리케이션은 Spring API만 DB에 접근한다. `orca`를 Supabase Data API에 노출하지 않고 `anon`, `authenticated`에 권한을 주지 않는다.

## 외부 서비스와 비밀값

- 인증, 데이터베이스, object storage, CDN, 지도 provider는 인프라 경계로 취급하고 도메인 코드에 provider 종속을 퍼뜨리지 않는다.
- 로컬 OAuth2 인증은 hosted 또는 운영 Supabase를 사용하지 않고 `supabase start`로 실행한 로컬 Supabase Auth를 사용한다. 운영 Auth JWT를 로컬 DB와 연결하지 않는다.
- 로컬 `AUTH_ISSUER_URI`와 `AUTH_JWKS_URI`는 `supabase status`의 API URL에서 구성한다. `AUTH_ISSUER_URI`는 API URL 뒤에 `/auth/v1`, `AUTH_JWKS_URI`는 그 뒤에 `/.well-known/jwks.json`을 붙인다.
- 로컬 Auth에서 JWKS 검증을 사용할 때는 비대칭 signing key를 사용한다. signing key private 파일과 Google/Kakao OAuth secret은 `config.toml`에 직접 기록하지 않고 `env()` 또는 실행 환경에서 주입하며 Git에 커밋하지 않는다.
- Manta는 Google·Kakao 등 OAuth provider 로그인만 사용하며 Supabase Auth Confirm Email은 사용하지 않는다. Kakao는 provider 설정에서 이메일 제공을 필수로 하고, Spring은 JWT의 `sub`와 비어 있지 않은 `email`만 확인한다. Custom Access Token Hook과 `email_verified` claim은 추가하지 않는다.
- 운영 Dashboard나 운영 SQL Editor에 자동으로 접속·변경하지 않는다. 운영 schema 변경은 검토 가능한 versioned Flyway migration으로 관리한다.
- 비밀번호, access key, OAuth secret, private key를 프론트엔드·로그·Git에 넣지 않는다. 실제 값은 실행 환경 또는 승인된 secret manager에서 주입한다.
- JWT의 사용자 수정 가능 metadata를 관리자 권한의 근거로 사용하지 않는다. Spring Resource Server가 issuer, JWKS, 서명, 만료를 검증하고 로컬 회원 권한을 확인한다. 이메일은 OAuth provider가 제공한 비공개·읽기 전용 값이며, provider의 별도 이메일 검증 claim을 Manta의 인증 조건으로 사용하지 않는다. 현재 활성 provider는 Supabase 설정에서 관리하며, Naver 같은 provider 추가 시 백엔드 provider별 코드를 추가하지 않는다.

## 코드와 API 규칙

- Controller는 입력 validation, 인증 주체, 응답 매핑만 담당한다. 성공 응답은 `ResponseEntity<ApiResponse<T>>` 형태로 반환하며 `ApiResponse<T>`는 HTTP 상태·헤더를 알지 않는 데이터 envelope로 유지한다. 도메인 규칙과 QueryDSL 조회는 Service/Repository에 둔다.
- 공개 조회, 회원 API, 관리자 API의 인증·인가 경계를 명시하고 테스트한다.
- 단건은 `ApiResponse<T>`, 목록은 `SliceResponse<T>` 또는 `PageResponse<T>` 계약을 따른다.
- 오류 응답은 `code`, `traceId`, `fieldErrors`를 안정된 계약으로 유지한다.
- 외부 링크·파일 업로드·provider 입력은 신뢰하지 않고 allowlist, MIME, 크기, 상태를 서버에서 검증한다.
- 중요한 비즈니스 로직에는 짧은 주석을 남겨 의도와 불변식을 설명한다. 주석은 코드를 반복하지 않고 판단 이유를 적는다. 코드는 한 줄에 과도한 표현식을 넣지 말고 메서드 호출·조건·인자 목록의 의미 단위로 적절히 줄바꿈해 가독성을 유지하되, 짧은 코드를 불필요하게 세로로 쪼개지 않는다.

## API 문서와 테스트

- API 문서의 기준은 Spring REST Docs 테스트다. 테스트에서 생성한 OpenAPI 문서를 Scalar로 렌더링한다.
- REST Docs와 OpenAPI 산출물 사이에는 `restdocs-api-spec` 계열 adapter를 사용한다. Scalar는 OpenAPI 문서의 표시 계층이며 API 계약을 별도로 작성하지 않는다.
- Controller에 문서용 annotation과 설명을 과도하게 넣지 않는다. 요청·응답 필드, 예제, 오류 계약은 Controller 통합 테스트에서 검증하고 문서화한다.
- `dev` 대상 PR과 `dev → main` Release PR의 Controller 통합 테스트에서 REST Docs snippet과 OpenAPI 파일을 생성한다. 생성된 파일은 검증 후 배포 산출물로만 사용한다.
- 테스트는 `given`, `when`, `then` 단계가 드러나는 구조로 작성한다.
- 테스트 메서드에는 `@DisplayName`을 사용하고, 테스트 설명은 한글로 작성한다.
- 단위 테스트의 범위는 Service, 도메인 규칙, Query 로직으로 한정한다. 외부 HTTP와 실제 DB 연결에 의존하지 않는다.
- 통합 테스트의 범위는 인증·인가, Controller, PostgreSQL/Testcontainers DB, 실제 API 요청·응답 검증으로 한정한다.
- 테스트 종류는 단위 테스트와 통합 테스트만 사용한다. 별도의 보안 변환기·설정·Modulith 구조·단순 enum 매핑 테스트를 추가하지 않으며, 해당 동작은 필요한 통합 테스트에서 검증한다.
- 하나의 동작을 단위 테스트·보안 테스트·Controller 테스트로 중복 검증하지 않는다. 같은 통합 경로에서 인증·인가·DB·실제 응답을 함께 확인하고, 테스트 클래스와 시나리오는 최소한으로 유지한다.
- 구현 계획이나 TDD 절차를 이유로 위 범위를 벗어난 테스트를 추가하지 않는다. 새 테스트를 만들기 전 단위(Service·도메인 규칙·Query) 또는 통합(인증·Controller·DB·실제 API) 범위를 먼저 명시한다.
- DTO·record의 accessor와 정적 팩토리, `ApiResponse`·`PageResponse`·`SliceResponse`의 단순 직렬화, `List.copyOf`, ErrorCode enum, common filter/config, GlobalExceptionHandler 단독 동작은 별도 단위 테스트를 만들지 않는다. 실제 Controller 통합 테스트의 응답·오류 계약으로 검증한다.
- OAuth2 인증 기반을 기능 모듈보다 먼저 구현하고, 인증 통합 테스트가 통과한 뒤 회원·콘텐츠 기능을 추가한다.
- 실제 Supabase JWKS 자동 통합 테스트는 기본 테스트에 포함하지 않는다. 일반 통합 테스트는 테스트용 JwtDecoder로 인증 흐름을 검증하고, 인증 설정 변경이나 운영 전환 시에만 로컬 Supabase 토큰으로 수동 smoke 검증한다.
- 통합 테스트가 통과해야 REST Docs snippet과 OpenAPI 문서를 갱신할 수 있다. 문서 생성 실패를 테스트 성공으로 취급하지 않는다.

## 검증

변경 후 최소한 다음을 실행한다.

```bash
./gradlew compileJava processResources --no-daemon --console=plain
./gradlew test --no-daemon --console=plain
```

DB를 사용하는 변경은 필요한 외부 서비스가 실행 중일 때 추가로 확인한다. 테스트가 외부 서비스 미실행으로 실패하면 실패 원인과 실행 조건을 구분해 보고한다. 연결 오류를 숨기기 위해 테스트용 H2나 임시 설정을 추가하지 않는다.
