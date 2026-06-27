package com.receitagram.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class RecipeApiServer {
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger(1);
    private static final AtomicInteger RECIPE_SEQUENCE = new AtomicInteger(1);
    private static final Map<Integer, User> USERS = new LinkedHashMap<>();
    private static final Map<String, User> USERS_BY_EMAIL = new HashMap<>();
    private static final List<Recipe> RECIPES = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        seed();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", RecipeApiServer::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Receitagram API rodando em http://localhost:8080");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            addCors(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                send(exchange, 204, "");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("/health".equals(path) && "GET".equals(method)) {
                sendJson(exchange, 200, "{\"status\":\"ok\"}");
            } else if ("/auth/register".equals(path) && "POST".equals(method)) {
                register(exchange);
            } else if ("/auth/login".equals(path) && "POST".equals(method)) {
                login(exchange);
            } else if ("/recipes".equals(path) && "GET".equals(method)) {
                listRecipes(exchange);
            } else if ("/recipes".equals(path) && "POST".equals(method)) {
                createRecipe(exchange);
            } else if (path.startsWith("/recipes/") && "POST".equals(method)) {
                toggleRecipe(exchange, path);
            } else {
                sendJson(exchange, 404, error("Rota nao encontrada."));
            }
        } catch (Exception e) {
            sendJson(exchange, 500, error("Erro interno: " + e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private static void register(HttpExchange exchange) throws IOException {
        Map<String, Object> body = parseJsonObject(readBody(exchange));
        String nome = text(body, "nome");
        String email = text(body, "email").toLowerCase(Locale.ROOT);
        String senha = text(body, "senha");
        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            sendJson(exchange, 400, error("Nome, email e senha sao obrigatorios."));
            return;
        }
        if (USERS_BY_EMAIL.containsKey(email)) {
            sendJson(exchange, 409, error("Ja existe uma conta com este email."));
            return;
        }
        User user = new User(USER_SEQUENCE.getAndIncrement(), nome, email, senha);
        USERS.put(user.id, user);
        USERS_BY_EMAIL.put(email, user);
        sendJson(exchange, 201, "{\"user\":" + userJson(user) + "}");
    }

    private static void login(HttpExchange exchange) throws IOException {
        Map<String, Object> body = parseJsonObject(readBody(exchange));
        String email = text(body, "email").toLowerCase(Locale.ROOT);
        String senha = text(body, "senha");
        User user = USERS_BY_EMAIL.get(email);
        if (user == null || !user.password.equals(senha)) {
            sendJson(exchange, 401, error("Email ou senha invalidos."));
            return;
        }
        sendJson(exchange, 200, "{\"user\":" + userJson(user) + "}");
    }

    private static void listRecipes(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        int userId = intValue(query.get("userId"));
        String mode = clean(query.get("mode"));
        String search = clean(query.get("query")).toLowerCase(Locale.ROOT);
        String ingredient = clean(query.get("ingredient")).toLowerCase(Locale.ROOT);
        String tool = clean(query.get("tool")).toLowerCase(Locale.ROOT);
        StringBuilder json = new StringBuilder("{\"recipes\":[");
        boolean first = true;
        for (Recipe recipe : RECIPES) {
            if ("saved".equals(mode) && !recipe.savedBy.contains(userId)) continue;
            if ("mine".equals(mode) && recipe.authorId != userId) continue;
            if (!search.isEmpty() && !contains(recipe.title, search) && !contains(recipe.description, search)) continue;
            if (!ingredient.isEmpty() && !containsAny(recipe.ingredients, ingredient)) continue;
            if (!tool.isEmpty() && !containsAny(recipe.tools, tool)) continue;
            if (!first) json.append(',');
            json.append(recipeJson(recipe, userId));
            first = false;
        }
        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private static void createRecipe(HttpExchange exchange) throws IOException {
        int userId = currentUserId(exchange);
        User user = USERS.get(userId);
        if (user == null) {
            sendJson(exchange, 401, error("Usuario nao autenticado."));
            return;
        }
        Map<String, Object> body = parseJsonObject(readBody(exchange));
        String title = text(body, "title");
        List<String> ingredients = stringList(body.get("ingredients"));
        List<String> steps = stringList(body.get("steps"));
        if (title.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            sendJson(exchange, 400, error("Titulo, ingredientes e passos sao obrigatorios."));
            return;
        }
        Recipe recipe = new Recipe();
        recipe.id = RECIPE_SEQUENCE.getAndIncrement();
        recipe.authorId = user.id;
        recipe.authorName = user.name;
        recipe.title = title;
        recipe.description = text(body, "description");
        recipe.ingredients = ingredients;
        recipe.tools = stringList(body.get("tools"));
        recipe.steps = steps;
        RECIPES.add(0, recipe);
        sendJson(exchange, 201, "{\"recipe\":" + recipeJson(recipe, userId) + "}");
    }

    private static void toggleRecipe(HttpExchange exchange, String path) throws IOException {
        int userId = currentUserId(exchange);
        if (!USERS.containsKey(userId)) {
            sendJson(exchange, 401, error("Usuario nao autenticado."));
            return;
        }
        String[] parts = path.split("/");
        if (parts.length != 4) {
            sendJson(exchange, 404, error("Rota de receita invalida."));
            return;
        }
        int recipeId = intValue(parts[2]);
        Recipe recipe = findRecipe(recipeId);
        if (recipe == null) {
            sendJson(exchange, 404, error("Receita nao encontrada."));
            return;
        }
        String action = parts[3];
        if ("like".equals(action)) toggle(recipe.likedBy, userId);
        else if ("save".equals(action)) toggle(recipe.savedBy, userId);
        else {
            sendJson(exchange, 404, error("Acao invalida."));
            return;
        }
        sendJson(exchange, 200, "{\"recipe\":" + recipeJson(recipe, userId) + "}");
    }

    private static void seed() {
        if (!USERS.isEmpty()) return;
        User ana = new User(USER_SEQUENCE.getAndIncrement(), "Ana Souza", "ana@demo.com", "123456");
        User bruno = new User(USER_SEQUENCE.getAndIncrement(), "Bruno Lima", "bruno@demo.com", "123456");
        USERS.put(ana.id, ana);
        USERS.put(bruno.id, bruno);
        USERS_BY_EMAIL.put(ana.email, ana);
        USERS_BY_EMAIL.put(bruno.email, bruno);
        addRecipe(ana, "Bolo de cenoura com cobertura", "Classico fofinho para o cafe da tarde.",
                Arrays.asList("3 cenouras medias", "3 ovos", "2 xicaras de farinha", "1 xicara de acucar", "1 colher de fermento"),
                Arrays.asList("Liquidificador", "Forma", "Forno"),
                Arrays.asList("Bata cenoura, ovos e oleo no liquidificador.", "Misture com farinha, acucar e fermento.", "Asse por cerca de 40 minutos.", "Finalize com cobertura de chocolate."));
        addRecipe(bruno, "Moqueca rapida de banana-da-terra", "Versao vegetariana, colorida e aromatica.",
                Arrays.asList("Banana-da-terra", "Leite de coco", "Pimentao", "Tomate", "Coentro"),
                Arrays.asList("Panela larga", "Faca"),
                Arrays.asList("Refogue tomate e pimentao.", "Adicione banana em rodelas e leite de coco.", "Cozinhe ate ficar macio.", "Finalize com coentro."));
    }

    private static void addRecipe(User author, String title, String description, List<String> ingredients, List<String> tools, List<String> steps) {
        Recipe recipe = new Recipe();
        recipe.id = RECIPE_SEQUENCE.getAndIncrement();
        recipe.authorId = author.id;
        recipe.authorName = author.name;
        recipe.title = title;
        recipe.description = description;
        recipe.ingredients = new ArrayList<>(ingredients);
        recipe.tools = new ArrayList<>(tools);
        recipe.steps = new ArrayList<>(steps);
        RECIPES.add(recipe);
    }

    private static Recipe findRecipe(int id) {
        for (Recipe recipe : RECIPES) if (recipe.id == id) return recipe;
        return null;
    }

    private static void toggle(Set<Integer> set, int value) {
        if (set.contains(value)) set.remove(value); else set.add(value);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean containsAny(List<String> values, String needle) {
        for (String value : values) if (contains(value, needle)) return true;
        return false;
    }

    private static int currentUserId(HttpExchange exchange) {
        return intValue(exchange.getRequestHeaders().getFirst("X-User-Id"));
    }

    private static int intValue(String value) {
        try { return Integer.parseInt(clean(value)); } catch (Exception e) { return 0; }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (!(value instanceof List)) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Object item : (List<Object>) value) {
            String clean = String.valueOf(item).trim();
            if (!clean.isEmpty()) result.add(clean);
        }
        return result;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            byte[] bytes = input.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,X-User-Id");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        send(exchange, status, json);
    }

    private static void send(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String error(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    private static String userJson(User user) {
        return "{\"id\":" + user.id + ",\"nome\":\"" + escape(user.name) + "\",\"email\":\"" + escape(user.email) + "\"}";
    }

    private static String recipeJson(Recipe recipe, int currentUserId) {
        return "{"
                + "\"id\":" + recipe.id + ","
                + "\"authorId\":" + recipe.authorId + ","
                + "\"authorName\":\"" + escape(recipe.authorName) + "\","
                + "\"title\":\"" + escape(recipe.title) + "\","
                + "\"description\":\"" + escape(recipe.description) + "\","
                + "\"ingredients\":" + arrayJson(recipe.ingredients) + ","
                + "\"tools\":" + arrayJson(recipe.tools) + ","
                + "\"steps\":" + arrayJson(recipe.steps) + ","
                + "\"likes\":" + recipe.likedBy.size() + ","
                + "\"likedByCurrentUser\":" + recipe.likedBy.contains(currentUserId) + ","
                + "\"savedByCurrentUser\":" + recipe.savedBy.contains(currentUserId)
                + "}";
    }

    private static String arrayJson(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(',');
            json.append('"').append(escape(values.get(i))).append('"');
        }
        return json.append(']').toString();
    }

    private static String escape(String value) {
        return clean(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }

    private static Map<String, String> parseQuery(String raw) throws IOException {
        Map<String, String> result = new HashMap<>();
        if (raw == null || raw.isEmpty()) return result;
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], "UTF-8");
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], "UTF-8") : "";
            result.put(key, value);
        }
        return result;
    }

    private static Map<String, Object> parseJsonObject(String json) {
        JsonCursor cursor = new JsonCursor(clean(json));
        return cursor.object();
    }

    private static class User {
        final int id;
        final String name;
        final String email;
        final String password;

        User(int id, String name, String email, String password) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }

    private static class Recipe {
        int id;
        int authorId;
        String authorName = "";
        String title = "";
        String description = "";
        List<String> ingredients = new ArrayList<>();
        List<String> tools = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        Set<Integer> likedBy = new LinkedHashSet<>();
        Set<Integer> savedBy = new LinkedHashSet<>();
    }

    private static class JsonCursor {
        private final String json;
        private int index;

        JsonCursor(String json) {
            this.json = json;
        }

        Map<String, Object> object() {
            Map<String, Object> result = new HashMap<>();
            skipWhitespace();
            if (!consume('{')) return result;
            while (index < json.length()) {
                skipWhitespace();
                if (consume('}')) break;
                String key = string();
                skipWhitespace();
                consume(':');
                Object value = value();
                result.put(key, value);
                skipWhitespace();
                if (consume('}')) break;
                consume(',');
            }
            return result;
        }

        private Object value() {
            skipWhitespace();
            if (peek() == '"') return string();
            if (peek() == '[') return array();
            StringBuilder builder = new StringBuilder();
            while (index < json.length() && ",}".indexOf(json.charAt(index)) == -1) builder.append(json.charAt(index++));
            return builder.toString().trim();
        }

        private List<String> array() {
            List<String> result = new ArrayList<>();
            consume('[');
            while (index < json.length()) {
                skipWhitespace();
                if (consume(']')) break;
                result.add(String.valueOf(value()));
                skipWhitespace();
                if (consume(']')) break;
                consume(',');
            }
            return result;
        }

        private String string() {
            StringBuilder builder = new StringBuilder();
            consume('"');
            while (index < json.length()) {
                char current = json.charAt(index++);
                if (current == '"') break;
                if (current == '\\' && index < json.length()) {
                    char escaped = json.charAt(index++);
                    if (escaped == 'n') builder.append('\n');
                    else builder.append(escaped);
                } else {
                    builder.append(current);
                }
            }
            return builder.toString();
        }

        private boolean consume(char expected) {
            skipWhitespace();
            if (index < json.length() && json.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            return index < json.length() ? json.charAt(index) : ' ';
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) index++;
        }
    }
}
