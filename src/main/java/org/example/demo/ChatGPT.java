package org.example.demo;

import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ChatGPT {
    private static final Logger LOG = Logger.getInstance(ChatGPT.class);
    private static final String apiKey = readJsonKey();
    private static final String url = "https://api.openai.com/v1/chat/completions";

    /**
     * This method sends the prompt to ChatGPT and returns the response
     * @param prompt The prompt that the user provides to ChatGPT
     * @return The response from ChatGPT as a String or "Error" if an error occurred
     * @see <a href="https://beta.openai.com/docs/api-reference/chat">ChatGPT API Reference</a>
     */
    public static String infer(String prompt){
        try{
            // Create connection
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // Set headers
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + apiKey);

            // Generate JSON body
            JSONObject data = getJsonBody(prompt);

            con.setDoOutput(true);
            con.getOutputStream().write(data.toString().getBytes());

            // Get response from OpenAI
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                reader.lines().forEach(responseBuilder::append);
            }

            String response = responseBuilder.toString();

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

    /**
     * This method creates the JSON body that is sent to ChatGPT
     * @param prompt The prompt that the user provides to ChatGPT
     * @return The JSON body as a JSONObject
     */
    private static JSONObject getJsonBody(String prompt)
    {
        // Create the JSON body
        String model = "gpt-4";
        JSONObject data = new JSONObject();
        data.put("model", model);
        data.put("max_tokens", 200);
        data.put("temperature", 0.5);
        data.put("n", 1);

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

    /**
     * This method reads the API key from config.json.
     * If config.json is not found, it returns the key from the top of this file
     * @return The API key as a String
     */
    private static String readJsonKey(){

        ClassLoader classLoader = ChatGPT.class.getClassLoader();
        String jsonContent;

        // Read config.json to get the API key
        try (InputStream inputStream = classLoader.getResourceAsStream("META-INF/config.json")) {
            if(inputStream == null){

                // If config.json is not found, return the key defined by the user in user_data.json
                LOG.warn("Could not find config.json in resources folder");

                // Try reading the user_data key
                try (InputStream inputStream2 = classLoader.getResourceAsStream("META-INF/user_data.json")) {
                    if(inputStream2 == null){
                        LOG.warn("Could not find user_data.json in resources folder");
                        return "Error";
                    }else
                        jsonContent = new String(inputStream2.readAllBytes());
                } catch (IOException e) {
                    LOG.warn("An error occurred during reading user_data.json: ",e);
                    return "Error";
                }
            }else
                jsonContent = new String(inputStream.readAllBytes());
        } catch (IOException e) {
            LOG.warn("An error occurred during reading config.json: ",e);
            return "Error";
        }

        JSONObject json = new JSONObject(jsonContent);
        return json.getString("api_key");
    }

}
