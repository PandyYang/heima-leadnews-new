package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.ArticleSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ArticleSearchServiceImpl implements ArticleSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private final String index = "app_info_article";
    /**
     * 文章搜索
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {



        //1.检查参数
        if (dto == null || StringUtils.isBlank(dto.getSearchWords())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.设置查询条件
        SearchRequest searchRequest = new SearchRequest(index);
        SearchSourceBuilder builder = new SearchSourceBuilder();

        // bool查询用于构建复杂查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //关键字分词之后查询
        QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords())
                .field("title")
                .field("content")
                .defaultOperator(Operator.OR);

        boolQueryBuilder.must(queryStringQueryBuilder);

        // 查询小于mindata的数据
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime());
//        boolQueryBuilder.filter(rangeQueryBuilder);

        // 分页查询
        builder.from(0);
        builder.size(dto.getPageSize());

        // 发布时间倒序查询
        builder.sort("publishTime", SortOrder.DESC);

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>");
        highlightBuilder.postTags("</font>");
        builder.highlighter(highlightBuilder);

        builder.query(boolQueryBuilder);
        searchRequest.source(builder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //3.结果封装返回

        List<Map> list = new ArrayList<Map>();

        SearchHits hits = searchResponse.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            Map map = JSON.parseObject(json, Map.class);
            // 处理高亮
            if(hit.getHighlightFields() != null && hit.getHighlightFields().size() > 0) {
                Text[] titles = hit.getHighlightFields().get("title").getFragments();
                String title = StringUtils.join(titles);
                map.put("h_title", title);
            }else {
                map.put("h_title", map.get("title"));
            }
            list.add(map);
        }

        return ResponseResult.okResult(list);
    }
}
