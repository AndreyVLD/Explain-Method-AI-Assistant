package org.example.demo;

import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;


public class ChatGPT {

    private static final Logger LOG = Logger.getInstance(ChatGPT.class);

    public static String infer(String prompt){
        String apiKey = readJsonKey();
        String url = "https://api.openai.com/v1/chat/completions";

        try{
            // Create connection
            URL obj = new URI(url).toURL();
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + apiKey);

            // Generate JSON body
            JSONObject data = getJsonBody(prompt);

            con.setDoOutput(true);
            con.getOutputStream().write(data.toString().getBytes());

            // Get response from OpenAI
            String response = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                    .reduce((a,b)->a+b).get();

            JSONObject json = new JSONObject(response);
            JSONObject responseObject = json.getJSONArray("choices").getJSONObject(0);
            return responseObject.getJSONObject("message").getString("content");

        } catch (Exception e) {
            LOG.warn("An error occurred during with chatGPT: ",e);
            return "Error";
        }
    }
    private static JSONObject getJsonBody(String prompt)
    {
        String model = "gpt-3.5-turbo";
        JSONObject data = new JSONObject();
        data.put("model", model);

        // Provide conversation history as messages array
        JSONArray messagesArray = new JSONArray();

        // System message
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "I explain Python Methods in Plain English.");
        messagesArray.put(systemMessage);

        // User message
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messagesArray.put(userMessage);

        // Add messages array to data object
        data.put("messages", messagesArray);

        return data;
    }

    private static String readJsonKey(){

        ClassLoader classLoader = ChatGPT.class.getClassLoader();
        String jsonContent;
        try (InputStream inputStream = classLoader.getResourceAsStream("META-INF/config.json")) {
            if(inputStream == null){
                throw new IOException("Could not find config.json");
            }
            jsonContent = new String(inputStream.readAllBytes());
        } catch (IOException e) {
            LOG.warn("An error occurred during reading config.json: ",e);
            return "Error";
        }

        JSONObject json = new JSONObject(jsonContent);
        return json.getString("api_key");

    }
}
