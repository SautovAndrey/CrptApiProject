
import org.example.CrptApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CrptApiTest {
    private CrptApi crptApi;
    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockHttpResponse = mock(HttpResponse.class);
        crptApi = new CrptApi(TimeUnit.SECONDS, 1, "https://ismp.crpt.ru/api/v3/lk/documents/create");

        // Replace the real HttpClient with the mock
        CrptApi finalCrptApi = crptApi;
        finalCrptApi.httpClient = mockHttpClient;
    }

    @Test
    void testCreateDocumentSuccess() throws IOException, InterruptedException {
        // Arrange
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"status\":\"success\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer((Answer<HttpResponse<String>>) invocation -> mockHttpResponse);

        CrptApi.Document document = new CrptApi.Document();
        document.setDescription(new CrptApi.Description("1234567890"));
        document.setDoc_id("doc_id_value");
        document.setDoc_status("DRAFT");
        document.setDoc_type("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwner_inn("owner_inn_value");
        document.setParticipant_inn("participant_inn_value");
        document.setProducer_inn("producer_inn_value");
        document.setProduction_date("2020-01-23");
        document.setProduction_type("production_type_value");
        document.setProducts(Collections.emptyList());
        document.setReg_date("2020-01-23");
        document.setReg_number("reg_number_value");

        // Act
        crptApi.createDocument(document, "signature", "authToken");

        // Assert
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testCreateDocumentUnauthorized() throws IOException, InterruptedException {
        // Arrange
        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpResponse.body()).thenReturn("{\"error\":\"Unauthorized\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer((Answer<HttpResponse<String>>) invocation -> mockHttpResponse);

        CrptApi.Document document = new CrptApi.Document();
        document.setDescription(new CrptApi.Description("1234567890"));
        document.setDoc_id("doc_id_value");
        document.setDoc_status("DRAFT");
        document.setDoc_type("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwner_inn("owner_inn_value");
        document.setParticipant_inn("participant_inn_value");
        document.setProducer_inn("producer_inn_value");
        document.setProduction_date("2020-01-23");
        document.setProduction_type("production_type_value");
        document.setProducts(Collections.emptyList());
        document.setReg_date("2020-01-23");
        document.setReg_number("reg_number_value");

        // Act
        crptApi.createDocument(document, "signature", "authToken");

        // Assert
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testSemaphoreLimiting() throws InterruptedException, IOException {
        // Arrange
        HttpClient realHttpClient = HttpClient.newHttpClient();
        CrptApi spyCrptApi = spy(new CrptApi(TimeUnit.SECONDS, 1, "https://ismp.crpt.ru/api/v3/lk/documents/create"));
        spyCrptApi.httpClient = realHttpClient;
        Semaphore semaphore = new Semaphore(1);
        spyCrptApi.semaphore = semaphore;

        CrptApi.Document document = new CrptApi.Document();
        document.setDescription(new CrptApi.Description("1234567890"));
        document.setDoc_id("doc_id_value");
        document.setDoc_status("DRAFT");
        document.setDoc_type("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwner_inn("owner_inn_value");
        document.setParticipant_inn("participant_inn_value");
        document.setProducer_inn("producer_inn_value");
        document.setProduction_date("2020-01-23");
        document.setProduction_type("production_type_value");
        document.setProducts(Collections.emptyList());
        document.setReg_date("2020-01-23");
        document.setReg_number("reg_number_value");

        // Act
        Thread testThread = new Thread(() -> {
            try {
                spyCrptApi.createDocument(document, "signature", "authToken");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        testThread.start();

        // Assert
        assertTimeout(Duration.ofSeconds(2), () -> {
            assertEquals(0, semaphore.availablePermits());
        });

        // Clean up
        testThread.join();
        spyCrptApi.shutdown();
    }
}