package main_project.audit;

import java.lang.reflect.Method;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main_project.model.dto.AuditSaveDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;
import main_project.service.AuditService;
import main_project.util.JwtUtil;

//  =====================================================================
//  감사 로그 자동 기록 (AOP)
//  - controller 패키지의 모든 컨트롤러 메소드 "주변(@Around)"에서 실행됨 → 팀원 코드는 수정할 필요 없음
//  - 등록표(AuditTargets)에 있는 API 만 기록 , 나머지는 그냥 통과
//
//  [흐름]
//  1) 지금 요청의 "HTTP메소드 + 주소 패턴" 으로 등록표 찾기 → 없으면 원래 메소드만 실행
//  2) 원래 컨트롤러 메소드 실행
//  3) 성공 / 실패 판단 (예외 , null 반환 , false 반환 = 실패)
//  4) 회원 , IP , 대상 문구 만들어서 AuditService.record() 로 저장
//     (저장 중 오류가 나도 원래 응답에는 영향 없음)
//  =====================================================================
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

//  로그인 성공 시 LoginController 가 만들어주는 JWT 쿠키 이름
    private static final String LOGIN_COOKIE = "login_member";

//  매칭 거절 / 반려 요청에서 사유가 담기는 필드 이름 (팀원 API 와 이름을 맞춰야 함)
    private static final String REJECT_REASON_FIELD = "rejectReason";

    private final AuditTargets auditTargets;
    private final AuditService auditService;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;


    @Around("within(main_project.controller..*)")
    public Object writeAuditLog(ProceedingJoinPoint joinPoint) throws Throwable {

    //  1. 등록표에서 찾기
        HttpServletRequest request = currentRequest();
        if (request == null) { return joinPoint.proceed(); }

        Object urlPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);   // 예) /api/cscore/{cscore1Id}
        AuditTargets.Target target = (urlPattern == null) ? null : auditTargets.find(request.getMethod(), urlPattern.toString());

        if (target == null) { return joinPoint.proceed(); }

    //  2. 원래 컨트롤러 메소드 실행
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
        //  3~4. 성공이든 실패든 기록
            saveLog(joinPoint, request, target, result, error);
        }
    }


