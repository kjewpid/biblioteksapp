package se.yrgo.libraryapp.integrations;

import io.jooby.JoobyTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.yrgo.libraryapp.App;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
@Testcontainers
@JoobyTest(value = App.class, factoryMethod = "createApp")
public class LoginLogoutIntegrationTest {
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
    public void alwaysOkToLogout(int serverPort) throws IOException {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + serverPort + "/logout")).build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals("", resp.body());
            assertEquals(HttpURLConnection.HTTP_OK, resp.statusCode());
        } catch (IllegalArgumentException | InterruptedException ex) {
            fail(ex);
        }
    }

    @Test
    public void loginAndLogoutWithValidUser(int serverPort) throws IOException {
        try {
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();

            String loginData = """
                    {
                        "username": "test",
                        "password": "yrgoP4ssword"
                    }
                    """;

            HttpRequest loginRequest = HttpUtils.sendHttpPost("/login", serverPort, "Content-Type", "application/json", loginData);
            HttpResponse<String> loginResponse = HttpUtils.getHttpResponse(client, loginRequest);

            assertThat(loginResponse.statusCode()).isEqualTo(200);

            HttpRequest logoutRequest = HttpUtils.sendHttpGet("/logout", serverPort);
            HttpResponse<String> logoutResponse = HttpUtils.getHttpResponse(client, logoutRequest);

            assertThat(logoutResponse.statusCode()).isEqualTo(200);

            HttpRequest checkRequest = HttpUtils.sendHttpGet("/login/check", serverPort);
            HttpResponse<String> checkResponse = HttpUtils.getHttpResponse(client, checkRequest);

            assertThat(checkResponse.statusCode()).isEqualTo(200);
            assertThat(checkResponse.body()).isEqualTo("[]");
        } catch (InterruptedException e) {
            fail(e);
        }
    }


    @Test
    public void loginWithInvalidUser(int serverPort) {
        try {
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();

            String invalidLoginData = """
                    {
                        "username":"invalid",
                        "password":"madeup123"
                    }
                    """;

            HttpRequest loginRequest = HttpUtils.sendHttpPost(
                    "/login", serverPort, "Content-Type",
                    "application/json", invalidLoginData);
            HttpResponse<String> loginResponse = HttpUtils.getHttpResponse(client, loginRequest);

            assertThat(loginResponse.statusCode()).isEqualTo(401);
            assertThat(cookieManager.getCookieStore().getCookies()).isEmpty();
        } catch (InterruptedException | IOException e) {
            fail(e);
        }
    }

    @Test
    public void registerValidUser(int serverPort) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String registerData = HttpUtils.createRegisterData(
                    "Tulipe", "DaCoolestFlower", "supersecretpassword");

            HttpRequest registerRequest = HttpUtils.sendHttpPost(
                    "/register", serverPort, "Content-Type",
                    "Application/json", registerData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, registerRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("true");
        } catch (IOException | InterruptedException e) {
            fail(e);
        }
    }

    @Test
    public void registerUserWithAlreadyReservedName(int serverPort) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String registerData = HttpUtils.createRegisterData(
                    "test", "realname", "password");

            HttpRequest registerRequest = HttpUtils.sendHttpPost(
                    "/register", serverPort, "Content-Type",
                    "application/json", registerData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, registerRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("false");
        } catch (IOException | InterruptedException e){
            fail(e);
        }
    }

    @Test
    public void registerWithInvalidName(int serverPort) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String registerData = HttpUtils.createRegisterData(
                    "", "realname", "password");

            HttpRequest registerRequest = HttpUtils.sendHttpPost(
                    "/register", serverPort, "Content-Type",
                    "application/json", registerData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, registerRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("false");
        } catch (IOException | InterruptedException e){
            fail(e);
        }
    }

    @Test
    public void registerWithInvalidRealName(int serverPort) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String registerData = HttpUtils.createRegisterData(
                    "tulipe", "", "password");

            HttpRequest registerRequest = HttpUtils.sendHttpPost(
                    "/register", serverPort, "Content-Type",
                    "application/json", registerData
            );

            HttpResponse<String> response = HttpUtils.getHttpResponse(client, registerRequest);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("false");
        } catch (IOException | InterruptedException e){
            fail(e);
        }
    }


}