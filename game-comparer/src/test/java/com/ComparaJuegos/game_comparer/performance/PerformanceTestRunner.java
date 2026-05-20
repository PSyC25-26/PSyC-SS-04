package com.ComparaJuegos.game_comparer.performance;

import com.github.noconnor.junitperf.JUnitPerfTest;
import com.github.noconnor.junitperf.JUnitPerfTestRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @defgroup tests_rendimiento Tests de Rendimiento
 * JUnit 6 compatible performance test runner.
 *
 * JUnitPerf ≤1.25.0 calls ExtensionContext.getRequiredTestInstance() from background
 * threads, which fails in JUnit Jupiter 6.0.3 (Spring Boot 4.x) because the extension
 * context is not accessible from non-JUnit threads.
 *
 * This runner captures the test body as a Runnable (no JUnit context required), runs it
 * in the requested number of threads for the requested duration, collects the same
 * metrics as JUnitPerf, reads @JUnitPerfTest/@JUnitPerfTestRequirement from the calling
 * method via StackWalker, asserts the requirements, and generates an HTML report.
 */
public final class PerformanceTestRunner {

    private static final Logger logger = LogManager.getLogger(PerformanceTestRunner.class);
    private static final Path REPORT_PATH = Paths.get("target/junitperf/report.html");

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /**
     * Run a load test. Reads @JUnitPerfTest and @JUnitPerfTestRequirement from the
     * calling test method (via StackWalker) to get threads, duration, and requirements.
     */
    public static void assertPerformance(ThrowingRunnable testBody) {
        Method caller = findCallerMethod();
        JUnitPerfTest config = caller != null ? caller.getAnnotation(JUnitPerfTest.class) : null;
        JUnitPerfTestRequirement req = caller != null ? caller.getAnnotation(JUnitPerfTestRequirement.class) : null;

        int threads    = config != null ? config.threads()         : 10;
        int durationMs = config != null ? config.durationMs()      : 5_000;
        int warmUpMs   = config != null ? config.warmUpMs()        : 1_000;
        int rampUpMs   = config != null ? config.rampUpPeriodMs()  : 0;
        String testName = caller != null ? caller.getName() : "unknown";

        runLoadTest(testName, testBody, threads, durationMs, warmUpMs, rampUpMs, req);
    }

    // -------------------------------------------------------------------------
    // Internal execution logic
    // -------------------------------------------------------------------------

