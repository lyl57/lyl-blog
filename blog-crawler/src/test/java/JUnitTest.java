
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.CollectionUtils;
import service.dto.BaseResponse;
import service.utils.HttpClientUtil;

import java.util.HashMap;

/**
 * @author Created by lyl57 on 2017/12/26
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest(classes = BlogCralwerApplication.class)
//@WebAppConfiguration
public class JUnitTest {

    @Test
    public void likeName() {
        System.out.println(0);
    }

    @Test
    public void testCrawler() {
        try {
            String url = "http://192.168.31.49:4003/";
            BaseResponse baseResponse = HttpClientUtil.doGetRequest(url, new HashMap<>());
            Document document = Jsoup.parse(baseResponse.getResult());
            Elements li = document.getElementsByTag("li");
            StringBuffer sb = new StringBuffer();
            li.forEach(element -> {
                Elements uls = element.getElementsByTag("ul");
                if (element.attributes().getIgnoreCase("data-path").contains("环境") && !CollectionUtils.isEmpty(uls)) {
                    Elements liList = uls.get(0).getElementsByTag("li");
                    liList.forEach(element1 -> {
                        String href = element1.child(0).attributes().getIgnoreCase("href");
                        System.out.println(href);
                        sb.append(href).append("\n");
                        try {
                            BaseResponse baseResponse1 = HttpClientUtil.doGetRequest(url + href, new HashMap<>());
                            Document document1 = Jsoup.parse(baseResponse1.getResult());
                            Element table = document1.getElementsByTag("table").get(0);
                            Elements trList = table.getElementsByTag("tbody").get(0).getElementsByTag("tr");
                            trList.forEach(element2 -> {
                                Elements tdList = element2.getElementsByTag("td");
                                String format = String.format("服务：%s,ip:%s", tdList.get(5).html(), tdList.get(4).html());
                                System.out.println(format);
                                sb.append(format).append("\n");
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
            System.out.println(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}


