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


public class ChatGPT {

    private static final String userKey = "";
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

            // Parse response
            JSONObject json = new JSONObject(response);

            // Extract the first response from the choices given by ChatGPT
            JSONObject responseObject = json.getJSONArray("choices").getJSONObject(0);
            return responseObject.getJSONObject("message").getString("content");

        } catch (Exception e) {
            LOG.warn("An error occurred during with chatGPT: ",e);
            return "Error";
        }
    }
    private static JSONObject getJsonBody(String prompt)
    {
        // Create the JSON body
        String model = "gpt-3.5-turbo";
        JSONObject data = new JSONObject();
        data.put("model", model);
        data.put("max_tokens", 150);
        data.put("temperature", 0.5);

        // Provide conversation history as messages array
        JSONArray messagesArray = new JSONArray();

        // The role of the system is to explain the method
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "I explain Python Methods in Plain English.");
        messagesArray.put(systemMessage);

        // The user provides the prompt
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messagesArray.put(userMessage);

        // Add messages array to JSON body
        data.put("messages", messagesArray);

        return data;
    }

    private static String readJsonKey(){

        ClassLoader classLoader = ChatGPT.class.getClassLoader();
        String jsonContent;

        // Read config.json
        try (InputStream inputStream = classLoader.getResourceAsStream("META-INF/config.json")) {
            if(inputStream == null){

                // If config.json is not found, return the key from the top of this file
                LOG.warn("Could not find config.json");
                return userKey;
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
