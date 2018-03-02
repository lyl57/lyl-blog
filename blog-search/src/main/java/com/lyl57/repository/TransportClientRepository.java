package com.lyl57.repository;

import com.alibaba.fastjson.JSON;
import com.lyl57.common.ElasticSearchPage;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;

@Component
public class TransportClientRepository {

    private static final Logger log = LoggerFactory.getLogger(TransportClientRepository.class);

    @Autowired
    private TransportClient client;

    public TransportClientRepository(TransportClient client) {
        super();
        this.client = client;
    }

    /**
     * 创建搜索引擎文档
     *
     * @param index 索引名称
     * @param type  索引类型
     * @param id    索引id
     * @param doc
     * @return
     */
    public String saveDoc(String index, String type, String id, Object doc) {
        IndexResponse response = client.prepareIndex(index, type, id).setSource(getXContentBuilderKeyValue(doc)).get();
        return response.getId();
    }

    /**
     * 更新文档
     *
     * @param index
     * @param type
     * @param id
     * @param doc
     * @return
     */
    public String updateDoc(String index, String type, String id, Object doc) {
        UpdateResponse response = client.prepareUpdate(index, type, id).setDoc(getXContentBuilderKeyValue(doc)).get();
        return response.getId();
    }

    /**
     * 删除索引
     *
     * @param index
     * @param type
     * @param id
     * @return
     */
    public String deleteById(String index, String type, String id) {
        DeleteResponse response = client.prepareDelete(index, type, id).get();
        return response.getId();
    }

    /**
     * 获取索引对应的存储内容
     *
     * @param index
     * @param type
     * @param id
     * @return
     */
    public String getIdx(String index, String type, String id) {
        GetResponse response = client.prepareGet(index, type, id).get();
        if (response.isExists()) {
            return response.getSourceAsString();
        }
        return null;
    }

    /**
     * 对象转换
     *
     * @param t
     * @param src
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> T parseObject(T t, String src) {
        try {
            return (T) JSON.parseObject(src, t.getClass());
        } catch (Exception e) {
            log.error("解析失败，{}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取索引对应的存储内容自动转换成对象的方式
     *
     * @param index
     * @param type
     * @param id
     * @param t
     * @return
     */
    public <T> T getIdx(String index, String type, String id, T t) {
        return parseObject(t, getIdx(index, type, id));
    }

