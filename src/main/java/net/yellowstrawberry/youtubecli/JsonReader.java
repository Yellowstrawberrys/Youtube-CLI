package net.yellowstrawberry.youtubecli;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class JsonReader {
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
    /**
     * ReadFromUrl
     * @throws IOException
     * @throws JSONException
     * **/
    public static JSONObject readFromUrl(String url) throws IOException, JSONException {
        URLConnection con = new URL(url).openConnection();
        con.setRequestProperty("User-Agent", RandomUserAgent.getRandomUserAgent());
        InputStream is = con.getInputStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }
}
