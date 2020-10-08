
import static java.util.Objects.isNull;

import java.io.File;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@RunWith(VertxUnitRunner.class)
public class WebClientSendStreamTest {

    private static final String HOST = "0.0.0.0";
    private static final int PORT = 8085;
    private static final String PATH_TO_FILE = "test_path/test.json";


    public static Vertx vertx;
    public static HttpServer server;

    @BeforeClass
    public static void start(TestContext context) {
        Async completion = context.async();
        vertx = Vertx.vertx();
        server = vertx.createHttpServer(new HttpServerOptions().setHost(HOST).setPort(PORT));
        Router router = Router.router(vertx);
        router.post("/upload").handler(e -> {
            vertx.setTimer(2000, end -> {
                server.close();
            });
        });
        server.requestHandler(router).listen(e -> {
            completion.complete();
        });
    }

    @AfterClass
    public static void stop(TestContext context) {
        Async completion = context.async();
        vertx.close(e -> {
            if (e.succeeded()) {
                completion.complete();
            } else {
                context.fail(e.cause());
            }
        });
    }

    // Vertx WebClient HttpRequest.sendStream() hangs when HttpServer close connection
    @Test()
    public void uploadStreamRequestTest(TestContext context) {
        Async completion = context.async();
        File file = getFileFromResources(PATH_TO_FILE);

        // get stream from file
        vertx.fileSystem().open(file.getAbsolutePath(), new OpenOptions(), ar -> {
            if (ar.succeeded()) {
                WebClientOptions options = new WebClientOptions().setDefaultHost(HOST).setDefaultPort(PORT);
                WebClient client = WebClient.create(vertx, options);
                // send stream to server
                HttpRequest<Buffer> httpRequest = client.post(PORT, HOST, "/upload");
                httpRequest.sendStream(ar.result(), e -> {
                    if (e.succeeded()) {
                        HttpResponse<Buffer> response = e.result();
                        System.out.println("Received response with status code: " + response.statusCode());
                        context.assertEquals(200, response.statusCode());
                        String resp = response.bodyAsString();
                        System.out.println(resp);
                        context.assertEquals("done", resp);
                    } else {
                        System.out.println("Something went wrong " + e.cause().getMessage());
                        context.fail(e.cause());
                    }
                    completion.complete();
                });
            } else {
                context.fail(ar.cause());
            }
        });
    }


    private static File getFileFromResources(String pathToResource) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(pathToResource);
        if (isNull(url)) {
            throw new RuntimeException("Resource not found : " + pathToResource);
        }
        return new File(url.getFile());
    }
}
