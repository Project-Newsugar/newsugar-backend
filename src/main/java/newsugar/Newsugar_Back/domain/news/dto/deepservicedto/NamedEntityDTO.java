package newsugar.Newsugar_Back.domain.news.dto.deepservicedto;

import java.util.List;

public record NamedEntityDTO(
        String type,
        String exchange,
        String market,
        String symbol,
        String name,
        List<Object> ceo,
        String company_rid,
        String business_rid,
        String industry_id,
        Boolean is_external_audit,
        Integer count
) {}