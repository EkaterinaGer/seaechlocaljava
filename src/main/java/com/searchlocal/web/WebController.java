package com.searchlocal.web;

import com.searchlocal.dto.StatisticsResponse;
import com.searchlocal.service.SearchService;
import com.searchlocal.model.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class WebController {
    
    @Autowired
    private SearchService searchService;
    
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
    
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> search(@RequestParam String query, 
                                      @RequestParam(required = false) String site) {
        List<SearchResult> results = searchService.search(query, site);
        return Map.of(
                "result", true,
                "count", results.size(),
                "data", results
        );
    }
}

