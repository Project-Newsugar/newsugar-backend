package newsugar.Newsugar_Back.domain.news.service;

import io.github.cdimascio.dotenv.Dotenv;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.DeepSearchResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

        // 검색어에 공백이 있으면 따옴표로 감싸서 정확한 Phrase 검색 유도
        String searchKeyword = keyword;
        if (keyword != null && keyword.contains(" ") && !keyword.startsWith("\"")) {
            searchKeyword = "\"" + keyword + "\"";
        }

        String url = UriComponentsBuilder
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
                .toUriString();

        System.out.println("DeepSearch URL = " + url);

        // API 호출
        return restTemplate.getForObject(url, DeepSearchResponseDTO.class);
    }
}