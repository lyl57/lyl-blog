package com.lyl57.service.impl;

import com.lyl57.config.ElasticsearchConfiguration;
import com.lyl57.service.SearchService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author Created by lyl57 on 2018/3/1
 */
@Service
public class SearchServiceImpl implements SearchService {
    Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    @Autowired
    private TransportClient client;
    String json = null;

    /**
     * demo，模糊查询
     *
     * @param value
     * @return
     */
    @Override
    public String search(String value) {
        try {
            if (!StringUtils.isEmpty(value)) {
                WildcardQueryBuilder wqb = QueryBuilders.wildcardQuery("message", "*" + value + "*");
                //  SimpleQueryStringBuilder sqs = QueryBuilders.simpleQueryStringQuery(value);
                SearchResponse response = client.prepareSearch().setQuery(wqb).setFrom(0).setSize(1000).execute().actionGet();
                Iterator<SearchHit> iterator = response.getHits().iterator();
                Map<String, String> mm = new HashMap<String, String>();
                List<Map> list = new ArrayList<Map>();
                while (iterator.hasNext()) {
                    Map<String, Object> map = iterator.next().getSource();
                    // mm.put("id", iterator.next().getId());
                    mm.put("message", map.get("message").toString());
                    list.add(mm);
                }
//                if (list.size() > 0) {
//                    json = JsonConvert.convertToJson(new MessageBean(true, JsonConvert.convertToJson(list)));
//                } else {
//                    json = JsonConvert.convertToJson(new MessageBean(false
//                            , Constant.SEARCH_NO_DATA));
//                }

            }
        } catch (Exception e) {
//            logger.error(Constant.SEARCH_FALSE, e);
//            json = JsonConvert.convertToJson(new MessageBean(false
//                    , Constant.SEARCH_FALSE));
        }
        return json;
    }


}