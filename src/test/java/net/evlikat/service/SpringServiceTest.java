package net.evlikat.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.DoubleSummaryStatistics;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = Application.class)
public class SpringServiceTest {

    private RestTemplate restTemplate;

    @Before
    public void setUp() throws Exception {
        restTemplate = new RestTemplate();
    }

    @Test
    @DirtiesContext
    public void shouldProperlyPutTransactions() {
        int totalRequests = 1000;
        IntStream.range(0, totalRequests)
                .parallel()
                .forEach(i -> {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                    HttpEntity<String> entity = new HttpEntity<>(generateTransaction(i), httpHeaders);
                    restTemplate.exchange("http://localhost:8080/transactions",
                            HttpMethod.POST,
                            entity,
                            String.class);
                });

        DoubleSummaryStatistics stat =
                restTemplate.getForObject("http://localhost:8080/statistics", DoubleSummaryStatistics.class);

        assertThat(stat.getCount()).isEqualTo(totalRequests);
        assertThat(stat.getSum()).isEqualTo(5500.0);
        assertThat(stat.getAverage()).isEqualTo(5.5);
        assertThat(stat.getMin()).isEqualTo(1.0);
        assertThat(stat.getMax()).isEqualTo(10.0);
    }

    @Test
    @DirtiesContext
    public void shouldProperlyPutTransactionsAndDeclineOld() {
        int totalRequests = 1000;
        IntStream.range(0, totalRequests)
                .parallel()
                .forEach(i -> {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                    HttpEntity<String> entity = new HttpEntity<>(generateTransactionOrOld(i), httpHeaders);
                    restTemplate.exchange("http://localhost:8080/transactions",
                            HttpMethod.POST,
                            entity,
                            String.class);
                });

        DoubleSummaryStatistics stat =
                restTemplate.getForObject("http://localhost:8080/statistics", DoubleSummaryStatistics.class);

        assertThat(stat.getCount()).isEqualTo(totalRequests - 100);
        assertThat(stat.getSum()).isEqualTo(5400.0);
        assertThat(stat.getAverage()).isEqualTo(6.0);
        assertThat(stat.getMin()).isEqualTo(2.0);
        assertThat(stat.getMax()).isEqualTo(10.0);
    }

    @Test
    @DirtiesContext
    public void shouldProperlyPutTransactionsAndDeclineOldAndRequestStatistics() {
        int totalRequests = 1000;
        IntStream.range(0, totalRequests)
                .parallel()
                .forEach(i -> {
                    if (i % 10 == 1) {
                        restTemplate.getForObject("http://localhost:8080/statistics", DoubleSummaryStatistics.class);
                        return;
                    }
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                    HttpEntity<String> entity = new HttpEntity<>(generateTransactionOrOld(i), httpHeaders);
                    restTemplate.exchange("http://localhost:8080/transactions",
                            HttpMethod.POST,
                            entity,
                            String.class);
                });

        DoubleSummaryStatistics stat =
                restTemplate.getForObject("http://localhost:8080/statistics", DoubleSummaryStatistics.class);

        assertThat(stat.getCount()).isEqualTo(totalRequests - 200);
        assertThat(stat.getSum()).isEqualTo(5200.0);
        assertThat(stat.getAverage()).isEqualTo(6.5);
        assertThat(stat.getMin()).isEqualTo(3.0);
        assertThat(stat.getMax()).isEqualTo(10.0);
    }

    private String generateTransaction(int index) {
        return "{\"amount\": " + ((index % 10) + 1) + ", \"timestamp\": " + System.currentTimeMillis() + "}";
    }

    private String generateTransactionOrOld(int index) {
        long ts = System.currentTimeMillis();
        if (index % 10 == 0) {
            ts -= 2 * 60 * 1000;
        }
        return "{\"amount\": " + ((index % 10) + 1) + ", \"timestamp\": " + ts + "}";
    }
}
