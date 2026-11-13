package se.yrgo.libraryapp.integrations;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import io.jooby.JoobyTest;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import se.yrgo.libraryapp.App;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
@JoobyTest(value = App.class, factoryMethod = "createApp")
public class AdminLoanIntegrationTest {
    @Container
    private static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.29");

    public static App createApp() {
        System.setProperty("db.url", mySQLContainer.getJdbcUrl());
        System.setProperty("db.user", mySQLContainer.getUsername());
        System.setProperty("db.password", mySQLContainer.getPassword());
        return new App();
    }

    @BeforeEach
    public void resetDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        mySQLContainer.getJdbcUrl(),
                        mySQLContainer.getUsername(),
                        mySQLContainer.getPassword()
                )
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();
    }

    @AfterAll
    static void reset() {
        System.clearProperty("db.url");
        System.clearProperty("db.user");
        System.clearProperty("db.password");
    }

    @Test
    public void loanAvailableBook(int serverPort) {
        try {
            HttpClient client = createLoggedInAdminClient(serverPort);

            String loanData = """
                    {
                        "book": 1,
                        "user": 2
                    }
                    """;

            HttpRequest loanRequest = HttpUtils.sendHttpPost(
                    "/admin/loan/lend", serverPort, "Content-Type",
                    "application/json", loanData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, loanRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("true");
        } catch (IOException | InterruptedException e) {
            fail(e);
        }
    }

    @Test
    public void loanUnavailableBook(int serverPort) {
        try {
            HttpClient client = createLoggedInAdminClient(serverPort);

            String loanData = """
                    {
                        "book": 2,
                        "user": 2
                    }
                    """;

            HttpRequest loanRequest = HttpUtils.sendHttpPost(
                    "/admin/loan/lend", serverPort, "Content-Type",
                    "application/json", loanData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, loanRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("false");
        } catch (IOException | InterruptedException e) {
            fail(e);
        }
    }

    @Test
    public void returnBook(int serverPort) {
        try {
            HttpClient client = createLoggedInAdminClient(serverPort);

            String returnData = "2";

            HttpRequest loanRequest = HttpUtils.sendHttpPost(
                    "/admin/loan/return", serverPort, "Content-Type",
                    "application/json", returnData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, loanRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("true");
        } catch (IOException | InterruptedException e) {
            fail(e);
        }
    }

    @Test
    public void returnUnloanedBook(int serverPort) {
        try {
            HttpClient client = createLoggedInAdminClient(serverPort);

            String returnData = "1";

            HttpRequest loanRequest = HttpUtils.sendHttpPost(
                    "/admin/loan/return", serverPort, "Content-Type",
                    "application/json", returnData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, loanRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("false");
        } catch (IOException | InterruptedException e) {
            fail(e);
        }
    }

    @Test
    public void getOverdueBooks(int serverPort) {
        try {
            HttpClient client = createLoggedInAdminClient(serverPort);

            HttpRequest loanRequest = HttpUtils.sendHttpGet(
                    "/admin/loan/overdue", serverPort
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, loanRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .contains("Här kommer Pippi Långstrump")
                    .contains("T-SQL Fundamentals")
                    .contains("\"overdue\":true");
        } catch (IOException | InterruptedException e) {
            fail(e);
        }
    }

    private void loginAsAdmin(HttpClient client, int serverPort) throws IOException, InterruptedException {
        String loginData = """
                {
                    "username": "test2",
                    "password": "yrgoP4ssword"
                }
                """;

        HttpRequest loginRequest = HttpUtils.sendHttpPost(
                "/login", serverPort,
                "Content-Type", "application/json", loginData
        );

        HttpResponse<String> response =
                HttpUtils.getHttpResponse(client, loginRequest);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpClient createClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();
    }

    private HttpClient createLoggedInAdminClient(int serverPort)
            throws IOException, InterruptedException {
        HttpClient client = createClient();
        loginAsAdmin(client, serverPort);
        return client;
    }
}
