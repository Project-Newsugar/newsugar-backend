package newsugar.Newsugar_Back.domain.news.service;

import io.github.cdimascio.dotenv.Dotenv;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.DeepSearchResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.ArticleDTO;

@Service
public class NewsService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    public NewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String key = System.getenv("NEWS_API_KEY");
        if (key == null) key = dotenv.get("NEWS_API_KEY");
        this.apiKey = key;
    }

    public DeepSearchResponseDTO getNewsByCategory(
            List<String> categories,
            LocalDate dateFrom,
            Integer page,
            Integer page_size
    ) {
        int currentPage = (page != null) ? page : 1;
        int currentPageSize = (page_size != null) ? page_size : 10;
        
        // 날짜 필터링 범위 설정: 시작일(dateFrom)부터 종료일(오늘)까지
        // date_to 파라미터가 없으면 API가 과거 데이터를 포함할 수 있으므로 명시적으로 지정
        LocalDate dateTo = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("https://api-v2.deepsearch.com/v1/articles")
                        .queryParam("api_key", apiKey)
                        .queryParam("page", currentPage)
                        .queryParam("page_size", currentPageSize)
                        .queryParam("sort", "date")
                        .queryParam("uniquify", "true")
                        .queryParam("date_to", dateTo.toString());
        
        if (dateFrom != null) {
            builder.queryParam("date_from", dateFrom.toString());
        }

        // 복수 카테고리 처리
        if (categories != null && !categories.isEmpty()) {
            String categoryPath = categories.stream()
                    .map(c -> URLEncoder.encode(c, StandardCharsets.UTF_8))
                    .collect(Collectors.joining(","));

            builder = UriComponentsBuilder
                    .fromHttpUrl("https://api-v2.deepsearch.com/v1/articles/" + categoryPath)
                    .queryParam("api_key", apiKey)
                    .queryParam("page", currentPage)
                    .queryParam("page_size", currentPageSize)
                    .queryParam("sort", "date")
                    .queryParam("uniquify", "true")
                    .queryParam("date_to", dateTo.toString());
            
            if (dateFrom != null) {
                builder.queryParam("date_from", dateFrom.toString());
            }
        }

        String url = builder.toUriString();
        DeepSearchResponseDTO response = restTemplate.getForObject(url, DeepSearchResponseDTO.class);
        
        // API 응답 후 Java 레벨에서 날짜 필터링 수행 (API가 파라미터를 무시하거나 부정확한 경우 대비)
        if (response != null && response.data() != null) {
            List<ArticleDTO> filteredData = filterByDate(response.data(), dateFrom, dateTo);
            return new DeepSearchResponseDTO(
                response.detail(),
                response.total_items(),
                response.total_pages(),
                response.page(),
                response.page_size(),
                filteredData
            );
        }
        
        return response;
    }

    public DeepSearchResponseDTO getNewsByKeyword(String keyword, Integer page, Integer page_size){

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        // 검색 정확도를 위해 최근 1일(어제~오늘) 데이터로 제한 (기존 1개월 -> 1일)
        // 사용자가 과거 뉴스가 나오는 것을 극도로 꺼려하므로 범위를 매우 좁게 설정
        LocalDate dateFrom = today.minusDays(1);
        LocalDate dateTo = today;

        // 검색어 전처리: 정확도 향상을 위해 모든 검색어를 따옴표로 감쌈 (이미 감싸져 있지 않다면)
        // 이렇게 해야 DeepSearch가 형태소 분석을 하지 않고 정확한 키워드 매칭을 수행함
        String searchKeyword = keyword;
        if (keyword != null && !keyword.isBlank()) {
             if (!keyword.startsWith("\"") && !keyword.endsWith("\"")) {
                 searchKeyword = "\"" + keyword + "\"";
             }
             // 제목 검색으로 제한하여 정확도 향상 (예: 본문에만 나오는 관련 없는 기사 제외)
             if (!searchKeyword.startsWith("title:")) {
                 searchKeyword = "title:" + searchKeyword;
             }
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://api-v2.deepsearch.com/v1/articles")
                .queryParam("keyword", searchKeyword)
                .queryParam("sort", "date")
                .queryParam("uniquify", "true")
                .queryParam("date_from", dateFrom.toString())
                .queryParam("date_to", dateTo.toString())
                .queryParam("page", page)
                .queryParam("page_size", page_size)
                .queryParam("api_key", apiKey)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        System.out.println("DeepSearch URL = " + uri);

        // API 호출
        DeepSearchResponseDTO response = restTemplate.getForObject(uri, DeepSearchResponseDTO.class);

        // 결과 중복 제거 (API의 uniquify가 완벽하지 않은 경우 대비) 및 날짜 필터링
        if (response != null && response.data() != null) {
            List<ArticleDTO> distinctArticles = response.data().stream()
                .filter(distinctByKey(article -> article.title() + "_" + article.publisher()))
                .collect(Collectors.toList());
            
            // 날짜 필터링 적용
            List<ArticleDTO> filteredData = filterByDate(distinctArticles, dateFrom, dateTo);

            // 레코드 재생성 (데이터만 교체)
            return new DeepSearchResponseDTO(
                response.detail(),
                response.total_items(), // 전체 개수는 정확하지 않을 수 있지만 API 응답 유지
                response.total_pages(),
                response.page(),
                response.page_size(),
                filteredData
            );
        }

        return response;
    }

    // 날짜 필터링 헬퍼 메서드
    private List<ArticleDTO> filterByDate(List<ArticleDTO> articles, LocalDate dateFrom, LocalDate dateTo) {
        if (articles == null || articles.isEmpty()) return List.of();
        
        return articles.stream()
            .filter(article -> {
                if (article.published_at() == null) return false;
                try {
                    // 날짜 파싱 (YYYY-MM-DD 또는 ISO 형식)
                    LocalDate pubDate;
                    String pubStr = article.published_at();
                    if (pubStr.length() > 10) {
                        pubStr = pubStr.substring(0, 10);
                    }
                    pubDate = LocalDate.parse(pubStr);
                    
                    if (dateFrom != null && pubDate.isBefore(dateFrom)) return false;
                    if (dateTo != null && pubDate.isAfter(dateTo)) return false;
                    return true;
                } catch (Exception e) {
                    // 날짜 형식이 올바르지 않으면 제외
                    return false;
                }
            })
            .collect(Collectors.toList());
    }

    // 중복 제거를 위한 유틸리티 메서드
    private static <T> java.util.function.Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        java.util.Set<Object> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}