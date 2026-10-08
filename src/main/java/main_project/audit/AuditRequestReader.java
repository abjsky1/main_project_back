package main_project.audit;

import java.lang.reflect.Method;
import java.util.Map;

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
import main_project.model.dto.MemberDto;
import main_project.model.entity.MemberEntity;
import main_project.model.repository.MemberRepository;
import main_project.util.JwtUtil;

//  =====================================================================
//  감사 로그 도우미 ① : 요청(HttpServletRequest)에서 필요한 정보 꺼내기
//
//  AOP 와는 상관없는 "일반 도우미" 코드라서 AuditAspect 에서 따로 떼어 냄.
//  - 지금 처리 중인 요청 꺼내기
//  - 요청 주소의 모양(패턴) , 주소 안의 {번호}
//  - 접속 IP
//  - 요청 body(DTO) 안의 값 (userEmail , rejectReason 등)
//  - 누가 요청했는지 (회원 번호)
//  =====================================================================
@Component
@RequiredArgsConstructor   // 아래 final 필드들을 스프링이 자동으로 넣어 줌 (생성자 주입)
public class AuditRequestReader {

//  로그인 성공 시 LoginController 가 만들어 주는 출입증(JWT) 쿠키 이름
//  ※ LoginController 의 쿠키 이름이 바뀌면 여기도 같이 바꿔야 함
    private static final String LOGIN_COOKIE = "AccessToken";

    private final MemberRepository memberRepository;   // 이메일로 회원 찾기
    private final JwtUtil jwtUtil;                     // 출입증(JWT)에서 회원 번호 읽기


//  ---------------------------------------------------------------------
//  1. 지금 처리 중인 요청 꺼내기
//  스프링은 요청이 들어오면 그 요청을 잠깐 "보관함(RequestContextHolder)"에 넣어 둠.
//  AOP 코드에서는 컨트롤러처럼 요청을 매개변수로 받을 수 없으니 여기서 꺼내 씀.
//  ---------------------------------------------------------------------
    public HttpServletRequest currentRequest(){
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes) {
            ServletRequestAttributes servletAttributes = (ServletRequestAttributes) attributes;
            return servletAttributes.getRequest();
        }
        return null;   // 웹 요청이 아닌 경우 (예: 서버 시작 중 실행되는 코드)
    }


//  ---------------------------------------------------------------------
//  2. 요청 주소의 "모양(패턴)"
//  실제 주소가 /api/cscore/12 이면 → "/api/cscore/{cscore1Id}"
//  스프링이 어느 컨트롤러 메소드로 보낼지 정할 때 요청에 적어 둔 값을 꺼냄.
//  등록표(AuditTargets)는 이 패턴으로 찾기 때문에 번호가 달라도 같은 줄을 찾을 수 있음.
//  ---------------------------------------------------------------------
    public String urlPattern(HttpServletRequest request){
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return null;
        }
        return pattern.toString();
    }


//  ---------------------------------------------------------------------
//  3. 주소 안의 {이름} 값
//  예) 패턴 /api/authorization/{memberId}/role , 실제 주소 /api/authorization/abc-123/role
//      → pathValue(request, "memberId") = "abc-123"
//  ---------------------------------------------------------------------
    public String pathValue(HttpServletRequest request, String name){
        Map<?, ?> variables = pathVariables(request);
        if (variables == null || variables.get(name) == null) {
            return null;
        }
        return variables.get(name).toString();
    }

//  주소 안의 첫 번째 {번호} 값 (이름을 몰라도 됨)  예) /api/cscore/12 → "12"
    public String firstPathValue(HttpServletRequest request){
        Map<?, ?> variables = pathVariables(request);
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        return String.valueOf(variables.values().iterator().next());
    }

//  스프링이 요청에 적어 둔 { 이름 → 값 } 목록
    private Map<?, ?> pathVariables(HttpServletRequest request){
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map) {
            return (Map<?, ?>) variables;
        }
        return null;
    }


//  ---------------------------------------------------------------------
//  4. 주소 뒤 ?이름=값 (쿼리 파라미터)   예) /api/matching/reject/5?rejectReason=일정 불일치
//  ---------------------------------------------------------------------
    public String requestParam(HttpServletRequest request, String name){
        return request.getParameter(name);
    }


