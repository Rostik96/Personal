package com.srg.hydra.rost;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Hello world!
 */
public class App {
    private static final String FULL_URL_PATTERN = ".*\\|(.*\\.com)\\|(.*/)\\|.*";
    private static final String NB_COMLEXES_ID_API_REQUEST_URL = "https://msk.etagi.com/rest/etagi.nh_flats?protName=newhousesOnMap&fields&filter=[\"and\",[[\"=\",\"f.status\",\"active\"],[\"<>\",\"f.newhouses_id\",\"0\"],[\"in|=\",\"f.city_id\",[%d]],[\"or\",[[\"=\",\"n.active\",true],[\"null\",\"n.active\"]]],[\"or\",[[\"=\",\"n.active\",true],[\"null\",\"n.active\"]],null]]]&order=[]&orderId=default&limit=1000&as=f&join&group=[\"f.newcomplex_id\",\"n.name\"]&lang=ru&nocache=0&caseFilters={}&bAddLimit=0&bIsFunction=0";
    private static final String NB_COMLEX_FLATS_ID_API_REQUEST_URL = "https://msk.etagi.com/rest/etagi.nh_flats?protName=newHouseGpFlats&fields&filter=[\"and\",[[\"=\",\"newcomplex_id\",\"%1$d\"],[\"=\",\"status\",\"active\"]]]&order=[[\"floor\",\"ASC\"],[\"on_floor\",\"ASC\"]]&orderId=default&limit=2000&as=f&join&group=[\"f.id\",\"f.object_id\",\"f.newhouses_id\",\"on_floor\",\"rooms\",\"type\",\"studio\",\"price\",\"square\",\"square_kitchen\",\"status\",\"section\",\"floor\",\"floors\",\"price_m2\",\"keep\",\"ondeadline\",\"deadline_q\",\"deadline_y\",\"price_on_floor_diff\",\"dolshik\",\"active_contractor\",\"reservation.date_reservation\",\"newhouse_characteristics\"]&lang=ru&nocache=1&caseFilters={\"flats\":[\"and\",[[\"=\",\"newcomplex_id\",\"%1$d\"],[\"=\",\"status\",\"active\"]]]}&bAddLimit=0&bIsFunction=0&count=1";

    private static final String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.167 Safari/537.36";
    private static final String acceptJson = "application/json";
    private static final String contentTypeJson = "application/json; utf-8";

    private static final HttpClient httpClient = HttpClientBuilder.create().build();

    public App() throws URISyntaxException, IOException {
    }


    public static void main(String[] args) throws IOException, UnirestException, URISyntaxException {


        final String moscowNbIdsApiRequestURL = String.format(NB_COMLEXES_ID_API_REQUEST_URL, 155);

        UriComponents uriComponents1 = UriComponentsBuilder.fromHttpUrl(moscowNbIdsApiRequestURL).build();

        //(1)
        //GET-request за списком ID новостроек по Москве.
        HttpGet moscowNbIdsApiRequest = new HttpGet(uriComponents1.toUri());
        moscowNbIdsApiRequest.addHeader("User-Agent", userAgent);
        moscowNbIdsApiRequest.addHeader("Accept", acceptJson);
        moscowNbIdsApiRequest.addHeader("Content-Type", contentTypeJson);
        //pre download//
        //JsonParser jsonParser = new JsonFactory().createParser(App.class.getResourceAsStream("/preDownload/nbIDs.json"));

        //***EXECUTE***
        HttpResponse moscowNbIdsApiResponse = httpClient.execute(moscowNbIdsApiRequest);
        JsonParser jsonParser = new JsonFactory().createParser(moscowNbIdsApiResponse.getEntity().getContent());
        List<Integer> nbIds = new LinkedList<>();
        while (jsonParser.nextToken() != null) {
            if ("id".equals(jsonParser.getCurrentName())) {
                int id = jsonParser.nextIntValue(-1);
                nbIds.add(id);
            }
        }
        jsonParser.close();

        //(2)
        //GET-request за HTML страницей первой новостройки из списка.
        int firstNbId = nbIds.get(0);
        UriComponents firsNbShortUriComponents = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host("msk.etagi.com")
                .pathSegment("zastr")
                .pathSegment("jk")
                .pathSegment(String.valueOf(firstNbId))
                .build();
        URI uri = new URI(firsNbShortUriComponents.toUriString());
        HttpGet firstNbRequest = new HttpGet(uri);
        firstNbRequest.addHeader("User-Agent", userAgent);
        //pre download//
        //Document firstNbHtml = Jsoup.parse(App.class.getResourceAsStream("/ZilYugShagal.html"), "utf-8", "");

        //***EXECUTE***
        HttpResponse firstNbResponse = httpClient.execute(firstNbRequest);
        Document firstNbHtml = Jsoup.parse(firstNbResponse.getEntity().getContent(), StandardCharsets.UTF_8.name(), "");

        String varDataJson = firstNbHtml.select("script:containsData(var data)").first().data();
        varDataJson = varDataJson.substring(varDataJson.indexOf("{"));

        JsonParser nbJsonParser = new JsonFactory().createParser(varDataJson);
        nbJsonParser.setCodec(new ObjectMapper());
        String firstNbPageUrl = null;
        while (nbJsonParser.nextToken() != null) {
            if ("page_url".equals(nbJsonParser.currentName())) {
                firstNbPageUrl = nbJsonParser.nextTextValue();
                break;
            }
        }

        //(3)
        //GET-request за списком ID квартир
        URI url = UriComponentsBuilder.fromHttpUrl(String.format(NB_COMLEX_FLATS_ID_API_REQUEST_URL, firstNbId)).build().toUri();
        HttpGet flatsNbApiRequest = new HttpGet(url);
        flatsNbApiRequest.addHeader("User-Agent", userAgent);
        flatsNbApiRequest.addHeader("Accept", acceptJson);
        flatsNbApiRequest.addHeader("Content-Type", contentTypeJson);
        //pre download//
        //JsonParser flatsJsonParser = new JsonFactory().createParser(App.class.getResourceAsStream("/preDownload/firstNbFlatsId.json"));
        //String firstNbFlatsIdJson = IOUtils.toString(App.class.getResourceAsStream("/preDownload/firstNbFlatsId.json"), StandardCharsets.UTF_8);
        //String firstNbFlatsIdJson = IOUtils.toString(flatsIdResponse.getEntity().getContent(), StandardCharsets.UTF_8);

        //***EXECUTE***
        HttpResponse flatsIdResponse = httpClient.execute(flatsNbApiRequest);
        JsonParser flatsJsonParser = new JsonFactory().createParser(flatsIdResponse.getEntity().getContent());
        List<Long> nbFlatsIds = new LinkedList<>();
        while (flatsJsonParser.nextToken() != null) {
            if ("object_id".equals(flatsJsonParser.getCurrentName())) {
                nbFlatsIds.add(flatsJsonParser.nextLongValue(-1));
            }
        }
        flatsJsonParser.close();
        Long firstNbFirstFlatId = nbFlatsIds.get(0);


        System.out.println(
                UriComponentsBuilder.newInstance()
                        .scheme("https")
                        .host(firstNbPageUrl)
                        .path(String.valueOf(firstNbFirstFlatId))
                        .build()
        );
    }
}
