package de.grimmpp.AppManager.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

    public final static long DIST_BWT_RUNS =  3 * 60 * 1000; // each 3 min

    @Scheduled(fixedRate = DIST_BWT_RUNS)
    public void run() {

    }
}
