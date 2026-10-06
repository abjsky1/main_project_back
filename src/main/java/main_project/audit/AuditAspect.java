package main_project.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main_project.model.dto.AuditSaveDto;
import main_project.service.AuditService;

//  =====================================================================
//  감사 로그 자동 기록 AOP
//
//  [AOP 가 뭔가요?]
//  "여러 메소드에 공통으로 필요한 일"을 한 곳에 모아서 , 원래 메소드 코드를 고치지 않고 끼워 넣는 방법.
//  여기서는 "누가 무엇을 했는지 기록하기"를 모든 컨트롤러에 끼워 넣음.
//  → 팀원 컨트롤러 / 서비스 코드는 한 줄도 고치지 않아도 로그가 쌓임.
//
//  [이 파일에서 쓰는 AOP 용어]
//  - Aspect(애스펙트)     : 공통 기능을 모아 둔 클래스 = 이 클래스 (@Aspect)
//  - Advice(어드바이스)   : 끼워 넣을 실제 코드 = writeAuditLog() 메소드
//      @Around            : 원래 메소드의 "앞과 뒤"를 감싸서 실행 (앞에서 확인 → 원래 메소드 실행 → 뒤에서 기록)
//  - Pointcut(포인트컷)   : 어디에 끼워 넣을지 정하는 규칙
//      "within(main_project.controller..*)" = controller 패키지(하위 포함) 안의 모든 클래스의 메소드
//  - JoinPoint(조인포인트): 끼워 넣어진 "그 순간의 원래 메소드" 정보 (어떤 메소드인지 , 받은 값 , 반환 타입 ...)
//      ProceedingJoinPoint.proceed() 를 호출해야 원래 컨트롤러 메소드가 실행됨
//      (proceed 를 안 부르면 원래 메소드는 실행되지 않으니 꼭 불러야 함!)
//
//  [흐름]
//   요청 → 스프링이 컨트롤러 메소드를 부르려는 순간 → writeAuditLog() 가 먼저 실행됨
//     [1] 지금 요청 꺼내기
//     [2] 등록표(AuditTargets)에 있는 API 인지 확인 → 없으면 원래 메소드만 실행하고 끝
//     [3] joinPoint.proceed() 로 원래 컨트롤러 메소드 실행
//     [4] 결과를 보고 성공/실패 판단 → 감사 로그 저장 (AuditService.record)
//     [5] 원래 결과를 그대로 돌려줌 (화면이 받는 응답은 AOP 가 없을 때와 똑같음)
//
//  [역할 나누기]
//   - AuditAspect        : AOP 흐름 (이 파일)
//   - AuditTargets       : 어떤 API 를 기록할지 적어 둔 등록표
//   - AuditRequestReader : 요청에서 IP , 회원 , 주소 번호 , body 값 꺼내기
//   - AuditDetailMaker   : 화면 "대상" 칸 문구 만들기
//   - AuditService       : DB(audit 테이블)에 저장
//  =====================================================================
@Slf4j                      // log.warn(...) 으로 콘솔에 경고를 남길 수 있게 해 줌
@Aspect                     // "이 클래스는 AOP(애스펙트)입니다"
@Component                  // 스프링이 서버를 켤 때 이 클래스를 만들어서 등록 (등록해야 AOP 가 동작함)
@RequiredArgsConstructor    // 아래 final 필드들을 스프링이 자동으로 넣어 줌
public class AuditAspect {

