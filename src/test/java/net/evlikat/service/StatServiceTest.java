package net.evlikat.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class StatServiceTest {

    private StatService service;

    @Mock
    private ScheduledExecutorService executorService;

    @Test
    public void shouldAcceptProperTransaction() {
        service = new StatService(() -> 120000L, executorService);
        service.acceptTransaction(new Transaction(5.0, 120000L));

        assertThat(service.lastMinuteStatistics().getCount()).isEqualTo(1);
    }

    @Test
    public void shouldNotAcceptTooOldTransaction() {
        service = new StatService(() -> 120000L, executorService);
        service.acceptTransaction(new Transaction(5.0, 20000L));

        assertThat(service.lastMinuteStatistics().getCount()).isEqualTo(0);
    }

    @Test
    public void shouldRetainAcceptedProperTransactions() {
        service = new StatService(() -> 120000L, executorService);
        service.acceptTransaction(new Transaction(5.0, 80000L));
        service.acceptTransaction(new Transaction(5.0, 20000L));

        assertThat(service.lastMinuteStatistics().getCount()).isEqualTo(1);
    }

    @Test
    public void shouldRetainAcceptedProperTransactionsSineTimes() {
        Iterator<Long> it = Arrays.asList(110000L, 120000L, 120000L, 130000L, 140000L, 140000L).iterator();
        service = new StatService(it::next, executorService);
        service.onTick();                                         // 110000
        service.acceptTransaction(new Transaction(5.0, 70000L));  // 120000
        service.onTick();                                         // 120000
        service.onTick();                                         // 130000
        service.acceptTransaction(new Transaction(5.0, 110000L)); // 140000
        service.onTick();                                         // 140000

        assertThat(service.lastMinuteStatistics().getCount()).isEqualTo(1);
    }
}