//  감사 로그 저장 (어떤 오류가 나도 여기서 끝내서 원래 응답은 그대로 나가게)
    private void saveLog(ProceedingJoinPoint joinPoint, HttpServletRequest request, AuditTargets.Target target, Object result, Throwable error){
        try {
            Object[] args = joinPoint.getArgs();
            boolean success = isSuccess(joinPoint, result, error);
            String actionType = target.actionFor(success);

            AuditSaveDto auditSaveDto = AuditSaveDto.builder()
                .memberId(findMemberId(request, args, result))
                .actionType(actionType)
                .actionDetail(makeDetail(target, actionType, request, args))
                .fipAddress(clientIp(request))
                .actionResult(success)
                .build();

            auditService.record(auditSaveDto);

        } catch (Exception e) {
            log.warn("[감사로그] 저장 실패 : {} {} → {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        }
    }


//  ---------------------------------------------------------------------
//  대상 문구 만들기 : 등록표의 기본 문구 + 작업 유형별로 필요한 정보 덧붙이기
//  (새로운 작업 유형에 정보를 붙이고 싶으면 여기에 case 추가)
//  ---------------------------------------------------------------------
    private String makeDetail(AuditTargets.Target target, String actionType, HttpServletRequest request, Object[] args){

        String detail = target.detail();

        return switch (actionType) {

        //  로그인 / 로그인 실패 : 입력한 이메일 (미가입 이메일 시도도 그대로 남김)
            case "로그인", "로그인 실패" -> detail + " (입력 이메일: " + valueOrDash(readField(args, "userEmail")) + ")";

        //  회원 가입 : 가입한 이메일
            case "회원 가입" -> detail + " (" + valueOrDash(readField(args, "userEmail")) + ")";

        //  조건 삭제 , 매칭 실행 : 주소의 번호
            case "매칭 조건 삭제", "매칭 실행" -> detail + pathNumber(request);

        //  매칭 거절 / 반려 : 주소의 번호 + 거절 사유
            case "매칭 거절", "매칭 반려" -> detail + pathNumber(request) + " - 사유: " + rejectReason(request, args);

        //  그 외(데이터 조회 등) : 등록표 문구 그대로
            default -> detail;
        };
    }


//  ---------------------------------------------------------------------
//  성공 / 실패 판단
//  예외 발생 , null 반환 (예: 로그인 실패) , false 반환 (예: 등록/삭제 실패) , 에러 상태 ResponseEntity → 실패
//  ---------------------------------------------------------------------
    private boolean isSuccess(ProceedingJoinPoint joinPoint, Object result, Throwable error){

        if (error != null) { return false; }

        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();
        if (returnType == void.class) { return true; }

        if (result == null) { return false; }
        if (result instanceof Boolean bool) { return bool; }
        if (result instanceof ResponseEntity<?> response) { return !response.getStatusCode().isError(); }

        return true;
    }


//  ---------------------------------------------------------------------
//  회원 찾기 (못 찾으면 null → AuditService 에서 비회원 GUEST 로 저장)
//  ---------------------------------------------------------------------
    private String findMemberId(HttpServletRequest request, Object[] args, Object result){

    //  1. 로그인 성공 : LoginController 가 돌려준 회원
        if (result instanceof MemberEntity memberEntity) { return memberEntity.getMemberId(); }

    //  2. 로그인 쿠키(JWT)가 있으면 그 회원 (만료 / 위조 토큰이면 null)
        String token = cookieValue(request, LOGIN_COOKIE);
        if (token != null) {
            String memberId = jwtUtil.getMemberIdFromToken(token);
            if (memberId != null) { return memberId; }
        }

    //  3. 쿠키가 없으면 요청 body 의 userEmail 로 찾기 (로그인 실패 , 회원 가입)
        Object userEmail = readField(args, "userEmail");
        if (userEmail != null) {
            return memberRepository.findByUserEmail(userEmail.toString())
                .map(MemberEntity::getMemberId)
                .orElse(null);
        }

        return null;
    }


//  ---------------------------------------------------------------------
//  작은 도우미들
//  ---------------------------------------------------------------------

//  지금 처리 중인 HTTP 요청
    private HttpServletRequest currentRequest(){
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

//  쿠키 값 꺼내기
    private String cookieValue(HttpServletRequest request, String name){
        Cookie[] cookies = request.getCookies();
        if (cookies == null) { return null; }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) { return cookie.getValue(); }
        }
        return null;
    }

//  접속 IP : 프록시(Vite , cloudflared)를 거치면 X-Forwarded-For 첫 값이 실제 IP
    private String clientIp(HttpServletRequest request){
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
            ? forwarded.split(",")[0].trim()
            : request.getRemoteAddr();

        if (ip.startsWith("::ffff:")) { ip = ip.substring(7); }                        // IPv6 로 표기된 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) { ip = "127.0.0.1"; }    // 내 컴퓨터(localhost)
        return ip;
    }

//  주소의 {번호} → " (#12)"  (번호가 없으면 빈 문자열)
    @SuppressWarnings("unchecked")
    private String pathNumber(HttpServletRequest request){
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> map && !map.isEmpty()) {
            return " (#" + ((Map<String, String>) map).values().iterator().next() + ")";
        }
        return "";
    }

//  거절 / 반려 사유 : 주소의 ?rejectReason= → 요청 body 의 rejectReason → 없으면 "사유 미기재"
    private String rejectReason(HttpServletRequest request, Object[] args){
        String reason = request.getParameter(REJECT_REASON_FIELD);
        if (reason == null || reason.isBlank()) {
            Object value = readField(args, REJECT_REASON_FIELD);
            reason = (value == null) ? null : value.toString();
        }
        return (reason == null || reason.isBlank()) ? "사유 미기재" : reason;
    }

//  컨트롤러 메소드 인자(요청 body DTO , Map)에서 필드 값 꺼내기
//  → 특정 DTO 클래스를 몰라도 getXxx() 가 있으면 읽음 (팀원 DTO 가 바뀌어도 이름만 같으면 동작)
    private Object readField(Object[] args, String fieldName){
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        for (Object arg : args) {
            if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse) { continue; }

            if (arg instanceof Map<?, ?> map) {
                if (map.get(fieldName) != null) { return map.get(fieldName); }
                continue;
            }

            try {
                Method getter = arg.getClass().getMethod(getterName);
                Object value = getter.invoke(arg);
                if (value != null) { return value; }
            } catch (ReflectiveOperationException e) {
                // 이 인자에는 해당 필드가 없음 → 다음 인자 확인
            }
        }
        return null;
    }

    private String valueOrDash(Object value){
        return (value == null) ? "-" : value.toString();
    }

}
