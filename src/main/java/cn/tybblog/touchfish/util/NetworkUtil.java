package cn.tybblog.touchfish.util;

import cn.tybblog.touchfish.PersistentState;
import cn.tybblog.touchfish.entity.Book;
import cn.tybblog.touchfish.entity.Chapter;
import cn.tybblog.touchfish.exception.FishException;
import cn.tybblog.touchfish.listener.EventListener;
import com.intellij.openapi.ui.MessageDialogBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkUtil {
    private static OkHttpClient client = new OkHttpClient();
    private static PersistentState persistentState = PersistentState.getInstance();

    /**
     * 搜索书
     * @param keyword 搜索关键词
     * @return html
     */
    public static List<Book> SearchBook(String keyword){
        //url拼接
        String url=persistentState.getUrl()+"/modules/article/search.php";
        List<Book> books = new ArrayList<>();
        try {
            String param = "searchtype=keywords&searchkey=" + URLEncoder.encode(keyword,"UTF-8") + "&action=login";
            Document doc = Jsoup.parse(postRequest(url,param));
            Elements trs = doc.select(".detail");
            if (trs == null||trs.size()==0) {
                return books;
            }
            String bookName = doc.select(".header").get(0).select(".title").text();
            String author = doc.select(".author").get(0).text();
            String href = doc.select(".button").get(0).selectFirst("a").attr("href");
            books.add(new Book(persistentState.getUrl() + href, bookName, author));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return books;
    }

    /**
     * 获取章节
     * @param url 书本地址
     * @return html
     */
    public static List<Chapter> getChapter(String url){
        Document doc = Jsoup.parse(sendRequest(url));
        Elements elements = doc.select("p");
        List<Chapter> chapters = new ArrayList<>();
        for (Element element : elements) {
            Element a = element.select("a").first();
            if(null == a){
                continue;
            }
            Chapter chapter = new Chapter(url.substring(0,url.lastIndexOf("/") + 1) + a.attr("href"),a.text());
            chapters.add(chapter);
        }
        return chapters;
    }

    /**
     * 获取书本内容
     * @param url 地址
     * @param callback 回调类
     */
    public static void getBookText(String url,ChapterCallback callback,String baseMethod){
        EventListener.loading=true;
        sendRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (e instanceof SocketTimeoutException) {
                    ConsoleUtils.info("加载超时");
                }
                if (e instanceof ConnectException || e instanceof UnknownHostException) {
                    ConsoleUtils.info("网络连接失败或域名错误！");
                }
                EventListener.loading=false;
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Document document = Jsoup.parse(response.body().string());
                String html = document.select("#nr1").html();
                String[] bookText = html.replaceAll("&nbsp;", "").split("\n<br>\n<br>");
                try {
                    callback.chapter(Arrays.asList(bookText),baseMethod);
                } catch (FishException e) {
                    ConsoleUtils.info(e.getMessage());
                    EventListener.loading=false;
                }
            }
        });
    }


    /**
     * 发送异步请求
     * @param url
     * @param callback 回调函数
     */
    public static void sendRequest(String url, Callback callback){
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    /**
     * 发送同步请求
     * @param url
     */
    public static String sendRequest(String url){
        Request request = new Request.Builder().url(url).build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                MessageDialogBuilder.yesNo("提示", "加载超时！").show();
            }
            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                MessageDialogBuilder.yesNo("提示", "网络连接失败！").show();
            }
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * 发送同步请求
     * @param url
     */
    public static String postRequest(String url,String param){
        Request request = new Request.Builder().url(url)
            .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),param)).build();
        try {
            Response response = client.newCall(request).execute();
            if(302 == response.code()){
                String location = response.header("Location");
                return postRequest(location,"");
            }
            return response.body().string();
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                MessageDialogBuilder.yesNo("提示", "加载超时！").show();
            }
            if (e instanceof ConnectException || e instanceof UnknownHostException) {
                MessageDialogBuilder.yesNo("提示", "网络连接失败！").show();
            }
            e.printStackTrace();
            return e.getMessage();
        }
    }
}