package it.reveal.bugcheck.log4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@RestController
public class Log4jApplication implements CommandLineRunner {
    private static final Logger LOGGER = LogManager.getLogger(Log4jApplication.class);

    @Value("${server.port}")
    private Integer port;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    private static final String PATH = "/dumb";

    private static ConfigurableApplicationContext context;

    private static final String HEADER_NAME = "X-Check-Bug";
    private static final String HEADER_VALUE = "${jndi:ldap://127.0.0.1/a}";

    public static void main(String[] args) {
        context = SpringApplication.run(Log4jApplication.class, args);
    }

    @GetMapping(value = PATH)
    public String check(@RequestHeader HttpHeaders request) {
        System.out.println("The check method is hit");
        final String value = request.getFirst(HEADER_NAME);
        System.out.println(String.format("The request contains a header %s with value %s", HEADER_NAME, value));
        System.out.println("Logging the value " + value);
        LOGGER.info("header: {}, value: {}", HEADER_NAME, value);
        return value;
    }

    @Override
    public void run(String... args) throws Exception {
        final String version = System.getProperty("java.version");
        System.out.println("Java version: " + version);
        System.out.println(getClassInfo(LOGGER.getClass()));
        final String url = String.format("http://localhost:%d%s", port, contextPath);
        System.out.println(String.format("Hitting %s%s with header %s=%s", url, PATH, HEADER_NAME, HEADER_VALUE));
        final WebClient client = WebClient.create(url);
        try {
            final String responce = client.get()
                .uri(PATH)
                .header(HEADER_NAME, HEADER_VALUE)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture()
                .get();
            this.onSuccess(responce, HEADER_VALUE);
        } catch (Throwable t) {
            System.err.println("Failed to make a request " + t.getLocalizedMessage());
        }
        System.out.println("Exiting...");
        System.exit(0);

    }

    private void onSuccess(String actual, String expected) {
        if (actual != null && actual.equals(expected)) {
            System.out.printf("The responce [%s] coincides with the expected one.%nEverything seems OK.%n", actual);
        } else {
            System.err.printf("The responce [%s] differs from the expected one [%s].%nIt might be a problem!%n", actual, expected);
        }
    }

    private String getClassInfo(Class<?> clazz) {
        Package p = clazz.getPackage();
        return String.format("Class name: %s%nTitle: %s%nVersion: %s%nVendor: %s", clazz.getName(), p.getImplementationTitle(), p.getImplementationVersion(), p.getImplementationVendor());
    }

}
