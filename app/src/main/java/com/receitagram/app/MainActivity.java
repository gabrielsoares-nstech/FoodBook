package com.receitagram.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.receitagram.app.data.ApiClient;
import com.receitagram.app.model.Recipe;
import com.receitagram.app.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BRAND = Color.rgb(31, 122, 83);
    private static final int BRAND_DARK = Color.rgb(21, 84, 58);
    private static final int ACCENT = Color.rgb(224, 102, 47);
    private static final int BACKGROUND = Color.rgb(246, 243, 238);
    private static final int SURFACE = Color.WHITE;
    private static final int TEXT = Color.rgb(34, 34, 34);
    private static final int MUTED = Color.rgb(98, 98, 98);

    private final ApiClient api = new ApiClient();
    private LinearLayout content;
    private User currentUser;
    private String currentTab = "discover";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSession();
        if (currentUser == null) showAuth(false); else showDiscover();
    }

    private void showAuth(boolean registerMode) {
        currentTab = "auth";
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(22), dp(34), dp(22), dp(22));
        screen.setBackgroundColor(BACKGROUND);
        setContentView(screen);

        screen.addView(label("Receitagram", 30, BRAND_DARK, Typeface.BOLD));
        screen.addView(label("Compartilhe receitas, salve favoritas e descubra novas ideias para cozinhar.", 16, MUTED, Typeface.NORMAL));
        addSpacer(screen, 22);

        EditText nome = input("Nome");
        if (registerMode) screen.addView(nome);
        EditText email = input("Email");
        email.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText senha = input("Senha");
        senha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        screen.addView(email);
        screen.addView(senha);

        Button primary = primaryButton(registerMode ? "Criar conta" : "Entrar");
        screen.addView(primary);
        primary.setOnClickListener(view -> {
            String emailValue = email.getText().toString().trim();
            String senhaValue = senha.getText().toString().trim();
            if (emailValue.isEmpty() || senhaValue.isEmpty() || (registerMode && nome.getText().toString().trim().isEmpty())) {
                toast("Preencha os campos obrigatorios.");
                return;
            }
            if (registerMode) {
                api.register(nome.getText().toString().trim(), emailValue, senhaValue, sessionCallback());
            } else {
                api.login(emailValue, senhaValue, sessionCallback());
            }
        });

        Button secondary = ghostButton(registerMode ? "Ja tenho conta" : "Criar nova conta");
        screen.addView(secondary);
        secondary.setOnClickListener(view -> showAuth(!registerMode));
    }

    private ApiClient.Callback<User> sessionCallback() {
        return new ApiClient.Callback<User>() {
            @Override public void onSuccess(User user) {
                currentUser = user;
                saveSession(user);
                showDiscover();
            }

            @Override public void onError(String message) {
                toast(message);
            }
        };
    }

    private void showDiscover() {
        currentTab = "discover";
        buildShell("Descobrir");
        content.addView(label("Recomendadas para voce", 20, TEXT, Typeface.BOLD));
        content.addView(label("Receitas recentes da comunidade.", 14, MUTED, Typeface.NORMAL));
        loadRecipeList("", "", "", "", "Nenhuma receita encontrada ainda.");
    }

    private void showSearch() {
        currentTab = "search";
        buildShell("Buscar");
        EditText query = input("Nome da receita ou palavra-chave");
        EditText ingredient = input("Ingrediente");
        EditText tool = input("Utensilio ou ferramenta");
        content.addView(query);
        content.addView(ingredient);
        content.addView(tool);
        Button search = primaryButton("Aplicar filtros");
        content.addView(search);
        LinearLayout resultArea = section();
        content.addView(resultArea);
        search.setOnClickListener(view -> {
            resultArea.removeAllViews();
            resultArea.addView(label("Buscando...", 14, MUTED, Typeface.NORMAL));
            api.listRecipes(currentUser.id, query.getText().toString(), ingredient.getText().toString(), tool.getText().toString(), "", recipeListCallback(resultArea, "Nenhuma receita combina com esses filtros."));
        });
    }

    private void showCreate() {
        currentTab = "create";
        buildShell("Nova receita");
        EditText title = input("Titulo da receita");
        EditText description = input("Descricao curta");
        EditText ingredients = multiInput("Ingredientes, um por linha");
        EditText tools = multiInput("Ferramentas opcionais, uma por linha");
        EditText steps = multiInput("Passos, um por linha");
        content.addView(title);
        content.addView(description);
        content.addView(ingredients);
        content.addView(tools);
        content.addView(steps);
        Button publish = primaryButton("Publicar receita");
        content.addView(publish);
        publish.setOnClickListener(view -> {
            if (title.getText().toString().trim().isEmpty() || ingredients.getText().toString().trim().isEmpty() || steps.getText().toString().trim().isEmpty()) {
                toast("Titulo, ingredientes e passos sao obrigatorios.");
                return;
            }
            api.createRecipe(
                    currentUser.id,
                    title.getText().toString().trim(),
                    description.getText().toString().trim(),
                    lines(ingredients.getText().toString()),
                    lines(tools.getText().toString()),
                    lines(steps.getText().toString()),
                    new ApiClient.Callback<Recipe>() {
                        @Override public void onSuccess(Recipe data) {
                            toast("Receita publicada!");
                            showProfile();
                        }

                        @Override public void onError(String message) {
                            toast(message);
                        }
                    }
            );
        });
    }

    private void showSaved() {
        currentTab = "saved";
        buildShell("Salvas");
        content.addView(label("Suas favoritas", 20, TEXT, Typeface.BOLD));
        loadRecipeList("", "", "", "saved", "Voce ainda nao salvou receitas.");
    }

    private void showProfile() {
        currentTab = "profile";
        buildShell("Perfil");
        content.addView(label(currentUser.nome, 22, TEXT, Typeface.BOLD));
        content.addView(label(currentUser.email, 14, MUTED, Typeface.NORMAL));
        Button logout = ghostButton("Sair da conta");
        content.addView(logout);
        logout.setOnClickListener(view -> {
            getPreferences(MODE_PRIVATE).edit().clear().apply();
            currentUser = null;
            showAuth(false);
        });
        addSpacer(content, 12);
        content.addView(label("Minhas receitas", 20, TEXT, Typeface.BOLD));
        loadRecipeList("", "", "", "mine", "Voce ainda nao publicou receitas.");
    }

    private void loadRecipeList(String query, String ingredient, String tool, String mode, String emptyMessage) {
        LinearLayout area = section();
        content.addView(area);
        area.addView(label("Carregando receitas...", 14, MUTED, Typeface.NORMAL));
        api.listRecipes(currentUser.id, query, ingredient, tool, mode, recipeListCallback(area, emptyMessage));
    }

    private ApiClient.Callback<List<Recipe>> recipeListCallback(LinearLayout area, String emptyMessage) {
        return new ApiClient.Callback<List<Recipe>>() {
            @Override public void onSuccess(List<Recipe> recipes) {
                area.removeAllViews();
                if (recipes.isEmpty()) {
                    area.addView(label(emptyMessage, 14, MUTED, Typeface.NORMAL));
                    return;
                }
                for (Recipe recipe : recipes) area.addView(recipeCard(recipe));
            }

            @Override public void onError(String message) {
                area.removeAllViews();
                area.addView(label(message, 14, MUTED, Typeface.NORMAL));
            }
        };
    }

    private View recipeCard(Recipe recipe) {
        LinearLayout card = section();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(cardBackground());
        card.addView(label(recipe.title, 19, TEXT, Typeface.BOLD));
        card.addView(label("por " + recipe.authorName, 13, MUTED, Typeface.NORMAL));
        if (!recipe.description.isEmpty()) card.addView(label(recipe.description, 14, TEXT, Typeface.NORMAL));
        card.addView(label("Ingredientes", 15, BRAND_DARK, Typeface.BOLD));
        card.addView(label(join(recipe.ingredients), 14, TEXT, Typeface.NORMAL));
        if (!recipe.tools.isEmpty()) {
            card.addView(label("Ferramentas", 15, BRAND_DARK, Typeface.BOLD));
            card.addView(label(join(recipe.tools), 14, TEXT, Typeface.NORMAL));
        }
        card.addView(label("Passos", 15, BRAND_DARK, Typeface.BOLD));
        card.addView(label(numbered(recipe.steps), 14, TEXT, Typeface.NORMAL));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(8), 0, 0);
        Button like = smallButton((recipe.likedByCurrentUser ? "Curtido" : "Curtir") + " (" + recipe.likes + ")");
        Button save = smallButton(recipe.savedByCurrentUser ? "Salva" : "Salvar");
        actions.addView(like, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(actions);
        like.setOnClickListener(view -> api.toggleLike(currentUser.id, recipe.id, refreshCallback()));
        save.setOnClickListener(view -> api.toggleSave(currentUser.id, recipe.id, refreshCallback()));
        return card;
    }

    private ApiClient.Callback<Recipe> refreshCallback() {
        return new ApiClient.Callback<Recipe>() {
            @Override public void onSuccess(Recipe data) {
                if ("saved".equals(currentTab)) showSaved();
                else if ("profile".equals(currentTab)) showProfile();
                else if ("search".equals(currentTab)) showSearch();
                else showDiscover();
            }

            @Override public void onError(String message) {
                toast(message);
            }
        };
    }

    private void buildShell(String title) {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BACKGROUND);
        setContentView(shell);

        TextView header = label(title, 24, BRAND_DARK, Typeface.BOLD);
        header.setPadding(dp(18), dp(24), dp(18), dp(10));
        shell.addView(header);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(4), dp(18), dp(18));
        scroll.addView(content);
        shell.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        shell.addView(bottomBar());
    }

    private LinearLayout bottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(6), dp(6), dp(6), dp(8));
        bar.setBackgroundColor(SURFACE);
        addTab(bar, "discover", "Descobrir", view -> showDiscover());
        addTab(bar, "search", "Buscar", view -> showSearch());
        addTab(bar, "create", "Novo", view -> showCreate());
        addTab(bar, "saved", "Salvas", view -> showSaved());
        addTab(bar, "profile", "Perfil", view -> showProfile());
        return bar;
    }

    private void addTab(LinearLayout bar, String key, String title, View.OnClickListener listener) {
        Button button = ghostButton(title);
        button.setTextColor(key.equals(currentTab) ? BRAND : MUTED);
        button.setTypeface(Typeface.DEFAULT, key.equals(currentTab) ? Typeface.BOLD : Typeface.NORMAL);
        button.setTextSize(12);
        button.setOnClickListener(listener);
        bar.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(TEXT);
        editText.setHintTextColor(MUTED);
        editText.setSingleLine(true);
        editText.setPadding(dp(12), dp(10), dp(12), dp(10));
        editText.setBackground(cardBackground());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(7), 0, dp(7));
        editText.setLayoutParams(params);
        return editText;
    }

    private EditText multiInput(String hint) {
        EditText editText = input(hint);
        editText.setSingleLine(false);
        editText.setMinLines(3);
        editText.setGravity(Gravity.TOP | Gravity.START);
        return editText;
    }

    private TextView label(String text, int size, int color, int style) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setTypeface(Typeface.DEFAULT, style);
        label.setLineSpacing(0, 1.08f);
        label.setPadding(0, dp(4), 0, dp(4));
        return label;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(round(BRAND, dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(6));
        button.setLayoutParams(params);
        return button;
    }

    private Button ghostButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(BRAND_DARK);
        button.setTextSize(14);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private Button smallButton(String text) {
        Button button = ghostButton(text);
        button.setTextColor(ACCENT);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return button;
    }

    private LinearLayout section() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(10), 0, dp(10));
        layout.setLayoutParams(params);
        return layout;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = round(SURFACE, dp(8));
        drawable.setStroke(dp(1), Color.rgb(230, 226, 218));
        return drawable;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void addSpacer(LinearLayout layout, int height) {
        View spacer = new View(this);
        layout.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private List<String> lines(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            String clean = line.trim();
            if (!clean.isEmpty()) result.add(clean);
        }
        return result;
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) builder.append("• ").append(value).append("\n");
        return builder.toString().trim();
    }

    private String numbered(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) builder.append(i + 1).append(". ").append(values.get(i)).append("\n");
        return builder.toString().trim();
    }

    private void saveSession(User user) {
        getPreferences(MODE_PRIVATE).edit()
                .putInt("id", user.id)
                .putString("nome", user.nome)
                .putString("email", user.email)
                .apply();
    }

    private void loadSession() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        int id = preferences.getInt("id", 0);
        if (id > 0) currentUser = new User(id, preferences.getString("nome", ""), preferences.getString("email", ""));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
