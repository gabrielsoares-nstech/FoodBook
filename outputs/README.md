# FoodBook

FoodBook é um aplicativo Android em Java para compartilhar receitas em formato de rede social. O projeto inclui um app Android e uma API REST local em Java.

## Funcionalidades

- Cadastro e login de usuários.
- Feed de descoberta com receitas recomendadas/recentes.
- Publicação de receitas com ingredientes, ferramentas opcionais e passos.
- Busca com múltiplos filtros: texto, ingrediente e ferramenta.
- Curtir receitas de outras pessoas.
- Salvar receitas favoritas.
- Perfil com dados da conta e receitas publicadas pelo usuário.

## Estrutura do projeto

- `app/`: aplicativo Android em Java para abrir no Android Studio.
- `backend/`: API REST local feita com Java puro, sem framework externo.
- `docs/API.md`: resumo das rotas da API.

## Como rodar a API REST

Requisitos: Java 17 ou superior instalado.

No terminal, a partir da pasta do projeto:

```bash
cd backend
mkdir out
javac -encoding UTF-8 -d out src/main/java/com/FoodBook/api/RecipeApiServer.java
java -cp out com.FoodBook.api.RecipeApiServer
```

A API ficará disponível em:

```text
http://localhost:8080
```

Para testar rapidamente:

```text
GET http://localhost:8080/health
```

## Como rodar o app Android

1. Abra o Android Studio.
2. Escolha **Open** e selecione a pasta raiz deste projeto.
3. Aguarde a sincronização do Gradle.
4. Inicie a API REST local seguindo os passos acima.
5. Rode o app em um emulador Android.

O app usa a URL `http://10.0.2.2:8080`, que é o endereço usado pelo emulador Android para acessar o `localhost` do computador.

Se for testar em um celular físico, altere a constante `BASE_URL` em `app/src/main/java/com/FoodBook/app/data/ApiClient.java` para o IP do computador na mesma rede, por exemplo:

```java
private static final String BASE_URL = "http://192.168.0.10:8080";
```

## Contas de demonstração

A API já inicia com duas contas:

- Email: `ana@demo.com` | Senha: `123456`
- Email: `bruno@demo.com` | Senha: `123456`

Também é possível criar uma nova conta pela tela inicial do app.

## Observações

Esta versão usa dados em memória na API. Ao encerrar o servidor, novos usuários, curtidas, salvos e receitas publicadas durante a sessão são perdidos. Isso mantém o projeto simples para estudo e demonstração; para produção, o próximo passo seria persistir os dados em um banco como PostgreSQL, MySQL ou SQLite.


Consulte tambem `docs/API.md` dentro do projeto para detalhes das rotas.
