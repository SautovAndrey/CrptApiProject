package org.example;

import com.google.gson.Gson;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс CrptApi предназначен для работы с API Честного знака.
 * Он поддерживает ограничение на количество запросов к API в заданный промежуток времени.
 */
public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    public Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    public HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiUrl;
    private static final Logger logger = Logger.getLogger(CrptApi.class.getName());

    /**
     * Конструктор класса CrptApi.
     *
     * @param timeUnit      единица времени для интервала запросов.
     * @param requestLimit  максимальное количество запросов в заданный интервал времени.
     * @param apiUrl        URL API для отправки запросов.
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit, String apiUrl) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.apiUrl = apiUrl;

        // Запускаем планировщик для сброса счетчика запросов
        this.scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, 0, 1, timeUnit);
    }

    /**
     * Метод для создания документа.
     *
     * @param document  документ для создания.
     * @param signature подпись для документа.
     * @param authToken токен аутентификации.
     * @throws InterruptedException если поток был прерван во время ожидания разрешения.
     */
    public void createDocument(Document document, String signature, String authToken) throws InterruptedException {
        semaphore.acquire();
        try {
            HttpRequest request = buildRequest(document, signature, authToken);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Проверяем статус ответа
            if (response.statusCode() == 200) {
                // Обрабатываем тело ответа при успешном статусе
                String responseBody = response.body();
                logger.info("Успешный ответ: " + responseBody);
                // Здесь вы можете добавить код для обработки тела ответа
                processResponseBody(responseBody);
            } else {
                // Обрабатываем ошибку при неуспешном статусе
                logger.warning("Ошибка: " + response.statusCode() + " Тело ответа: " + response.body());
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при отправке запроса: " + e.getMessage(), e);
        } finally {
            semaphore.release();
        }
    }

    /**
     * Метод для обработки тела ответа.
     *
     * @param responseBody тело ответа для обработки.
     */
    private void processResponseBody(String responseBody) {
        // Здесь вы можете добавить код для обработки тела ответа
    }

    /**
     * Метод для остановки планировщика.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Метод для построения HTTP-запроса.
     *
     * @param document  документ для создания запроса.
     * @param signature подпись для документа.
     * @param authToken токен аутентификации.
     * @return построенный HTTP-запрос.
     */
    private HttpRequest buildRequest(Document document, String signature, String authToken) {
        String json = convertToJson(document);
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    /**
     * Метод для преобразования документа в JSON.
     *
     * @param document документ для преобразования.
     * @return JSON-строка.
     */
    private String convertToJson(Document document) {
        Gson gson = new Gson();
        return gson.toJson(document);
    }

    /**
     * Внутренний класс для представления документа.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }

    /**
     * Внутренний класс для представления описания.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }

    /**
     * Внутренний класс для представления продукта.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
}