    public void searchFullText(String filed, String queryValue, int pageNum, int pageSize, String... indexs) {
        QueryBuilder builder = QueryBuilders.matchQuery(filed, queryValue);
        SearchResponse scrollResp = client.prepareSearch(indexs).addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                .setFrom(pageNum * pageSize).setSize(pageSize).setScroll(newTimeValue(60000)).setQuery(builder).get();
        do {
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                System.out.println("result:+++++" + hit.getSourceAsString());
            }
            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(newTimeValue(60000)).execute()
                    .actionGet();
        } while (scrollResp.getHits().getHits().length != 0);
    }

    private String newTimeValue(int i) {
        return null;
    }

    /**
     * 全文搜索
     *
     * @param param
     * @param page
     * @param indexs
     * @return
     */
    public <T> ElasticSearchPage<T> searchFullText(T param, ElasticSearchPage<T> page, String... indexs) {
        QueryBuilder builder = null;
        Map<String, Object> map = getObjectMap(param);
        if (map == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                // builder =QueryBuilders.wildcardQuery( entry.getKey(), "*" +
                // entry.getValue().toString()+ "*" );
                builder = QueryBuilders.matchQuery(entry.getKey(), entry.getValue());
                // builder =QueryBuilders.multiMatchQuery(text, fieldNames)(
                // entry.getKey(),entry.getValue());
            }
        }
        HighlightBuilder highlight = new HighlightBuilder();
        highlight.field("title").field("description");
        SearchResponse scrollResp = client.prepareSearch(indexs).setFrom(page.getPageNum() * page.getPageSize())
                .highlighter(highlight).setSize(page.getPageSize())
                // .setScroll(newTimeValue(60000))
                .setQuery(builder).get();
        List<T> result = new ArrayList<>();
        // ElasticSearchPage<T> ret = new ElasticSearchPage<>();
        for (SearchHit hit : scrollResp.getHits().getHits()) {
            try {
                Map<String, HighlightField> highlightResult = hit.getHighlightFields();
                highlightResult.get("description");
                result.add(parseObject(param, hit.getSourceAsString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        page.setTotal(scrollResp.getHits().totalHits);
        page.setParam(param);
        page.setResultList(result);
        return page;
    }

    public static Map<String, Object> getObjectMap(Object o) {
        List<Field> fieldList = new ArrayList<Field>();
        @SuppressWarnings("rawtypes")
        Class tempClass = o.getClass();
        while (tempClass != null) {
            fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            tempClass = tempClass.getSuperclass();
        }
        try {
            Map<String, Object> result = new HashMap<>(16);
            for (Field field : fieldList) {
                PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), o.getClass());
                result.put(field.getName(), descriptor.getReadMethod().invoke(o));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断某个索引下type是否存在
     *
     * @param index
     * @param type
     * @return
     */
    public boolean isTypeExist(String index, String type) {
        return client.admin().indices().prepareTypesExists(index).setTypes(type).execute().actionGet().isExists();
    }

    /**
     * 判断索引是否存在
     *
     * @param index
     * @return
     */
    public boolean isIndexExist(String index) {
        return client.admin().indices().prepareExists(index).execute().actionGet().isExists();
    }

    /**
     * 创建type（存在则进行更新）
     *
     * @param index 索引名称
     * @param type  type名称
     * @param o     要设置type的object
     * @return
     */
    public boolean createType(String index, String type, Object o) {
        if (!isIndexExist(index)) {
            log.error("{}索引不存在", index);
            return false;
        }
        try {
            // 若type存在则可通过该方法更新type
            return client.admin().indices().preparePutMapping(index).setType(type).setSource(o).get().isAcknowledged();
        } catch (Exception e) {
            log.error("创建type失败，{}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

//    private static Map getXContentBuilderKeyValue(Object o) {
//        try {
//            Map map = JSON.parseObject(JSON.toJSONString(o), HashMap.class);
//            log.debug(map.toString());
//            return map;
//        } catch (Exception e) {
//            log.error("获取object key-value失败，{}", e.getMessage());
//        }
//        return null;
//    }

    public static XContentBuilder getXContentBuilderKeyValue(Object o) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            List<Field> fieldList = new ArrayList<>();
            @SuppressWarnings("rawtypes")
            Class tempClass = o.getClass();
            while (tempClass != null) {// 当父类为null的时候说明到达了最上层的父类(Object类).
                fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
                tempClass = tempClass.getSuperclass();// 得到父类,然后赋给自己
            }
            for (Field field : fieldList) {
                PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), o.getClass());
                Object value = descriptor.getReadMethod().invoke(o);
                if (value != null) {
                    //指定es字段类型，缺少再补
                    if (value instanceof Byte) {
                        builder.field(field.getName(), (Byte) value);
                    } else if (value instanceof Integer) {
                        builder.field(field.getName(), (Integer) value);
                    } else if (value instanceof Long) {
                        builder.field(field.getName(), (Long) value);
                    } else if (value instanceof Double) {
                        builder.field(field.getName(), (Double) value);
                    } else if (value instanceof Float) {
                        builder.field(field.getName(), (Float) value);
                    } else if (value instanceof String) {
                        builder.field(field.getName(), (String) value);
                    } else if (value instanceof Date) {
                        builder.field(field.getName(), (Date) value);
                    } else {
                        builder.field(field.getName(), value);
                    }
                }
            }
            builder.endObject();
            log.debug(builder.string());
            return builder;
        } catch (Exception e) {
            log.error("获取object key-value失败，{}", e.getMessage());
        }
        return null;
    }
}