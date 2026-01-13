package newsugar.Newsugar_Back.domain.news.service;

import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.ArticleDTO;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.DeepSearchResponseDTO;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class RssNewsService {

    // Google News RSS URL (한국 뉴스)
    private static final String FEED_URL = "https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko";
    private final HttpClient client = HttpClient.newHttpClient();

    // 뉴스 헤드라인 조회 (RSS)
    public DeepSearchResponseDTO getTopHeadlines(Integer page, Integer page_size) {
        int p = page != null ? page : 1;
        int sz = page_size != null ? page_size : 10;

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(FEED_URL))
                    .header("Accept", "application/rss+xml")
                    .build();
            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() >= 300) {
                // 요청 실패 시 빈 리스트 반환
                return new DeepSearchResponseDTO(null, 0, 0, p, sz, List.of());
            }

            // XML 파싱 로직
            byte[] bytes = res.body();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(bytes));
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            List<ArticleDTO> all = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = textContent(item, "title");
                String link = textContent(item, "link");
                String desc = textContent(item, "description");
                String pubDate = textContent(item, "pubDate");
                String imageUrl = null;

                // 썸네일 이미지 추출
                NodeList enclosures = item.getElementsByTagName("enclosure");
                if (enclosures.getLength() > 0) {
                    Element enc = (Element) enclosures.item(0);
                    imageUrl = enc.getAttribute("url");
                }

                String publisher = "Google News";
                // 고유 ID 생성 (링크 또는 제목 해시)
                String id = link != null ? link : (title != null ? Integer.toString(title.hashCode()) : Integer.toString(i));
                String publishedAt = pubDate;
                if (publishedAt != null) {
                    try {
                        // 날짜 포맷 유지
                        publishedAt = publishedAt;
                    } catch (Exception ignored) {}
                }

                ArticleDTO dto = new ArticleDTO(
                        id,
                        List.of("top"),
                        title,
                        publisher,
                        null,
                        desc,
                        null,
                        null,
                        imageUrl,
                        null,
                        link,
                        null,
                        null,
                        null,
                        null,
                        publishedAt,
                        null
                );
                all.add(dto);
            }

            int totalItems = all.size();
            int totalPages = sz > 0 ? (int)Math.ceil(totalItems / (double)sz) : 0;
            int fromIdx = Math.max(0, (p - 1) * sz);
            int toIdx = Math.min(totalItems, fromIdx + sz);
            List<ArticleDTO> pageData = fromIdx < toIdx ? all.subList(fromIdx, toIdx) : List.of();

            return new DeepSearchResponseDTO(null, totalItems, totalPages, p, sz, pageData);
        } catch (Exception e) {
            return new DeepSearchResponseDTO(null, 0, 0, p, sz, List.of());
        }
    }

    private String textContent(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        String v = nodes.item(0).getTextContent();
        return v != null ? v.trim() : null;
    }
}

