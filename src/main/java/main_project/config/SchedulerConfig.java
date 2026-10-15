package main_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

//  =====================================================================
//  @Scheduled 작업 전용 일꾼 스레드 (ChatService ⑬ 채팅 배치 저장 , ExchangerateService 환율 갱신)
//
//  - 왜 필요한가
//      웹소켓(STOMP)을 켜면 스프링이 웹소켓 브로커용 스케줄러(messageBrokerTaskScheduler)를 만듦
//      → 따로 정해 주지 않으면 @Scheduled 작업도 그 스케줄러를 빌려 씀
//        (콘솔 로그의 [essageBroker-17] = 채팅 배치 저장이 웹소켓 브로커 스레드에서 돌던 흔적)
//      → DB 저장이 길어지면 웹소켓 쪽 작업(메시지 전달 등)까지 잠깐 밀릴 수 있음
//      → @Scheduled 만 쓰는 일꾼을 따로 만들어서 나눔
//
//  - 이름이 "taskScheduler" 인 이유 : 스케줄러가 여러 개일 때 @Scheduled 는 이 이름의 것을 골라 씀 (스프링 규칙)
//  - 콘솔 로그에 [scheduled-1] 처럼 찍히면 이 일꾼에서 돈 것
//  =====================================================================
@Configuration
public class SchedulerConfig {

    @Bean (name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler(){

    //  일꾼 2명 : 채팅 배치 저장(3초마다) · 환율 갱신(10분마다)이 서로 기다리지 않게
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduled-");
        return scheduler;   // @Bean 으로 등록하면 스프링이 initialize( ) 와 종료 정리를 알아서 함
    }

}
