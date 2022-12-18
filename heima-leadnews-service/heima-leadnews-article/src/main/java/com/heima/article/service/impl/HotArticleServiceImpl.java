package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class HotArticleServiceImpl implements HotArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 计算热点文章
     */
    @Override
    public void computeHotArticle() {
        // 1.查询前五天的文章数据
        Date date = DateTime.now().minusDays(5).toDate();
        List<ApArticle> articles = apArticleMapper.findArticleListByLast5days(date);
        // 2.计算文章的分值
        List<HotArticleVo> hotArticleVos = computeHotArticle(articles);
        // 3. 为每个频道缓存30条分值较高的文章
        cacheTagToRedis(hotArticleVos);
    }

    @Autowired
    private IWemediaClient iWmediaClient;

    @Autowired
    private CacheService cacheService;

    /**
     * 为每个频道缓存30条文章
     * @param hotArticleVos
     */
    private void cacheTagToRedis(List<HotArticleVo> hotArticleVos) {
        // 为每个频道缓存30条分值高的数据
        ResponseResult responseResult = iWmediaClient.getChannels();
        if (responseResult.getCode().equals(200)) {
            Object data = responseResult.getData();
            String jsonString = JSON.toJSONString(data);
            List<WmChannel> wmChannels = JSON.parseArray(jsonString, WmChannel.class);
            // 检索出每个频道的文章
            if (wmChannels != null && wmChannels.size() > 0) {
                for (WmChannel wmChannel : wmChannels) {
                    // 该频道的所有数据
                    List<HotArticleVo> hotArticleVoList = hotArticleVos.stream().filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
                    // 给文章排序  取30条分值高的文章 存入redis， key：频道id value：30条文章
                    sortAndCache(hotArticleVoList, Comparator.comparing(HotArticleVo::getScore).reversed(), ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
                }
            }
        }
        // 设置推荐数据
        sortAndCache(hotArticleVos, Comparator.comparing(HotArticleVo::getScore), ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);
    }

    private void sortAndCache(List<HotArticleVo> hotArticleVoList, Comparator<HotArticleVo> reversed, String HOT_ARTICLE_COLLECTION_WEIGHT) {
        List<HotArticleVo> latestHotArticleVos = hotArticleVoList.stream().sorted(reversed).limit(30).collect(Collectors.toList());
        cacheService.set(HOT_ARTICLE_COLLECTION_WEIGHT, JSON.toJSONString(latestHotArticleVos));
    }

    /**
     * 计算文章的分值
     * @param articles
     * @return
     */
    private List<HotArticleVo> computeHotArticle(List<ApArticle> articles) {
        List<HotArticleVo> list = new ArrayList<>();
        if (articles != null && articles.size() > 0) {
            for (ApArticle article : articles) {
                HotArticleVo hot = new HotArticleVo();
                BeanUtils.copyProperties(article, hot);
                Integer score = computeScore(article);
                hot.setScore(score);
                list.add(hot);
            }
        }
        return list;
    }

    /**
     * 计算文章的具体分值
     * @param article
     * @return
     */
    private Integer computeScore(ApArticle article) {
        Integer score = 0;
        if (article.getLikes() != null) {
            score += article.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if (article.getViews() != null) {
            score += article.getViews();
        }
        if (article.getComment() != null) {
            score += article.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if (article.getCollection() != null) {
            score += article.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }

        return score;
    }

}
