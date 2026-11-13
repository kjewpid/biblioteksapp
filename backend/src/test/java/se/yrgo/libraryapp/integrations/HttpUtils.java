package se.yrgo.libraryapp.integrations;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpUtils {

    public static HttpResponse<String> getHttpResponse(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpRequest sendHttpPost(String endpoint, int serverPort, String headerName, String headerValue, String data) {
        return HttpRequest.newBuilder().uri(URI.create("http://localhost:" + serverPort + endpoint)).header(headerName, headerValue).POST(HttpRequest.BodyPublishers.ofString(data)).build();
    }

    public static HttpRequest sendHttpGet(String url, int serverPort) {
        return HttpRequest.newBuilder().uri(URI.create("http://localhost:" + serverPort + url)).GET().build();
    }

    public static String createRegisterData(String name, String realname, String password) {
        return String.format("""
                {
                    "name":"%s",
                    "realName":"%s",
                   "password":"%s"
                }
                """, name, realname, password);

    }
}
