package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;


    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    /**
     * 生成静态文件 上传至minio
     * @param aparticle
     * @param content
     */
    @Override
    @Async
    public void buildArticleToMinIO(ApArticle aparticle, String content) {

        if(StringUtils.isNotBlank(content)){

            Template template = null;
            StringWriter out = new StringWriter();
            try {
                //2.文章内容通过freemarker生成html文件
                out = new StringWriter();
                template = configuration.getTemplate("article.ftl");

                Map<String, Object> params = new HashMap<>();
                params.put("content", JSONArray.parseArray(content));

                template.process(params, out);
            } catch (Exception e) {
                e.printStackTrace();
            }

            InputStream is = new ByteArrayInputStream(out.toString().getBytes());

            //3.把html文件上传到minio中
            String path = fileStorageService.uploadHtmlFile("", aparticle.getId() + ".html", is);

            //4.修改ap_article表，保存static_url字段
            ApArticle article = new ApArticle();
            article.setId(aparticle.getId());
            article.setStaticUrl(path);

            apArticleMapper.updateById(article);

            ApArticle apArticle = apArticleMapper.selectById(article.getId());

            // 发送消息 创建索引
            createArticleEsIndex(apArticle, content, path);
        }
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息 创建索引
     * @param article
     * @param content
     * @param path
     */
    private void createArticleEsIndex(ApArticle article, String content, String path) {
        SearchArticleVo searchArticleVo = new SearchArticleVo();
        try {
            BeanUtils.copyProperties(article, searchArticleVo);
            searchArticleVo.setContent(content);
            searchArticleVo.setStaticUrl(path);
        } catch (Exception e) {
            e.printStackTrace();
        }

        kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSON.toJSONString(searchArticleVo));
    }
}
