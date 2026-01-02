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
            Integer page,
            Integer page_size
    ) {
        int currentPage = (page != null) ? page : 1;
        int currentPageSize = (page_size != null) ? page_size : 10;

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl("https://api-v2.deepsearch.com/v1/articles")
                        .queryParam("api_key", apiKey)
                        .queryParam("page", currentPage)
                        .queryParam("page_size", currentPageSize)
                        .queryParam("sort", "desc")
                        .queryParam("uniquify", "true");

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
                    .queryParam("sort", "desc")
                    .queryParam("uniquify", "true");
        }

        String url = builder.toUriString();
        return restTemplate.getForObject(url, DeepSearchResponseDTO.class);
    }

    public DeepSearchResponseDTO getNewsByKeyword(String keyword, Integer page, Integer page_size){

        LocalDate today = LocalDate.now();
        // 검색 정확도를 위해 최근 1년 데이터로 제한
        LocalDate dateFrom = today.minusYears(1);

        // 검색어 전처리: 정확도 향상을 위해 모든 검색어를 따옴표로 감쌈 (이미 감싸져 있지 않다면)
        // 이렇게 해야 DeepSearch가 형태소 분석을 하지 않고 정확한 키워드 매칭을 수행함
        String searchKeyword = keyword;
        if (keyword != null && !keyword.isBlank()) {
             if (!keyword.startsWith("\"") && !keyword.endsWith("\"")) {
                 searchKeyword = "\"" + keyword + "\"";
             }
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://api-v2.deepsearch.com/v1/articles")
                .queryParam("keyword", searchKeyword)
                .queryParam("sort", "desc")
                .queryParam("uniquify", "true")
                .queryParam("date_from", dateFrom.toString())
                .queryParam("page", page)
                .queryParam("page_size", page_size)
                .queryParam("api_key", apiKey)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        System.out.println("DeepSearch URL = " + uri);

        // API 호출
        DeepSearchResponseDTO response = restTemplate.getForObject(uri, DeepSearchResponseDTO.class);

        // 결과 중복 제거 (API의 uniquify가 완벽하지 않은 경우 대비)
        if (response != null && response.data() != null) {
            List<ArticleDTO> distinctArticles = response.data().stream()
                .filter(distinctByKey(article -> article.title() + "_" + article.publisher()))
                .collect(Collectors.toList());
            
            // 레코드 재생성 (데이터만 교체)
            return new DeepSearchResponseDTO(
                response.detail(),
                response.total_items(), // 전체 개수는 정확하지 않을 수 있지만 API 응답 유지
                response.total_pages(),
                response.page(),
                response.page_size(),
                distinctArticles
            );
        }

        return response;
    }

    // 중복 제거를 위한 유틸리티 메서드
    private static <T> java.util.function.Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        java.util.Set<Object> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}