package net.minecraft.server.dedicated;

import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportType;
import net.minecraft.SystemUtils;
import net.minecraft.server.DispenserRegistry;
import net.minecraft.util.TimeRange;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class ThreadWatchdog implements Runnable {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MAX_SHUTDOWN_TIME = 10000L;
    private static final int SHUTDOWN_STATUS = 1;
    private final DedicatedServer server;
    private final long maxTickTimeNanos;

    public ThreadWatchdog(DedicatedServer dedicatedserver) {
        this.server = dedicatedserver;
        this.maxTickTimeNanos = dedicatedserver.getMaxTickLength() * TimeRange.NANOSECONDS_PER_MILLISECOND;
    }

    public void run() {
        while (this.server.isRunning()) {
            long i = this.server.getNextTickTime();
            long j = SystemUtils.getNanos();
            long k = j - i;

            if (k > this.maxTickTimeNanos) {
                ThreadWatchdog.LOGGER.error(LogUtils.FATAL_MARKER, "A single server tick took {} seconds (should be max {})", String.format(Locale.ROOT, "%.2f", (float) k / (float) TimeRange.NANOSECONDS_PER_SECOND), String.format(Locale.ROOT, "%.2f", this.server.tickRateManager().millisecondsPerTick() / (float) TimeRange.MILLISECONDS_PER_SECOND));
                ThreadWatchdog.LOGGER.error(LogUtils.FATAL_MARKER, "Considering it to be crashed, server will forcibly shutdown.");
                CrashReport crashreport = createWatchdogCrashReport("Watching Server", this.server.getRunningThread().threadId());

                this.server.fillSystemReport(crashreport.getSystemReport());
                CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Performance stats");

                crashreportsystemdetails.setDetail("Random tick rate", () -> {
                    return ((GameRules.GameRuleInt) this.server.getWorldData().getGameRules().getRule(GameRules.RULE_RANDOMTICKING)).toString();
                });
                crashreportsystemdetails.setDetail("Level stats", () -> {
                    return (String) Streams.stream(this.server.getAllLevels()).map((worldserver) -> {
                        String s = String.valueOf(worldserver.dimension().location());

                        return s + ": " + worldserver.getWatchdogStats();
                    }).collect(Collectors.joining(",\n"));
                });
                DispenserRegistry.realStdoutPrintln("Crash report:\n" + crashreport.getFriendlyReport(ReportType.CRASH));
                Path path = this.server.getServerDirectory().resolve("crash-reports").resolve("crash-" + SystemUtils.getFilenameFormattedDateTime() + "-server.txt");

                if (crashreport.saveToFile(path, ReportType.CRASH)) {
                    ThreadWatchdog.LOGGER.error("This crash report has been saved to: {}", path.toAbsolutePath());
                } else {
                    ThreadWatchdog.LOGGER.error("We were unable to save this crash report to disk.");
                }

                this.exit();
            }

            try {
                Thread.sleep((i + this.maxTickTimeNanos - j) / TimeRange.NANOSECONDS_PER_MILLISECOND);
            } catch (InterruptedException interruptedexception) {
                ;
            }
        }

    }

    public static CrashReport createWatchdogCrashReport(String s, long i) {
        ThreadMXBean threadmxbean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] athreadinfo = threadmxbean.dumpAllThreads(true, true);
        StringBuilder stringbuilder = new StringBuilder();
        Error error = new Error("Watchdog");

        for (ThreadInfo threadinfo : athreadinfo) {
            if (threadinfo.getThreadId() == i) {
                error.setStackTrace(threadinfo.getStackTrace());
            }

            stringbuilder.append(threadinfo);
            stringbuilder.append("\n");
        }

        CrashReport crashreport = new CrashReport(s, error);
        CrashReportSystemDetails crashreportsystemdetails = crashreport.addCategory("Thread Dump");

        crashreportsystemdetails.setDetail("Threads", stringbuilder);
        return crashreport;
    }

    private void exit() {
        try {
            Timer timer = new Timer();

            timer.schedule(new TimerTask() {
                public void run() {
                    Runtime.getRuntime().halt(1);
                }
            }, 10000L);
            System.exit(1);
        } catch (Throwable throwable) {
            Runtime.getRuntime().halt(1);
        }

    }
}
