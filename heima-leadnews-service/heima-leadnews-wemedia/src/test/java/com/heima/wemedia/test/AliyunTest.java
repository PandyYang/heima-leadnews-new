package com.heima.wemedia.test;

import com.heima.common.aliyun.GreenTextScan;
import com.heima.wemedia.WemediaApplication;
import com.heima.wemedia.service.WmNewsAutoScanService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class AliyunTest {

    @Autowired
    private GreenTextScan greenTextScan;

    @Test
    public void testScanText() throws Exception {
        Map map = greenTextScan.greeTextScan("杀人放火，无恶不作。");
        System.out.println(map);

    }

    @Test
    public void testScanImage() {

    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Test
    public void testAutoScanWmNews() {
        wmNewsAutoScanService.autoScanWmNews(6232);
    }
}
