#!/usr/bin/env bash
set -e
mkdir -p out
javac -encoding UTF-8 -d out src/main/java/com/receitagram/api/RecipeApiServer.java
java -cp out com.receitagram.api.RecipeApiServer
