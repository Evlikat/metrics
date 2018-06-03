package net.evlikat.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.DoubleSummaryStatistics;

@RestController
public class Controller {

    private final StatService service;

    public Controller(StatService service) {
        this.service = service;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public void accept(@RequestBody Transaction transaction) {
        service.acceptTransaction(transaction);
    }

    @GetMapping("/statistics")
    public DoubleSummaryStatistics lastMinuteStatistics() {
        return service.lastMinuteStatistics();
    }
}