    private final AuditTargets auditTargets;               // 등록표
    private final AuditRequestReader requestReader;        // 요청 정보 꺼내기 도우미
    private final AuditDetailMaker detailMaker;            // "대상" 문구 만들기 도우미
    private final AuditService auditService;               // 감사 로그 저장


//  ---------------------------------------------------------------------
//  Advice : controller 패키지의 모든 메소드를 감싸서 실행
//  반환값(Object) = 원래 컨트롤러 메소드가 돌려준 값 → 그대로 돌려줘야 화면이 정상 응답을 받음
//  throws Throwable = 원래 메소드에서 난 오류도 그대로 밖으로 내보냄
//  ---------------------------------------------------------------------
    @Around("within(main_project.controller..*)")
    public Object writeAuditLog(ProceedingJoinPoint joinPoint) throws Throwable {

    //  [1] 지금 처리 중인 요청 꺼내기 (웹 요청이 아니면 기록하지 않고 원래 메소드만 실행)
        HttpServletRequest request = requestReader.currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

    //  [2] 등록표에서 찾기  예) "DELETE" + "/api/cscore/{cscore1Id}"
    //      등록표에 없는 API 면 기록하지 않고 원래 메소드만 실행
        String httpMethod = request.getMethod();
        String urlPattern = requestReader.urlPattern(request);
        AuditTarget target = auditTargets.find(httpMethod, urlPattern);

        if (target == null) {
            return joinPoint.proceed();
        }

    //  [3] 원래 컨트롤러 메소드 실행
        Object result;
        try {
            result = joinPoint.proceed();

        } catch (Throwable error) {
        //  원래 메소드에서 오류가 났을 때 : "실패"로 기록하고 , 오류는 그대로 다시 던짐
        //  (오류를 여기서 삼키면 원래 동작이 바뀌어 버리니까)
            saveAuditLog(joinPoint, request, target, null, error);
            throw error;
        }

    //  [4] 원래 메소드가 끝났을 때 : 결과를 보고 성공/실패 판단해서 기록
        saveAuditLog(joinPoint, request, target, result, null);

    //  [5] 원래 결과를 그대로 돌려줌
        return result;
    }


//  ---------------------------------------------------------------------
//  감사 로그 1건 저장
//  기록하다가 어떤 오류가 나도 여기서 잡아서 경고만 남김
//  → 로그 저장 실패 때문에 사용자의 원래 요청(로그인 , 조회 등)이 실패하면 안 되니까
//  ---------------------------------------------------------------------
    private void saveAuditLog(ProceedingJoinPoint joinPoint, HttpServletRequest request, AuditTarget target, Object result, Throwable error){
        try {
            Object[] args = joinPoint.getArgs();                       // 컨트롤러가 받은 값들 (요청 body DTO 등)
            boolean success = isSuccess(joinPoint, result, error);     // 성공했나?
            String actionType = target.getActionType(success);         // 성공/실패에 맞는 작업 유형

        //  저장할 내용을 쪽지(AuditSaveDto)에 담기
            AuditSaveDto auditSaveDto = AuditSaveDto.builder()
                .memberId(requestReader.findMemberId(request, args, result))                       // 누가 (못 찾으면 null → 비회원)
                .actionType(actionType)                                                            // 무슨 작업
                .actionDetail(detailMaker.makeDetail(target, actionType, success, request, args))  // 대상 문구
                .fipAddress(requestReader.clientIp(request))                                       // 어디서 (IP)
                .actionResult(success)                                                             // 성공 / 실패
                .build();

            auditService.record(auditSaveDto);

        } catch (Exception e) {
            log.warn("[감사로그] 저장 실패 : {} {} → {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        }
    }


//  ---------------------------------------------------------------------
//  성공 / 실패 판단 규칙 (위에서부터 차례로 확인)
//   1) 오류가 났으면                          → 실패
//   2) 반환 타입이 void (아무것도 안 돌려줌)  → 성공
//   3) 돌려준 값이 null                       → 실패  (예: 로그인 실패하면 null 을 돌려줌)
//   4) 돌려준 값이 true / false               → 그 값 그대로  (예: 삭제 실패하면 false)
//   5) ResponseEntity 의 상태 코드가 오류(4xx , 5xx) → 실패
//   6) 그 밖에는                              → 성공
//  ---------------------------------------------------------------------
    private boolean isSuccess(ProceedingJoinPoint joinPoint, Object result, Throwable error){

        if (error != null) {
            return false;
        }

    //  joinPoint 에서 원래 메소드 정보를 꺼내 반환 타입 확인
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (signature.getReturnType() == void.class) {
            return true;
        }

        if (result == null) {
            return false;
        }

        if (result instanceof Boolean) {
            return (Boolean) result;
        }

        if (result instanceof ResponseEntity) {
            ResponseEntity<?> response = (ResponseEntity<?>) result;
            return !response.getStatusCode().isError();
        }

        return true;
    }

}
