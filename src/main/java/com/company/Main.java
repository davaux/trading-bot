package com.company;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        //getFromCryptoCompare();
        //getFromBitfinex();
        new Manager().runBot();

//        new ManagerOpt().runBot();
    }

    private static void getFromBitfinex() throws URISyntaxException, IOException, InterruptedException {
        //LocalDateTime date = LocalDateTime.now().minusDays(7);
        //Timestamp t = Timestamp.valueOf(date);
        long startDate = 1483228800 * 1000;
        String pair = "BTCUSD";
        String timeFrame = "1h";

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(BotCandle.class, new BotCandleDeserializer())
                .create();
        Set<BotCandle> olhcData = new HashSet<>();
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");


        for(int i = 0; i < 100; i++) {
            System.out.println("Start date " + startDate);
            URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("api.bitfinex.com")
                    .setPath("/v2/candles/trade:" + timeFrame + ":t" + pair + "/hist")
                    .setParameter("sort", String.valueOf(1))
                    .setParameter("limit", String.valueOf(1000))
                    .setParameter("start", String.valueOf(startDate))
                    .build();
            HttpGet httpget = new HttpGet(uri);
            ResponseHandler<JSONArray> rh = response -> {
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                if (statusLine.getStatusCode() >= 300) {
                    throw new HttpResponseException(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase());
                }
                if (entity == null) {
                    throw new ClientProtocolException("Response contains no content");
                }

                ContentType contentType = ContentType.getOrDefault(entity);
                Charset charset = contentType.getCharset();
                String collect = new BufferedReader(new InputStreamReader(entity.getContent(), charset))
                        .lines().collect(Collectors.joining("\n"));
                System.out.println(collect);
                JSONArray jsonObject = new JSONArray(collect);
                return jsonObject;
            };

            JSONArray execute;
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                System.out.println("Executing request " + httpget.getRequestLine());
                execute = httpClient.execute(httpget, rh);
            }

            Collection<BotCandle> botCandles = new ArrayList<>();
            for(int x = 0; x < execute.length(); x++) {
                BotCandle e = gson.fromJson(execute.get(x).toString(), BotCandle.class);
                botCandles.add(e);
                if(x == execute.length() - 1) {
                    startDate = e.getTime() + 60 * 60 * 1000;
                }
            }
            olhcData.addAll(botCandles);
            //startDate = botCandles.toArray(new BotCandle[botCandles.size()])[botCandles.size() - 1].getTime() + 60 * 5 * 1000;
            System.out.println(df.format(new Date(startDate)) + " " + botCandles.size());
            Thread.sleep(5000);
        }
        System.out.println(olhcData.size());
        try (Writer writer = new FileWriter("BFX_" + pair + "_" + timeFrame + "_Output.json")) {
            //Gson gson = new GsonBuilder().create();
            gson.toJson(olhcData, writer);
        }
    }

    private static void getFromCryptoCompare() throws URISyntaxException, IOException, InterruptedException {
        //LocalDateTime date = LocalDateTime.now().minusDays(7);
        LocalDateTime date = LocalDateTime.now().minusYears(1);
        //Timestamp t = Timestamp.valueOf(date);
        long startDate = date.toEpochSecond(ZoneOffset.UTC) + (2000 * 60 * 24);
        String fromSymbol = "BTC";
        String toSymbol = "USD";
        String timeFrame = "histohour";

        Gson gson = new Gson();
        Set<BotCandle> olhcData = new HashSet<>();

        for(int i = 0; i < 10; i++) {
            System.out.println("Start date " + startDate);
            URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("min-api.cryptocompare.com")
                    .setPath("/data/" + timeFrame)
                    .setParameter("fsym", fromSymbol)
                    .setParameter("tsym", toSymbol)
                    .setParameter("toTs", String.valueOf(startDate))
                    .setParameter("limit", "2000")
                    .build();
            HttpGet httpget = new HttpGet(uri);
            ResponseHandler<JSONObject> rh = response -> {
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                if (statusLine.getStatusCode() >= 300) {
                    throw new HttpResponseException(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase());
                }
                if (entity == null) {
                    throw new ClientProtocolException("Response contains no content");
                }

                ContentType contentType = ContentType.getOrDefault(entity);
                Charset charset = contentType.getCharset();
                String collect = new BufferedReader(new InputStreamReader(entity.getContent(), charset))
                        .lines().collect(Collectors.joining("\n"));
                //System.out.println(collect);
                JSONObject jsonObject = new JSONObject(collect);
                return jsonObject;
            };

            JSONObject execute;
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                System.out.println("Executing request " + httpget.getRequestLine());
                execute = httpClient.execute(httpget, rh);
            }

            Type collectionType = new TypeToken<Collection<BotCandle>>(){}.getType();
            Collection<BotCandle> botCandles = gson.fromJson(execute.getJSONArray("Data").toString(), collectionType);
            olhcData.addAll(botCandles);
            startDate = ((int)execute.get("TimeTo")) + (2000 * 60 * 24) + 60 * 24;
            System.out.println(olhcData.size());
            Thread.sleep(1000);
        }
    }

    private static class BotCandleDeserializer implements JsonDeserializer<BotCandle> {
        public BotCandle deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonArray jsonArray = json.getAsJsonArray();
            long time = jsonArray.get(0).getAsLong();
            double open = jsonArray.get(1).getAsDouble();
            double close = jsonArray.get(2).getAsDouble();
            double high = jsonArray.get(3).getAsDouble();
            double low = jsonArray.get(4).getAsDouble();
            double volume = jsonArray.get(5).getAsDouble();
            BotCandle botCandle = new BotCandle();
            botCandle.setTime(time);
            botCandle.setOpen(open);
            botCandle.setClose(close);
            botCandle.setHigh(high);
            botCandle.setLow(low);
            botCandle.setVolumeFrom(volume);
            botCandle.setVolumeTo(volume);
            return botCandle;
        }
    }
}


