package newsugar.Newsugar_Back.domain.news.dto.deepservicedto;

public record CompanyDTO(
        String name,
        String symbol,
        String exchange,
        Object sentiment
) {}