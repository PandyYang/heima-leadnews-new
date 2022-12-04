package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;
import freemarker.template.TemplateException;

import java.io.IOException;

public interface ArticleFreemarkerService {

    /**
     * 生成静态文件 上传至minio
     * @param aparticle
     * @param content
     */
    public void buildArticleToMinIO(ApArticle aparticle, String content) throws IOException, TemplateException;
}
