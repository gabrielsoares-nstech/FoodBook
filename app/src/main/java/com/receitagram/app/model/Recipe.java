package com.receitagram.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    public int id;
    public int authorId;
    public String authorName;
    public String title;
    public String description;
    public List<String> ingredients = new ArrayList<>();
    public List<String> tools = new ArrayList<>();
    public List<String> steps = new ArrayList<>();
    public int likes;
    public boolean likedByCurrentUser;
    public boolean savedByCurrentUser;

    public static Recipe fromJson(JSONObject json) {
        Recipe recipe = new Recipe();
        recipe.id = json.optInt("id");
        recipe.authorId = json.optInt("authorId");
        recipe.authorName = json.optString("authorName");
        recipe.title = json.optString("title");
        recipe.description = json.optString("description");
        recipe.ingredients = toList(json.optJSONArray("ingredients"));
        recipe.tools = toList(json.optJSONArray("tools"));
        recipe.steps = toList(json.optJSONArray("steps"));
        recipe.likes = json.optInt("likes");
        recipe.likedByCurrentUser = json.optBoolean("likedByCurrentUser");
        recipe.savedByCurrentUser = json.optBoolean("savedByCurrentUser");
        return recipe;
    }

    private static List<String> toList(JSONArray array) {
        List<String> result = new ArrayList<>();
        if (array == null) return result;
        for (int i = 0; i < array.length(); i++) {
            String item = array.optString(i).trim();
            if (!item.isEmpty()) result.add(item);
        }
        return result;
    }
}
