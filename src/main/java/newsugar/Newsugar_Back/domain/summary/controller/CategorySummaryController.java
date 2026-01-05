package newsugar.Newsugar_Back.domain.summary.controller;

import newsugar.Newsugar_Back.common.ApiResult;
import newsugar.Newsugar_Back.domain.summary.service.CategorySummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
@Validated
public class CategorySummaryController {
    private CategorySummaryService categorySummaryService;
    private final String[] categories = {"politics", "economy","society", "culture", "tech", "entertainment", "opinion"};


    public CategorySummaryController(CategorySummaryService categorySummaryService){
        this.categorySummaryService =  categorySummaryService;
    }

    @PostMapping("/category-summary/generate")
    public void generateCategorySummary (){
        for (String category : categories) {
            String summary = categorySummaryService.generateCategorySummary(category);
            categorySummaryService.saveInRedis(category, summary);
            System.out.println("Category: " + category + ", Summary: " + summary);
        }
    }

    @GetMapping("/category-summary/{category}")
    public ResponseEntity<ApiResult<String>> getCategorySummary(@PathVariable String category) {
        String redisKey = "category_summary:v2:" + category;
        String summary = categorySummaryService.getSummary(redisKey);
        return ResponseEntity.ok(ApiResult.ok(summary));
    }
}