//  ---------------------------------------------------------------------
//  5. 접속 IP
//  중간 다리(Vite 개발 서버 , cloudflared 등)를 거치면 진짜 IP 는 X-Forwarded-For 헤더의 첫 값에 적혀 옴.
//  없으면 서버가 직접 본 주소(getRemoteAddr)를 씀.
//  ---------------------------------------------------------------------
    public String clientIp(HttpServletRequest request){
        String ip;
        String forwarded = request.getHeader("X-Forwarded-For");   // "실제IP, 중간IP1, 중간IP2 ..."

        if (forwarded != null && !forwarded.isBlank()) {
            ip = forwarded.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

    //  보기 쉽게 바꾸기
        if (ip.startsWith("::ffff:")) {                               // IPv6 모양으로 적힌 IPv4  예) ::ffff:127.0.0.1
            ip = ip.substring(7);
        }
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {      // 내 컴퓨터(localhost)
            ip = "127.0.0.1";
        }
        return ip;
    }


//  ---------------------------------------------------------------------
//  6. 컨트롤러가 받은 값(매개변수)들 중에서 원하는 칸 값 꺼내기
//
//  예) 로그인 컨트롤러는 LoginDto 를 받는데 , 그 안의 userEmail 이 필요할 때
//      readField(args, "userEmail") → "admin@naver.com"
//
//  팀원 DTO 클래스를 몰라도 꺼낼 수 있게 "리플렉션"을 씀.
//  리플렉션 = 이름(글자)으로 메소드를 찾아서 실행하는 자바 기능
//    1) 칸 이름 userEmail → 꺼내는 메소드 이름 getUserEmail 만들기 (Lombok @Data 가 만들어 둔 getter)
//    2) 매개변수 객체마다 그 이름의 메소드가 있는지 찾아보고 , 있으면 실행해서 값 꺼내기
//  → DTO 가 바뀌어도 칸 이름만 같으면 계속 동작
//  ---------------------------------------------------------------------
    public Object readField(Object[] args, String fieldName){
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        for (Object arg : args) {

        //  값이 없거나 , 요청/응답 객체 자체는 건너뜀
            if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse) {
                continue;
            }

        //  Map 으로 받은 경우 : 이름으로 바로 꺼내기
            if (arg instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) arg;
                if (map.get(fieldName) != null) {
                    return map.get(fieldName);
                }
                continue;
            }

        //  DTO 객체인 경우 : getXxx() 메소드를 이름으로 찾아서 실행
            try {
                Method getter = arg.getClass().getMethod(getterName);   // 그 이름의 메소드 찾기 (없으면 예외)
                Object value = getter.invoke(arg);                      // 찾은 메소드 실행 = arg.getUserEmail()
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException e) {
            //  이 매개변수에는 그 칸이 없음 → 다음 매개변수 확인
            }
        }
        return null;
    }


//  ---------------------------------------------------------------------
//  7. 누가 요청했는지 (회원 번호) 찾기
//  못 찾으면 null → AuditService 가 비회원(GUEST)으로 저장
//  ---------------------------------------------------------------------
    public String findMemberId(HttpServletRequest request, Object[] args, Object result){

    //  ① 방금 로그인에 성공한 경우 : LoginController 가 돌려준 회원 정보
    //     (로그인하는 순간에는 아직 출입증 쿠키가 없어서 이렇게 찾음)
        if (result instanceof MemberDto) {
            MemberDto memberDto = (MemberDto) result;
            return memberDto.getMemberId();
        }

    //  ② 로그인한 상태 : 출입증(JWT) 쿠키에서 회원 번호 읽기 (만료되었거나 가짜면 null)
        String token = cookieValue(request, LOGIN_COOKIE);
        if (token != null) {
            String memberId = jwtUtil.getMemberIdFromToken(token);
            if (memberId != null) {
                return memberId;
            }
        }

    //  ③ 쿠키가 없을 때 : 요청 body 의 userEmail 로 회원 찾기 (로그인 실패 , 회원 가입)
        Object userEmail = readField(args, "userEmail");
        if (userEmail != null) {
            MemberEntity memberEntity = memberRepository.findByUserEmail(userEmail.toString()).orElse(null);
            if (memberEntity != null) {
                return memberEntity.getMemberId();
            }
        }

        return null;
    }

//  쿠키 값 꺼내기 (브라우저가 보낸 쿠키들 중 이름이 같은 것)
    private String cookieValue(HttpServletRequest request, String name){
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
