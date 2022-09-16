package com.srg.hydra.rost;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadJson {
    public static void main(String[] args) throws IOException {
        /*InputStream with = ReadJson.class.getResourceAsStream("/preDownload/withNewHouseId.json");
        InputStream without = ReadJson.class.getResourceAsStream("/preDownload/withNewHouseId.json");

        JsonParser jsonParserWith = new JsonFactory().createParser(with);
        jsonParserWith.setCodec(new ObjectMapper());
        JsonNode rootWith = jsonParserWith.readValueAsTree();
        System.out.printf("with = %d\n", rootWith.get("data").size());
        jsonParserWith.close();

        JsonParser jsonParserWithout = new JsonFactory().createParser(without);
        jsonParserWithout.setCodec(new ObjectMapper());
        JsonNode rootWithout = jsonParserWithout.readValueAsTree();
        System.out.printf("without = %d\n", rootWithout.get("data").size());
        jsonParserWithout.close();*/

        JsonParser flatsParser = new JsonFactory().createParser(ReadJson.class.getResourceAsStream("/preDownload/firstNbFlatsId.json"));
        while (flatsParser.nextToken() != null) {
            if ("object_id".equals(flatsParser.getCurrentName())) {
                System.out.println(flatsParser.nextLongValue(-1));
            }
        }
    }
}
