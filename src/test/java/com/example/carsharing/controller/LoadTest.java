package com.example.carsharing.controller;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class LoadTest {

    private static final String LOG_FILE = "load_test_log.txt";
    private static final int USERS_PER_BATCH = 5;
    private static final int TOTAL_USERS = 1;

    // Константные учётные данные единственного пользователя
    private static final String LOGIN_EMAIL = "supersamat2004@gmail.com";
    private static final String LOGIN_PASSWORD = "supersamat";

    private static synchronized void logToFile(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.printf("%s - %s%n", Instant.now(), message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Statistics {
        long totalResponseTime = 0;
        long maxResponseTime = 0;
        int successfulRequests = 0;
        int failedRequests = 0;
    }

    private static Callable<Void> userSimulation(int simIndex, Statistics stats) {
        return () -> {
            WebDriver driver = new ChromeDriver();
            Instant start = Instant.now();
            try {
                logToFile("Симуляция #" + simIndex + ": старт");
                driver.get("http://localhost:3000/login");

                // Используем один и тот же email/password
                driver.findElement(By.name("email"))
                        .sendKeys(LOGIN_EMAIL);
                driver.findElement(By.name("password"))
                        .sendKeys(LOGIN_PASSWORD);

                // Кликаем кнопку входа (имейте в виду, что она должна иметь type="submit" в JSX!)
                driver.findElement(By.cssSelector("button[type='submit']"))
                        .click();

                // Ждём перехода на главную страницу до 30 сек
                new WebDriverWait(driver, Duration.ofSeconds(30))
                        .until(d -> d.getCurrentUrl().equals("http://localhost:3000/"));

                long rt = Duration.between(start, Instant.now()).toMillis();
                synchronized (stats) {
                    stats.totalResponseTime += rt;
                    stats.maxResponseTime = Math.max(stats.maxResponseTime, rt);
                    stats.successfulRequests++;
                }
                logToFile("Симуляция #" + simIndex + ": УСПЕХ, время = " + rt + " мс");
            } catch (Exception e) {
                synchronized (stats) {
                    stats.failedRequests++;
                }
                logToFile("Симуляция #" + simIndex + ": ОШИБКА — " + e.getMessage());
            } finally {
                driver.quit();
            }
            return null;
        };
    }

    public static void main(String[] args) throws InterruptedException {
        // Очистка предыдущего лога
        try (PrintWriter pw = new PrintWriter(LOG_FILE)) {
        } catch (IOException ignored) {}

        logToFile("=== Начало нагрузочного теста ===");

        Statistics stats = new Statistics();
        ExecutorService executor = Executors.newFixedThreadPool(USERS_PER_BATCH);
        Instant testStart = Instant.now();

        // Запускаем TOTAL_USERS симуляций батчами по USERS_PER_BATCH
        List<Future<Void>> batch = new ArrayList<>();
        for (int i = 1; i <= TOTAL_USERS; i++) {
            batch.add(executor.submit(userSimulation(i, stats)));
            if (i % USERS_PER_BATCH == 0 || i == TOTAL_USERS) {
                for (Future<Void> f : batch) {
                    try { f.get(); } catch (ExecutionException ignored) {}
                }
                batch.clear();
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        // Считаем итоговые метрики
        Instant testEnd = Instant.now();
        long totalTime = Duration.between(testStart, testEnd).toMillis();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double loadAvg = osBean.getSystemLoadAverage();

        double avgResp = stats.successfulRequests > 0
                ? (double) stats.totalResponseTime / stats.successfulRequests
                : 0;
        double failureRate = (double) stats.failedRequests / TOTAL_USERS * 100;
        double throughput = TOTAL_USERS / (totalTime / 1000.0);

        logToFile("=== Конец нагрузочного теста ===");
        logToFile(String.format("Общее время: %d мс", totalTime));
        logToFile(String.format("Среднее время отклика: %.2f мс", avgResp));
        logToFile(String.format("Максимальное время отклика: %d мс", stats.maxResponseTime));
        logToFile(String.format("Ошибки: %d (%.2f%%)", stats.failedRequests, failureRate));
        logToFile(String.format("Пропускная способность: %.2f запросов/с", throughput));
        logToFile(String.format("Нагрузка на систему (1 мин.): %.2f", loadAvg));

        System.out.println("=== Итоги нагрузочного теста ===");
        System.out.printf("Общее время: %d мс%n", totalTime);
        System.out.printf("Среднее время отклика: %.2f мс%n", avgResp);
        System.out.printf("Максимальное время отклика: %d мс%n", stats.maxResponseTime);
        System.out.printf("Ошибки: %d (%.2f%%)%n", stats.failedRequests, failureRate);
        System.out.printf("Пропускная способность: %.2f запросов/с%n", throughput);
        System.out.printf("Нагрузка на систему (1 мин.): %.2f%n", loadAvg);
    }
}
