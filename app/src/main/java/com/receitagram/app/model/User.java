package com.receitagram.app.model;

import org.json.JSONObject;

public class User {
    public final int id;
    public final String nome;
    public final String email;

    public User(int id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

    public static User fromJson(JSONObject json) {
        return new User(
                json.optInt("id"),
                json.optString("nome"),
                json.optString("email")
        );
    }
}
