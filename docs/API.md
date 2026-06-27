# API REST do Receitagram

Base local:

```text
http://localhost:8080
```

No app Android em emulador, a mesma API é acessada por:

```text
http://10.0.2.2:8080
```

## Rotas

### Saúde da API

`GET /health`

Resposta:

```json
{ "status": "ok" }
```

### Criar conta

`POST /auth/register`

Corpo:

```json
{
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "senha": "123456"
}
```

### Login

`POST /auth/login`

Corpo:

```json
{
  "email": "ana@demo.com",
  "senha": "123456"
}
```

### Listar receitas

`GET /recipes?userId=1`

Filtros opcionais:

- `query`: busca por título ou descrição.
- `ingredient`: filtra por ingrediente.
- `tool`: filtra por ferramenta.
- `mode=saved`: retorna receitas salvas pelo usuário.
- `mode=mine`: retorna receitas publicadas pelo usuário.

Exemplo:

```text
GET /recipes?userId=1&query=bolo&ingredient=cenoura
```

### Criar receita

`POST /recipes`

Cabeçalho obrigatório:

```text
X-User-Id: 1
```

Corpo:

```json
{
  "title": "Pão de queijo simples",
  "description": "Receita rápida para o lanche.",
  "ingredients": ["Polvilho", "Queijo", "Ovos", "Leite"],
  "tools": ["Tigela", "Forno"],
  "steps": ["Misture os ingredientes", "Modele as bolinhas", "Asse até dourar"]
}
```

### Curtir ou remover curtida

`POST /recipes/{id}/like`

Cabeçalho:

```text
X-User-Id: 1
```

### Salvar ou remover dos salvos

`POST /recipes/{id}/save`

Cabeçalho:

```text
X-User-Id: 1
```
