package com.receitagram.app.data;

import android.os.Handler;
import android.os.Looper;

import com.receitagram.app.model.Recipe;
import com.receitagram.app.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static final String BASE_URL = "http://10.0.2.2:8080";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void register(String nome, String email, String senha, Callback<User> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("nome", nome);
            body.put("email", email);
            body.put("senha", senha);
            postUser("/auth/register", body, callback);
        } catch (Exception e) {
            callback.onError("Nao foi possivel montar o cadastro.");
        }
    }

    public void login(String email, String senha, Callback<User> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("senha", senha);
            postUser("/auth/login", body, callback);
        } catch (Exception e) {
            callback.onError("Nao foi possivel montar o login.");
        }
    }

    public void listRecipes(int userId, String query, String ingredient, String tool, String mode, Callback<List<Recipe>> callback) {
        executor.execute(() -> {
            try {
                StringBuilder path = new StringBuilder("/recipes?userId=").append(userId);
                appendParam(path, "query", query);
                appendParam(path, "ingredient", ingredient);
                appendParam(path, "tool", tool);
                appendParam(path, "mode", mode);
                JSONObject response = request("GET", path.toString(), null, userId);
                JSONArray items = response.optJSONArray("recipes");
                List<Recipe> recipes = new ArrayList<>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) recipes.add(Recipe.fromJson(items.getJSONObject(i)));
                }
                success(callback, recipes);
            } catch (Exception e) {
                error(callback, "Nao foi possivel carregar as receitas. Verifique se a API esta rodando.");
            }
        });
    }

    public void myRecipes(int userId, Callback<List<Recipe>> callback) {
        listRecipes(userId, "", "", "", "mine", callback);
    }

    public void createRecipe(int userId, String title, String description, List<String> ingredients, List<String> tools, List<String> steps, Callback<Recipe> callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("title", title);
                body.put("description", description);
                body.put("ingredients", toArray(ingredients));
                body.put("tools", toArray(tools));
                body.put("steps", toArray(steps));
                JSONObject response = request("POST", "/recipes", body, userId);
                success(callback, Recipe.fromJson(response.getJSONObject("recipe")));
            } catch (Exception e) {
                error(callback, "Nao foi possivel publicar a receita.");
            }
        });
    }

    public void toggleLike(int userId, int recipeId, Callback<Recipe> callback) {
        toggle(userId, recipeId, "like", callback);
    }

    public void toggleSave(int userId, int recipeId, Callback<Recipe> callback) {
        toggle(userId, recipeId, "save", callback);
    }

    private void toggle(int userId, int recipeId, String action, Callback<Recipe> callback) {
        executor.execute(() -> {
            try {
                JSONObject response = request("POST", "/recipes/" + recipeId + "/" + action, new JSONObject(), userId);
                success(callback, Recipe.fromJson(response.getJSONObject("recipe")));
            } catch (Exception e) {
                error(callback, "Nao foi possivel atualizar a receita.");
            }
        });
    }

    private void postUser(String endpoint, JSONObject body, Callback<User> callback) {
        executor.execute(() -> {
            try {
                JSONObject response = request("POST", endpoint, body, 0);
                success(callback, User.fromJson(response.getJSONObject("user")));
            } catch (Exception e) {
                error(callback, "Email ou senha invalidos, ou API indisponivel.");
            }
        });
    }

    private JSONObject request(String method, String path, JSONObject body, int userId) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        if (userId > 0) connection.setRequestProperty("X-User-Id", String.valueOf(userId));
        if (body != null) {
            connection.setDoOutput(true);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String text = readAll(stream);
        if (status >= 400) throw new IllegalStateException(text);
        return new JSONObject(text);
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) builder.append(line);
        return builder.toString();
    }

    private static void appendParam(StringBuilder builder, String key, String value) throws Exception {
        if (value == null || value.trim().isEmpty()) return;
        builder.append('&').append(key).append('=').append(URLEncoder.encode(value.trim(), "UTF-8"));
    }

    private static JSONArray toArray(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            String clean = value.trim();
            if (!clean.isEmpty()) array.put(clean);
        }
        return array;
    }

    private <T> void success(Callback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private void error(Callback<?> callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
