package disruptor.practice.oms.monitoring;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

import java.util.Map;

public final class HdrLatencyRecorder {


    // Choose a range. Your current numbers: up to hundreds of ms.
// Use something generous to avoid clamping:
// 10 seconds in ns is 10_000_000_000
    private static final long HIGHEST_TRACKABLE_NS = 10_000_000_000L;
    private static final int SIGNIFICANT_DIGITS = 3;

    private final Recorder e2eRec = new Recorder(HIGHEST_TRACKABLE_NS, SIGNIFICANT_DIGITS);
    private final Recorder svcRec = new Recorder(HIGHEST_TRACKABLE_NS, SIGNIFICANT_DIGITS);
    private final Recorder qRec = new Recorder(HIGHEST_TRACKABLE_NS, SIGNIFICANT_DIGITS);
    private final Recorder execQRec = new Recorder(HIGHEST_TRACKABLE_NS, SIGNIFICANT_DIGITS);

    // Optional: counters for how many samples recorded in this interval.
// (Histogram has totalCount too, but this is convenient.)
    public void recordE2e(long ns) {
        record(e2eRec, ns);
    }

    public void recordSvc(long ns) {
        record(svcRec, ns);
    }

    public void recordQ(long ns) {
        record(qRec, ns);
    }

    public void recordExecQ(long ns) {
        record(execQRec, ns);
    }

    private static void record(Recorder r, long ns) {
        if (ns <= 0) ns = 1;
        // If you ever exceed highest trackable, either:
        // - increase HIGHEST_TRACKABLE_NS
        // - or clamp
        if (ns > HIGHEST_TRACKABLE_NS) ns = HIGHEST_TRACKABLE_NS;
        r.recordValue(ns);
    }

    public LatencySnapshot snapshotInterval() {
        Histogram e2e = e2eRec.getIntervalHistogram();
        Histogram svc = svcRec.getIntervalHistogram();
        Histogram q = qRec.getIntervalHistogram();
        Histogram execQ = execQRec.getIntervalHistogram();

        Map<String, long[]> stats = Map.of(
                "e2e", toPct(e2e),
                "svc", toPct(svc),
                "q", toPct(q),
                "execQ", toPct(execQ)
        );

        // Pick a single 'n' for display. If your pipeline always records all 4 per sample,
        // these will be equal. Otherwise, you can store per-metric counts.
        long n = e2e.getTotalCount();

        return new LatencySnapshot((int) n, stats);
    }

    private static long[] toPct(Histogram h) {
        return new long[]{
                h.getValueAtPercentile(50.0),
                h.getValueAtPercentile(95.0),
                h.getValueAtPercentile(99.0),
                h.getMaxValue()
        };
    }

    // Match your existing snapshot type (or replace with your own)
    public static final class LatencySnapshot {
        private final int n;
        private final Map<String, long[]> stats;

        public LatencySnapshot(int n, Map<String, long[]> stats) {
            this.n = n;
            this.stats = stats;
        }

        public int n() {
            return n;
        }

        public Map<String, long[]> stats() {
            return stats;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (var e : stats.entrySet()) {
                long[] v = e.getValue();
                sb.append(e.getKey())
                        .append("->p50:").append(v[0]).append(" ns, ")
                        .append("p95:").append(v[1]).append(" ns, ")
                        .append("p99:").append(v[2]).append(" ns, ")
                        .append("max:").append(v[3]).append(" ns\n");
            }
            sb.append("sample n = ").append(n);
            return sb.toString();
        }
    }
}