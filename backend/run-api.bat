@echo off
if not exist out mkdir out
javac -encoding UTF-8 -d out src\main\java\com\receitagram\api\RecipeApiServer.java
if errorlevel 1 exit /b 1
java -cp out com.receitagram.api.RecipeApiServer
