package org.example;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Создаем экземпляр CrptApi
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 1, "https://ismp.crpt.ru/api/v3/lk/documents/create");

        // Читаем JSON из файла
        String json = "";
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("src/main/resources/document.json"));
            json = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Преобразуем JSON в объект Document
        Gson gson = new Gson();
        CrptApi.Document document = gson.fromJson(json, CrptApi.Document.class);

        // Создаем подпись
        String signature = "signature";

        // Токен аутентификации
        String authToken = "your_auth_token"; // Нужно заменить на действительный токен аутентификации

        // Вызываем createDocument()
        api.createDocument(document, signature, authToken);

        // Завершаем работу с api
        api.shutdown();
    }
}
