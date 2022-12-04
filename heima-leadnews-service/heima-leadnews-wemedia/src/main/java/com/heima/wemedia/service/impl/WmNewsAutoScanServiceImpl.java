package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     */
    @Override
    @Async
    public void autoScanWmNews(Integer id) throws Exception {
        // 查询文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl-文章不存在");
        }

        // 审核文本内容
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            // 待审核
            Map<String, Object> textAndImages = handTextAndImages(wmNews);
            boolean isTextScan = handleTextScan((String) textAndImages.get("content"), wmNews);
            if (!isTextScan) return;

            // 审核图片
        }
        // 保存app端相关得文章数据
        ResponseResult responseResult = saveAppArticle(wmNews);

        if (!responseResult.getCode().equals(200)) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核-保存app端相关文章数据失败");
        }

        // 回填articleId
        wmNews.setArticleId((Long) responseResult.getData());
        updateWmNews(wmNews, (short) 9, "审核成功");
    }

    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    private ResponseResult saveAppArticle(WmNews wmNews) throws Exception {
        ArticleDto dto = new ArticleDto();

        // 属性得拷贝
        BeanUtils.copyProperties(wmNews, dto);
        // 文章得布局
        dto.setLayout(wmNews.getType());
        // 文章得布局
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            dto.setChannelName(wmChannel.getName());
        }

        // 文章的作者
        dto.setAuthorId(wmNews.getUserId().longValue());

        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null) {
            dto.setAuthorName(wmUser.getName());
        }

        if (wmNews.getArticleId() != null) {
            dto.setId(wmNews.getArticleId());
        }

        dto.setCreatedTime(new Date());

        return articleClient.saveArticle(dto);

    }

    @Autowired
    private GreenTextScan greenTextScan;

    private boolean handleTextScan(String content, WmNews wmNews) {

        return true;
    }

    private Map<String, Object> handTextAndImages(WmNews wmNews) {

        StringBuilder stringBuilder = new StringBuilder();

        List<String> images = new ArrayList<>();

        if (StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if (map.get("type").equals("text")) {
                    stringBuilder.append(map.get("value"));
                }

                if (map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                }
            }
        }
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", stringBuilder.toString());
        resultMap.put("images", images);
        return resultMap;
    }
}