    private static void runLoadTest(String testName, ThrowingRunnable body,
                                    int threads, int durationMs, int warmUpMs, int rampUpMs,
                                    JUnitPerfTestRequirement req) {
        AtomicLong errorCount   = new AtomicLong();
        AtomicLong successCount = new AtomicLong();
        LongAdder  latencyNsSum = new LongAdder();
        List<Long> latencies    = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch     = new CountDownLatch(threads);

        long startMs     = System.currentTimeMillis();
        long warmUpEndMs = startMs + warmUpMs;
        long endMs       = startMs + durationMs;

        logger.info("[{}] Starting load test — {} threads, {}ms duration ({}ms warmup)",
                testName, threads, durationMs, warmUpMs);

        for (int t = 0; t < threads; t++) {
            final long rampDelay = rampUpMs > 0 ? (long) (rampUpMs * t / (double) threads) : 0;
            executor.submit(() -> {
                try {
                    if (rampDelay > 0) Thread.sleep(rampDelay);
                    while (System.currentTimeMillis() < endMs) {
                        long callStart = System.nanoTime();
                        boolean isError = false;
                        try {
                            body.run();
                        } catch (Throwable e) {
                            isError = true;
                        }
                        long elapsed = System.nanoTime() - callStart;
                        if (System.currentTimeMillis() > warmUpEndMs) {
                            if (isError) {
                                errorCount.incrementAndGet();
                            } else {
                                successCount.incrementAndGet();
                                latencyNsSum.add(elapsed);
                                latencies.add(elapsed);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();

        long total = successCount.get() + errorCount.get();
        double errorRate    = total > 0 ? errorCount.get() * 100.0 / total : 100.0;
        double meanLatency  = successCount.get() > 0
                ? latencyNsSum.sum() / 1_000_000.0 / successCount.get()
                : Double.MAX_VALUE;
        double measuredSec  = Math.max((durationMs - warmUpMs) / 1000.0, 1.0);
        double throughput   = successCount.get() / measuredSec;

        logger.info("[{}] Executions={} errors={} errorRate={}% meanLatency={}ms throughput={}/sec",
                testName, total, errorCount.get(),
                String.format("%.2f", errorRate),
                String.format("%.3f", meanLatency),
                String.format("%.1f", throughput));

        Collections.sort(latencies);
        double p95 = percentile(latencies, 95);
        double p99 = percentile(latencies, 99);

        writeReport(testName, total, successCount.get(), errorCount.get(), errorRate,
                meanLatency, p95, p99, throughput, req);

        // Assert requirements
        if (req != null) {
            List<String> failures = new ArrayList<>();
            if (errorRate > req.allowedErrorPercentage()) {
                failures.add(String.format("Error rate %.2f%% exceeds allowed %.2f%%",
                        errorRate, req.allowedErrorPercentage()));
            }
            if (req.meanLatency() > 0 && meanLatency > req.meanLatency()) {
                failures.add(String.format("Mean latency %.2fms exceeds allowed %dms",
                        meanLatency, (int) req.meanLatency()));
            }
            if (req.executionsPerSec() > 0 && throughput < req.executionsPerSec()) {
                failures.add(String.format("Throughput %.1f exec/sec below required %d exec/sec",
                        throughput, req.executionsPerSec()));
            }
            if (!failures.isEmpty()) {
                fail("[" + testName + "] Performance requirements not met:\n" +
                        String.join("\n", failures));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Report generation
    // -------------------------------------------------------------------------

    private static synchronized void writeReport(String testName, long total, long success,
                                                  long errors, double errorRate,
                                                  double meanMs, double p95Ms, double p99Ms,
                                                  double throughput, JUnitPerfTestRequirement req) {
        try {
            Files.createDirectories(REPORT_PATH.getParent());

            boolean newFile = !Files.exists(REPORT_PATH);
            StringBuilder sb = new StringBuilder();

            if (newFile) {
                sb.append("<!doctype html><html><head>")
                  .append("<meta charset='utf-8'>")
                  .append("<title>JUnit Performance Report</title>")
                  .append("<style>body{font-family:Verdana;} table{border-collapse:collapse;}")
                  .append("th,td{border:1px solid #ccc;padding:6px 10px;} ")
                  .append(".pass{background:#d4edda;} .fail{background:#f8d7da;}</style>")
                  .append("</head><body>")
                  .append("<h1 style='color:#044e9b'>JUnit Performance Report</h1><hr>");
            }

            boolean passed = req == null ||
                    (errorRate <= req.allowedErrorPercentage() &&
                     (req.meanLatency() <= 0 || meanMs <= req.meanLatency()) &&
                     (req.executionsPerSec() <= 0 || throughput >= req.executionsPerSec()));

            sb.append("<h2>").append(testName).append("</h2>");
            sb.append("<table>");
            sb.append("<tr><th>Metric</th><th>Actual</th><th>Required</th><th>Result</th></tr>");
            appendRow(sb, "Total executions", String.valueOf(total), "-", true);
            appendRow(sb, "Error rate",
                    String.format("%.2f%%", errorRate),
                    req != null ? String.format("≤ %.2f%%", req.allowedErrorPercentage()) : "-",
                    req == null || errorRate <= req.allowedErrorPercentage());
            appendRow(sb, "Mean latency (ms)",
                    String.format("%.3f", meanMs),
                    req != null && req.meanLatency() > 0 ? String.format("≤ %d", (int) req.meanLatency()) : "-",
                    req == null || req.meanLatency() <= 0 || meanMs <= req.meanLatency());
            appendRow(sb, "95th percentile (ms)", String.format("%.3f", p95Ms), "-", true);
            appendRow(sb, "99th percentile (ms)", String.format("%.3f", p99Ms), "-", true);
            appendRow(sb, "Throughput (exec/sec)",
                    String.format("%.1f", throughput),
                    req != null && req.executionsPerSec() > 0 ? String.format("≥ %d", req.executionsPerSec()) : "-",
                    req == null || req.executionsPerSec() <= 0 || throughput >= req.executionsPerSec());
            sb.append("<tr class='").append(passed ? "pass" : "fail")
              .append("'><td colspan='4'><b>").append(passed ? "PASSED" : "FAILED")
              .append("</b></td></tr>");
            sb.append("</table><br>");

            StandardOpenOption mode = newFile ? StandardOpenOption.CREATE_NEW : StandardOpenOption.APPEND;
            Files.write(REPORT_PATH, sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE, mode);
        } catch (IOException e) {
            logger.warn("Could not write performance report: {}", e.getMessage());
        }
    }

    private static void appendRow(StringBuilder sb, String metric, String actual,
                                   String required, boolean ok) {
        sb.append("<tr class='").append(ok ? "pass" : "fail").append("'>")
          .append("<td>").append(metric).append("</td>")
          .append("<td>").append(actual).append("</td>")
          .append("<td>").append(required).append("</td>")
          .append("<td>").append(ok ? "✓" : "✗").append("</td></tr>");
    }

    private static double percentile(List<Long> sorted, int pct) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        idx = Math.min(Math.max(idx, 0), sorted.size() - 1);
        return sorted.get(idx) / 1_000_000.0;
    }

    // -------------------------------------------------------------------------
    // StackWalker to find the annotated test method
    // -------------------------------------------------------------------------

    private static Method findCallerMethod() {
        try {
            return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(frames -> frames
                            .filter(f -> f.getDeclaringClass() != PerformanceTestRunner.class)
                            .findFirst()
                            .map(frame -> {
                                String name = frame.getMethodName();
                                return Arrays.stream(frame.getDeclaringClass().getDeclaredMethods())
                                        .filter(m -> m.getName().equals(name))
                                        .findFirst()
                                        .orElse(null);
                            })
                            .orElse(null));
        } catch (Exception e) {
            return null;
        }
    }
}
