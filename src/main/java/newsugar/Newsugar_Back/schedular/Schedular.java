package newsugar.Newsugar_Back.schedular;

import newsugar.Newsugar_Back.domain.summary.repository.CategorySummaryRedis;
import newsugar.Newsugar_Back.domain.summary.service.CategorySummaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Schedular {

    @Value("${ENABLE_SCHEDULER:true}")
    private boolean enableScheduler;

    private final CategorySummaryService categorySummaryService;
    private final CategorySummaryRedis categorySummaryRedis;
    private final DailyTaskService dailyTaskService;

    private final String[] categories = {"politics", "economy","society", "culture","world", "tech", "entertainment", "opinion"};
    
    // 중복 실행 방지 플래그
    private volatile boolean isCategoryTaskRunning = false;

    public Schedular(CategorySummaryService categorySummaryService,
                     CategorySummaryRedis categorySummaryRedis,
                     DailyTaskService dailyTaskService) {
        this.categorySummaryService = categorySummaryService;
        this.categorySummaryRedis = categorySummaryRedis;
        this.dailyTaskService = dailyTaskService;
    }


    // 로컬에서만 실행
    // API 쿼터 분산을 위해 매시 30분에 실행
    @Scheduled(cron = "0 30 * * * *")
    public void runDailyTask() {
        if (!enableScheduler) {
            System.out.println("스케줄러 비활성화 상태: runDailyTask 스킵");
            return;
        }

        if (isCategoryTaskRunning) {
            System.out.println("카테고리 요약 작업이 이미 실행 중입니다. 스킵합니다.");
            return;
        }
        isCategoryTaskRunning = true;
        try {
            // 중복 실행 방지: 스케줄러가 너무 빨리 돌아서 이전 작업이 끝나기 전에 또 실행되는 것을 방지
            // 하지만 카테고리별로 루프를 돌기 때문에 전체 작업 시간은 길어질 수 있음
            for (String category : categories) {
                try {
                    String summary = categorySummaryService.generateCategorySummary(category);
                    categorySummaryService.saveInRedis(category, summary);
                    System.out.println("Category: " + category + ", Summary: " + summary);
                    
                    // API 쿼터 제한을 피하기 위해 카테고리 처리 사이에 30초 대기
                    // 무료 티어 한계로 인해 대기함 (1분 -> 30초 단축)
                    Thread.sleep(30000); 
                } catch (Exception e) {
                    System.err.println("카테고리 요약 생성 중 오류 발생 (" + category + "): " + e.getMessage());
                    // 오류가 나도 다음 카테고리는 계속 진행 시도
                }
            }
        } finally {
            isCategoryTaskRunning = false;
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void generateTodayMainContent() {
        if (!enableScheduler) {
            System.out.println("스케줄러 비활성화 상태: generateTodayMainContent 스킵");
            return;
        }
        dailyTaskService.executeDailyRoutine();
    }

    // 서버 시작 시 초기 데이터 생성을 위해 실행
    // 주의: 파드 재시작이 잦을 경우 API 쿼터 급증의 원인이 됨
    @EventListener(ApplicationReadyEvent.class)
    public void initDailyContent() {
        if (!enableScheduler) {
            System.out.println("스케줄러 비활성화 상태: initDailyContent 스킵");
            return;
        }
        
        System.out.println("서버 시작: 초기 데이터 생성 로직은 과도한 API 호출 방지를 위해 비활성화합니다.");
        // System.out.println("서버 시작: 초기 뉴스 요약 및 퀴즈 생성 시작...");
        // dailyTaskService.executeDailyRoutine();
        
        // 카테고리 요약도 비동기로 시작 (API 쿼터 고려)
        // new Thread(() -> {
        //    System.out.println("서버 시작: 카테고리 요약 백그라운드 생성 시작...");
        //    runDailyTask();
        // }).start();
    }
}

