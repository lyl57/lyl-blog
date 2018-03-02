package com.lyl57.controller;

import com.lyl57.common.ElasticSearchPage;
import com.lyl57.domain.Article;
import com.lyl57.repository.TransportClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Created by lyl57 on 2018/2/24
 */
@RestController
public class TestController {

    @Autowired
    TransportClientRepository repository;

    @RequestMapping("/add")
    public void testSaveArticleIndex(@RequestBody Article article) {
        article.setCreateTime(new Date());
        repository.saveDoc("blog", "articles", "1", article);
    }


    @RequestMapping("/query")
    public ElasticSearchPage testSearch(@RequestBody Article article) {
        ElasticSearchPage<Article> searchPage = new ElasticSearchPage<>();
        searchPage = repository.searchFullText(article, searchPage, "blog");
        return searchPage;
    }
}
