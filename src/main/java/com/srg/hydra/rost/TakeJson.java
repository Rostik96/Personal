package com.srg.hydra.rost;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TakeJson {
    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.parse(TakeJson.class.getResourceAsStream("/ZilYugShagal.html"), "utf-8", "");
        Element element = doc.selectFirst("div.guXNU > a");
        String href = element.attr("href").replace("#flats", "");
        System.out.println(href);
    }
}
