package newsugar.Newsugar_Back.domain.news.dto.deepservicedto;

import java.util.List;

public record ArticleDTO(
        String id,
        List<String> sections,
        String title,
        String publisher,
        String author,
        String summary,
        String highlight,
        Double score,
        String image_url,
        String thumbnail_url,
        String content_url,
        Object esg,
        List<CompanyDTO> companies,
        List<EntityDTO> entities,
        List<NamedEntityDTO> named_entities,
        String published_at,
        String body
) {}