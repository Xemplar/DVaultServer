package com.xemplarsoft.dv.medium.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xemplarsoft.dv.medium.server.DVServerHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class MarketHandler implements Runnable{
    private static final String API_KEY = "7b7eb533-b49f-46e4-a7d3-7bbd68c713e9";
    private static final String CMC = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest";
    private volatile boolean isRunning = false;
    private Thread t;
    private DVServerHandler handler;

    public MarketHandler(DVServerHandler handler){
        t = new Thread(this);
        this.handler = handler;
    }

    public MarketHandler(){
        t = new Thread(this);
    }

    public synchronized void start(){
        isRunning = true;
        t.start();

        System.out.println("INFO: MarketWatcher Starting");
    }

    public synchronized void stop(){
        isRunning = false;
    }

    public void run(){
        while(isRunning){
            try{
                BigDecimal usd = getPrice("USD");
                BigDecimal btc = getPrice("BTC");

                handler.setPrices(usd, btc);

                System.out.println("MarketWatcher: latest USD price is $" + usd.toPlainString());
                System.out.println("MarketWatcher: latest BTC price is \u20BF" + usd.toPlainString());

                Thread.sleep(1000 * 60 * 15);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        try{
            t.join();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public BigDecimal getPrice(String type){
        List<NameValuePair> params1 = new ArrayList<>();
        params1.add(new BasicNameValuePair("id","1769"));
        params1.add(new BasicNameValuePair("convert", type));

        BigDecimal d = new BigDecimal("0.0");
        try {
            String data = makeAPICall(CMC, params1);
            final ObjectNode n = new ObjectMapper().readValue(data, ObjectNode.class);
            d = new BigDecimal(n.get("data").get("1769").get("quote").get(type).get("price").toString());

        } catch (Exception e){
            e.printStackTrace();
        }
        d = d.setScale(8, BigDecimal.ROUND_DOWN);
        return d;
    }

    public static String makeAPICall(String uri, List<NameValuePair> parameters)
            throws URISyntaxException, IOException {
        String response_content = "";

        URIBuilder query = new URIBuilder(uri);
        query.addParameters(parameters);

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(query.build());

        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.addHeader("X-CMC_PRO_API_KEY", API_KEY);

        CloseableHttpResponse response = client.execute(request);

        try {
            HttpEntity entity = response.getEntity();
            response_content = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        } finally {
            response.close();
        }

        return response_content;
    }

//    public static void main(String[] args){
//        MarketHandler mh = new MarketHandler();
//        mh.start();
//    }
}
