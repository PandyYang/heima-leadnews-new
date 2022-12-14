package com.heima.search.listener;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.search.vos.SearchArticleVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SyncArticleListener {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @KafkaListener(topics = ArticleConstants.ARTICLE_ES_SYNC_TOPIC)
    public void onMessage(String msg) {
        if (StringUtils.isNotBlank(msg)) {

            log.info("SyncArticleListener, receive msg = {}", msg);

            SearchArticleVo searchArticleVo = JSON.parseObject(msg, SearchArticleVo.class);

            IndexRequest indexRequest = new IndexRequest("app_info_article");
            indexRequest.id(searchArticleVo.getId().toString());
            indexRequest.source(msg, XContentType.JSON);

            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("sync article to es error = {}", e.getMessage());
            }
        }
    }
}
