package disruptor.practice.oms.monitoring;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LatencyRecorder {
    private final int CAP = 262_144;

    private final long[] e2e = new long[CAP];
    private final long[] q   = new long[CAP];
    private final long[] svc = new long[CAP];
    private final long[] execQ = new long[CAP];

    AtomicInteger idx = new AtomicInteger(0);
    public void record(long e2eNs, long qNs, long svcNs, long execQNs) {
        int i = idx.getAndIncrement() & (CAP - 1);
        e2e[i] = e2eNs;
        q[i]   = qNs;
        svc[i] = svcNs;
        execQ[i] = execQNs;
    }

    /**
     *
     * @return Map
     * e.g.
     * "e2e": long[]{p50, p95, p99, max}
     * "svc": long[]{p50, p95, p99, max},
     */
    public LatencySnapshot snapshot() {
        int n = idx.get();                // simple version assumes no wrap
        long[] snapE2e = computeSnapshotForVersion(n, e2e);
        long[] snapSvc = computeSnapshotForVersion(n, svc);
        long[] snapQ = computeSnapshotForVersion(n, q);
        long[] snapExecQ = computeSnapshotForVersion(n, execQ);
        Map<String, long[]> stats = Map.of("e2e", snapE2e, "svc", snapSvc, "q", snapQ, "execQ", snapExecQ);
        return new LatencySnapshot(n, stats);
    }

    /**
     * copy first n into temp arrays
     * long[] res: 0: p50, 1:p95, 2:p99, 3:max
     * sort temp arrays
     * compute p50/p95/p99/max
     * @param n
     * @param original
     * @return
     */
    private long[] computeSnapshotForVersion(int n, long[] original) {
        long[] temp = Arrays.copyOf(original, n + 1);
        Arrays.sort(temp);
        // p50
        long[] res = new long[4];
        res[0] = temp[n / 2];
        // p95
        double index = (double)n * 0.95;
        res[1] = temp[(int)Math.round(index)];
        // p99
        index = (double)n * 0.99;
        res[2] = temp[(int)Math.round(index)];
        //max, last one in an ascending order
        res[3] = temp[n];
        return res;
    }

    public record LatencySnapshot(long n, Map<String, long[]> stats){

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            for (var ent : stats.entrySet()) {
                res.append(ent.getKey());
                long[] values = ent.getValue();
                String statistics = String.format("p50:%d ns, p95:%d ns, p99:%d ns, max:%d ns", values[0], values[1],
                        values[2], values[3]);
                res.append("->");
                res.append(statistics);
                res.append("\n");
            }
            res.append(String.format("sample n = %d", n));
            return res.toString();

        }
    }

}
