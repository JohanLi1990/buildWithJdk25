Perfect — here is a **tight, high-impact 7-day plan** designed exactly like your previous Netty practice (1 hour/day, hands-on, progressive difficulty).
This plan **builds toward a working Netty → Disruptor mini-OMS pipeline** by Day 7.

No code yet — only clear tasks & learning objectives.

---

# 🔥 **7-Day Netty + Disruptor Training Plan**

**Goal:** Build intuition + muscle memory for low-latency event pipelines.

---

# **📅 Day 1 — Disruptor Fundamentals (Mental Model + Minimal Pipeline)**

### Concepts to master

* What is a RingBuffer
* Sequencer
* Event publication
* Consumer dependency graph

### Practical tasks

* Create a simple Disruptor with:

    * 1 producer thread
    * 1 consumer thread
* Event = single long ID
* Producer publishes 1M events
* Consumer just counts/prints totals

### Learning outcomes

* Feel the sequence progression
* Understand single-writer principle
* See how insanely fast Disruptor moves

---

# **📅 Day 2 — Multi-Consumer + Event Flow Control**

### Concepts

* Fan-out consumers
* Pipeline consumers
* Sequence barriers
* Backpressure inside Disruptor

### Practical tasks

* Extend yesterday’s code to:

    * Consumer A (validation)
    * Consumer B (enrichment) → depends on A
    * Consumer C (logging) → independent

### Learning outcomes

* How Disruptor avoids locks
* How dependency graph controls throughput
* Visualize how backpressure stalls producer

---

# **📅 Day 3 — Netty Refresher (I/O Threading + Backpressure)**

### Concepts

* I/O threads (boss/worker)
* `channelRead` vs pipeline propagation
* Memory management via `ByteBuf`
* Writability & backpressure

### Practical tasks

* Build a tiny Netty TCP server:

    * Accept raw bytes
    * Echo back
    * Log thread names

### Learning outcomes

* Map Netty event loop model clearly
* Understand worker thread behavior
* Feel how backpressure triggers in logs

---

# **📅 Day 4 — Netty → Disruptor Handoff (The Core Skill)**

### Concepts

* Thread handoff costs
* Avoiding shared mutable state
* When to block, when not to block
* Passing messages into ring buffer

### Practical tasks

* Modify your Netty server:

    * `channelRead` publishes event into Disruptor
    * Disruptor consumer processes
    * Netty writes response back

### Learning outcomes

* The most important pattern in OMS gateways:

  **I/O thread → business pipeline → I/O thread**

* How handoff affects latency

* Clean separation between layers

---

# **📅 Day 5 — Build a Mini OMS Pipeline in Disruptor**

### Concepts

* Multi-stage pipeline modeling
* Stateless vs stateful handlers
* Zero-allocation strategies

### Practical tasks

* Replace simple consumers with:

    * Stage 1: Parse order (mock)
    * Stage 2: Risk check (mock)
    * Stage 3: Execution simulation
    * Stage 4: Prepare response

### Learning outcomes

* How to architect real OMS engines
* Where latency enters
* How you can scale stages differently

---

# **📅 Day 6 — Latency Measurement + GC Avoidance**

### Concepts

* p50, p95, p99 tail latency
* Allocation rate impact
* How GC pauses look
* Microbenchmarking pitfalls

### Practical tasks

* Instrument your pipeline:

    * capture timestamps at:

        * Netty receive
        * Disruptor entry
        * Disruptor exit
        * Netty write
    * calculate latencies
* Add an artificial allocation hotspot → observe p99 blowing up
* Add reuse (object pool or reusing event) → measure improvement

### Learning outcomes

* Real, practical latency mindset
* Why GC matters so much in trading
* How allocation shows up in p99

---

# **📅 Day 7 — Final Integration + Stability Testing**

### Concepts

* Throughput vs latency
* Backpressure tuning
* Threading bottlenecks
* Saturation behavior

### Practical tasks

* Run a benchmark:

    * Send 100k or 1M requests via your Netty gateway
    * Measure end-to-end latency distribution
* Stress-test:

    * Slow consumer simulation
    * Burst traffic
    * Ring buffer full scenario
* Document:

    * Bottlenecks
    * Observed behavior
    * How you solved backpressure

### Learning outcomes

* Recognize real-world trading engine failure modes
* Understand ring buffer saturation behavior
* Build confidence that you can debug & tune pipelines

---

Absolutely — here’s a **2-day, 4–5h** plan that turns your project into a **measurable low-latency lab** without turning it into a benchmarking rabbit hole.

I’ll keep it “get hands dirty”, with **small steps + expected outputs** so you can validate quickly.

---

# 2-Day Plan: Measurement → Tail Latency → GC/Alloc Insight → One Fix

## Guiding rule (important)

**No more optimizations** until you can answer:

* “What is my p50/p95/p99 end-to-end?”
* “Is the tail from queueing or from GC?”

---

# Day A (2–2.5h): Build a trustworthy latency harness

## A1) Define 3 scenarios (10 min)

You will run these repeatedly:

### Scenario 1 — Baseline steady

* 1 family, constant rate (no burst)
* Goal: establish stable p50/p99

### Scenario 2 — Burst (your current one)

* 1 family, 39k burst
* Goal: see queueing tail + drain behavior

### Scenario 3 — Hot + cold (fairness test)

* Hot family high rate
* 20–100 cold families low rate
* Goal: detect starvation and justify Track A later (slice)

✅ **Expected**: Scenario 2 shows huge queueing tail; Scenario 3 shows cold p99 blow-ups if you don’t enforce fairness.

---

## A2) Add timestamp hooks (30–40 min)

Add 4–5 `long` fields to your event (or sidecar) — **nanoTime** only:

* `t0_nettyRead`
* `t1_disruptorEnq` (or “scheduler enq”)
* `t2_workStart`
* `t3_workEnd`
* `t4_nettyWrite` (optional if you can reliably mark “write scheduled”)

From these compute:

* **Queue latency**: `t2 - t1`
* **Service time**: `t3 - t2`
* **E2E**: `t3 - t0` (or `t4 - t0` if you can)

✅ **Expected**: You can print one sample debug line occasionally (1 per 10k) to confirm deltas look sane (e.g., service ≈ 10–20ms).

**Pitfall to avoid**: don’t log per event.

---

## A3) Build a lightweight latency recorder (40–50 min)

Make a per-partition recorder:

* `long[] e2eNanos = new long[CAP]`
* `long[] qNanos = new long[CAP]`
* `long[] svcNanos = new long[CAP]`
* `int idx` (wrap around)

On completion, record into arrays (constant-time).

Every 1s (or 5s), snapshot:

* copy the filled portion into a temp array
* `Arrays.sort`
* compute p50/p95/p99/max

✅ **Expected**: You get logs like:

```
P3 e2e p50=... p95=... p99=...
P3 q   p50=... p95=... p99=...
P3 svc p50=... p95=... p99=...
samples=...
```

**CAP sizing**:

* start with 200_000 samples (fits memory, good resolution)

**Important**: sorting allocates / costs CPU — that’s fine for now; do it every **5s** to reduce overhead.

---

## A4) Add “sanity counters” for backlog correlation (10–15 min)

Keep your existing counters (pendMaxNow, done/s). Now you can correlate:

* If `q p99` is huge while `svc p99` is stable → tail is **queueing**
* If `svc p99` spikes → tail is **work/GC**

✅ **Expected**: In burst scenario, q dominates.

---

## A5) Run the 3 scenarios and capture a baseline (20–30 min)

Save a small note:

* scenario → p50/p99 e2e, q, svc
* done/s
* pendMaxNow

This is your “before”.

**Scenario 1: Baseline (steady 50msg/s, no burst)**
```avroidl
Scenario S1 (1 family, 50 msg/s, 6000 msgs)

e2e p50 ~15.4ms, p99 ~16.5ms

q p50 ~0.12ms, p99 ~0.30ms

execQ p50 ~0.12ms, p99 ~0.29ms

svc p50 ~15.1ms, p99 ~16.1ms
```

**Scenario 2: burst 6k, 1 family**
```avroidl
svc stable ~15–16ms
        
execQ tiny

q dominates, grows to ~90s tail

matches N * svc drain time
```
**Scenario 3 Result (core = 4, mixing hot and cold family)**
```avroidl

HOT remains low latency and stable.

COLD experiences about +1 service time in p95/p99 due to executor contention, not pending starvation.

No backlog (pendingMax=0), so fairness is acceptable in this load regime.
```

---

# Day B (2–2.5h): GC/Allocation learning + one measurable improvement

## B1) Turn on GC logging (10 min)

Run with (JDK 11/17+):

```
-Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags
```

✅ **Expected**: You’ll see minor GCs, pause times, and frequency.

---

## B2) Create an intentional allocation hotspot (20–30 min)

In the *work* section (or a controlled toggle), add something like:

* allocate `byte[1024]` per task, or
* build a `String`, or
* create a small `HashMap`

Do it behind a flag: `ALLOC_HOTSPOT=true`.

Run Scenario 1 (steady) for ~30–60s.

✅ **Expected**:

* Allocation rate increases
* GC frequency increases
* **svc p99** starts wobbling
* e2e p99 increases even if queueing is low

This is the “why GC matters” moment.

### Setup
- Client, 300 msg/s , corrId = 11, (60k msg total)
- Server, 1ms/task (in execSimulation), logging every 1s

#### Base line (ALLOC_HOTSPOT = false)
**Latency Logs**
```avroidl
15:06:34.069 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
execQ->p50:47400 ns, p95:173500 ns, p99:270400 ns, max:3024600 ns
e2e->p50:1584200 ns, p95:30173800 ns, p99:36510400 ns, max:37617600 ns
svc->p50:1119900 ns, p95:2045200 ns, p99:2187500 ns, max:4146800 ns
q->p50:96100 ns, p95:24415100 ns, p99:32603200 ns, max:34350600 ns
sample n = 116
15:06:34.069 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)

15:07:40.070 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
execQ->p50:44900 ns, p95:161100 ns, p99:203900 ns, max:3024600 ns
e2e->p50:1312400 ns, p95:1944100 ns, p99:2272200 ns, max:37617600 ns
svc->p50:1126200 ns, p95:1859800 ns, p99:2033600 ns, max:10747000 ns
q->p50:46200 ns, p95:163300 ns, p99:210000 ns, max:34350600 ns
sample n = 17154
15:07:40.070 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)

15:08:30.072 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
execQ->p50:41600 ns, p95:157200 ns, p99:201800 ns, max:3024600 ns
e2e->p50:1328300 ns, p95:1948800 ns, p99:2240400 ns, max:37617600 ns
svc->p50:1140900 ns, p95:1872000 ns, p99:2034000 ns, max:10747000 ns
q->p50:42400 ns, p95:159000 ns, p99:205800 ns, max:34350600 ns
sample n = 30121
15:08:30.072 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=1 q=0)

15:10:26.079 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
execQ->p50:58500 ns, p95:165700 ns, p99:209500 ns, max:3024600 ns
e2e->p50:1356900 ns, p95:1944700 ns, p99:2218100 ns, max:37617600 ns
svc->p50:1163700 ns, p95:1865100 ns, p99:2032400 ns, max:10747000 ns
q->p50:59000 ns, p95:166500 ns, p99:212800 ns, max:34350600 ns
sample n = 60000
15:10:26.080 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)

```
GC log
```avroidl
[2026-01-11T15:06:23.655+0800][0.011s][info][gc,init] CardTable entry size: 512
[2026-01-11T15:06:23.655+0800][0.011s][info][gc     ] Using G1
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] Version: 25+37-LTS-3491 (release)
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] CPUs: 12 total, 12 available
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] Memory: 16068M
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] Large Page Support: Disabled
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] NUMA Support: Disabled
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] Compressed Oops: Enabled (32-bit)
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] Heap Region Size: 1M
[2026-01-11T15:06:23.657+0800][0.014s][info][gc,init] Heap Min Capacity: 512M
[2026-01-11T15:06:23.658+0800][0.014s][info][gc,init] Heap Initial Capacity: 512M
[2026-01-11T15:06:23.658+0800][0.014s][info][gc,init] Heap Max Capacity: 512M
[2026-01-11T15:06:23.658+0800][0.014s][info][gc,init] Pre-touch: Disabled
[2026-01-11T15:06:23.658+0800][0.014s][info][gc,init] Parallel Workers: 10
[2026-01-11T15:06:23.658+0800][0.014s][info][gc,init] Concurrent Workers: 3
[2026-01-11T15:06:23.658+0800][0.014s][info][gc,init] Concurrent Refinement Workers: 10
[2026-01-11T15:06:23.658+0800][0.014s][info][gc,init] Periodic GC: Disabled
[2026-01-11T15:06:23.667+0800][0.024s][info][gc,metaspace] CDS archive(s) mapped at: [0x0000000035000000-0x0000000035dd0000-0x0000000035dd0000), size 14483456, SharedBaseAddress: 0x0000000035000000, ArchiveRelocationMode: 1.
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] Compressed class space mapped at: 0x0000000036000000-0x0000000076000000, reserved size: 1073741824
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] UseCompressedClassPointers 1, UseCompactObjectHeaders 0
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] Narrow klass pointer bits 32, Max shift 3
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] Narrow klass base: 0x0000000035000000, Narrow klass shift: 0
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] Encoding Range: [0x0000000035000000 - 0x0000000135000000), (4294967296 bytes)
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] Klass Range:    [0x0000000035010000 - 0x0000000076000000), (1090453504 bytes)
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] Klass ID Range:  [65536 - 1090519033) (1090453497)
[2026-01-11T15:06:23.668+0800][0.024s][info][gc,metaspace] Protection zone: [0x0000000035000000 - 0x0000000035010000), (65536 bytes)
[2026-01-11T15:06:49.084+0800][25.441s][info][gc,start    ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[2026-01-11T15:06:49.084+0800][25.441s][info][gc,task     ] GC(0) Using 10 workers of 10 for evacuation
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,phases   ] GC(0)   Pre Evacuate Collection Set: 0.28ms
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,phases   ] GC(0)   Merge Heap Roots: 0.11ms
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,phases   ] GC(0)   Evacuate Collection Set: 6.06ms
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,phases   ] GC(0)   Post Evacuate Collection Set: 1.25ms
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,phases   ] GC(0)   Other: 0.42ms
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,heap     ] GC(0) Eden regions: 47->0(186)
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,heap     ] GC(0) Survivor regions: 0->6(6)
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,heap     ] GC(0) Old regions: 2->3
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,heap     ] GC(0) Humongous regions: 26->25
[2026-01-11T15:06:49.094+0800][25.451s][info][gc,metaspace] GC(0) Metaspace: 12478K(12800K)->12478K(12800K) NonClass: 11283K(11456K)->11283K(11456K) Class: 1195K(1344K)->1195K(1344K)
[2026-01-11T15:06:49.094+0800][25.452s][info][gc          ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 74M->32M(512M) 10.627ms
[2026-01-11T15:06:49.094+0800][25.452s][info][gc,cpu      ] GC(0) User=0.09s Sys=0.06s Real=0.01s
[2026-01-11T15:06:49.094+0800][25.452s][info][safepoint   ] Safepoint "G1CollectForAllocation", Time since last: 25413892500 ns, Reaching safepoint: 12200 ns, At safepoint: 10678400 ns, Leaving safepoint: 10300 ns, Total: 10700900 ns, Threads: 0 runnable, 20 total
[2026-01-11T15:10:17.436+0800][233.793s][info][gc,start    ] GC(1) Pause Young (Normal) (G1 Evacuation Pause)
[2026-01-11T15:10:17.436+0800][233.793s][info][gc,task     ] GC(1) Using 10 workers of 10 for evacuation
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,phases   ] GC(1)   Pre Evacuate Collection Set: 0.40ms
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,phases   ] GC(1)   Merge Heap Roots: 0.18ms
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,phases   ] GC(1)   Evacuate Collection Set: 5.03ms
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,phases   ] GC(1)   Post Evacuate Collection Set: 1.79ms
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,phases   ] GC(1)   Other: 0.11ms
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,heap     ] GC(1) Eden regions: 186->0(272)
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,heap     ] GC(1) Survivor regions: 6->7(24)
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,heap     ] GC(1) Old regions: 3->3
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,heap     ] GC(1) Humongous regions: 25->25
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,metaspace] GC(1) Metaspace: 12567K(12864K)->12567K(12864K) NonClass: 11370K(11520K)->11370K(11520K) Class: 1196K(1344K)->1196K(1344K)
[2026-01-11T15:10:17.444+0800][233.801s][info][gc          ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 218M->33M(512M) 7.735ms
[2026-01-11T15:10:17.444+0800][233.801s][info][gc,cpu      ] GC(1) User=0.00s Sys=0.00s Real=0.01s
[2026-01-11T15:10:17.444+0800][233.801s][info][safepoint   ] Safepoint "G1CollectForAllocation", Time since last: 208341341200 ns, Reaching safepoint: 5500 ns, At safepoint: 7775800 ns, Leaving safepoint: 5700 ns, Total: 7787000 ns, Threads: 0 runnable, 23 total

```

### ALLOC_HOTSPOT == True (no mitigation)

Latency log:
```avroidl
15:36:10.826 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
execQ->p50:51700 ns, p95:153900 ns, p99:2508100 ns, max:2508100 ns
q->p50:10264600 ns, p95:30244700 ns, p99:32921000 ns, max:32921000 ns
svc->p50:1759800 ns, p95:2091800 ns, p99:9535800 ns, max:9535800 ns
e2e->p50:17366300 ns, p95:33700300 ns, p99:36433700 ns, max:36433700 ns
sample n = 39
15:36:10.826 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)

15:37:40.830 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
execQ->p50:55800 ns, p95:155000 ns, p99:199900 ns, max:4872200 ns
q->p50:58700 ns, p95:162900 ns, p99:837200 ns, max:82683300 ns
svc->p50:1184500 ns, p95:1913800 ns, p99:2151900 ns, max:48687800 ns
e2e->p50:1375200 ns, p95:2028300 ns, p99:4838300 ns, max:87293700 ns
sample n = 23251
15:37:40.830 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=1 q=0)

15:40:02.837 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
execQ->p50:55400 ns, p95:160900 ns, p99:207600 ns, max:26739100 ns
q->p50:58500 ns, p95:167800 ns, p99:453100 ns, max:82683300 ns
svc->p50:1195700 ns, p95:1914700 ns, p99:2141300 ns, max:48687800 ns
e2e->p50:1388900 ns, p95:2016200 ns, p99:3971000 ns, max:87293700 ns
sample n = 60000
15:40:02.838 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)
```

GC Logs:
```avroidl
67 GC logs, not pasting it here..
```

---

## B3) Remove/mitigate allocations (40–60 min)

Pick **one** mitigation (keep it simple):

### Option 1 — Reuse event objects (already mostly true with Disruptor)

* Ensure you are not creating new per-task objects in the hot path

### Option 2 — Preallocate buffers (thread-local or per-family)

* Replace `new byte[]` with reuse from `ThreadLocal<byte[]>` (for learning)

### Option 3 — Coalesce (domain-realistic)

* if tasks are “time-based routing” / “price requests”, keep last-one-wins

✅ **Expected**:

* GC frequency drops
* svc p99 stabilizes
* e2e p99 improves measurably (even in steady load)

---

### Option 2; ALLOC_MITIGATION == True

latency log
```avroidl
16:18:57.249 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
e2e->p50:1345100 ns, p95:1943000 ns, p99:2336800 ns, max:30190800 ns
svc->p50:1144900 ns, p95:1849500 ns, p99:2073200 ns, max:7247700 ns
q->p50:77000 ns, p95:175900 ns, p99:221800 ns, max:26687200 ns
execQ->p50:76200 ns, p95:173500 ns, p99:214500 ns, max:1637400 ns
sample n = 11006
16:18:57.250 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)

16:20:27.254 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
e2e->p50:1364400 ns, p95:1990100 ns, p99:6857300 ns, max:69027800 ns
svc->p50:1160100 ns, p95:1879300 ns, p99:2113600 ns, max:68928600 ns
q->p50:67300 ns, p95:176500 ns, p99:2052600 ns, max:41186600 ns
execQ->p50:65000 ns, p95:167200 ns, p99:213700 ns, max:20129400 ns
sample n = 34263
16:20:27.254 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)

16:22:07.260 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
e2e->p50:1373300 ns, p95:1958300 ns, p99:2806300 ns, max:69027800 ns
svc->p50:1169300 ns, p95:1853300 ns, p99:2078000 ns, max:68928600 ns
q->p50:74700 ns, p95:176300 ns, p99:242800 ns, max:41186600 ns
execQ->p50:73300 ns, p95:171600 ns, p99:217500 ns, max:20129400 ns
sample n = 60000
16:22:07.260 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax0 tpe(pool=4 active=0 q=0)
```
GC logs: Only 3 GC events

## B4) Now apply **one** structural protection (30–45 min)

Given your burst evidence, choose:

### Track B (recommended): per-family pending cap

* add `pendingLimit`
* add explicit counter `rejOverLimit`
* show `pendMaxNow` plateau

✅ **Expected**:

* Burst scenario no longer produces 39k queue depth
* e2e p99 becomes bounded (still high, but capped)
* other families unaffected

(Track A fairness can be next week; do it only after you have scenario 3 numbers.)

### B4 Outcome
- Client: 
  - corrId=11, 2000msg/s (via eventloop), 120K msgs
  - corrId=1003,1011,1019,1027, 1035 @ 5 msg/s (total), 600 msgs
  - Duration: roughly 120s
  - Server config: pendingLimit = 1024

latency log:
```avroidl
18:37:52.320 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
e2e->p50:1564001200 ns, p95:1600860100 ns, p99:1607399100 ns, max:1616188600 ns
svc->p50:1775800 ns, p95:2093400 ns, p99:2160700 ns, max:7602900 ns
q->p50:1562426400 ns, p95:1599323500 ns, p99:1605690100 ns, max:1614085900 ns
execQ->p50:9600 ns, p95:37500 ns, p99:64600 ns, max:2341300 ns
sample n = 11002
---------
COLD
e2e->p50:1443600 ns, p95:2963200 ns, p99:3071400 ns, max:3407200 ns
svc->p50:1007700 ns, p95:2039200 ns, p99:2068000 ns, max:2082200 ns
q->p50:142300 ns, p95:1814700 ns, p99:2023700 ns, max:2062000 ns
execQ->p50:115300 ns, p95:1814600 ns, p99:2023500 ns, max:2061800 ns
sample n = 80
18:37:52.320 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax=1024 rejectOverLimit=20155 tpe(pool=4 active=1 q=0)
...
18:37:57.324 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
e2e->p50:1561196300 ns, p95:1599668500 ns, p99:1605247100 ns, max:1616188600 ns
svc->p50:1784100 ns, p95:2091900 ns, p99:2155400 ns, max:7602900 ns
q->p50:1559603100 ns, p95:1598052600 ns, p99:1603649600 ns, max:1614085900 ns
execQ->p50:9400 ns, p95:34000 ns, p99:60400 ns, max:2341300 ns
sample n = 14310
---------
COLD
e2e->p50:1443600 ns, p95:2992500 ns, p99:3350600 ns, max:3407200 ns
svc->p50:1007700 ns, p95:2040100 ns, p99:2080600 ns, max:2082200 ns
q->p50:142400 ns, p95:1750400 ns, p99:2023700 ns, max:2062000 ns
execQ->p50:130600 ns, p95:1750300 ns, p99:2023500 ns, max:2061800 ns
sample n = 105
18:37:57.325 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax=1024 rejectOverLimit=26504 tpe(pool=4 active=1 q=0)
...
18:38:42.325 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
e2e->p50:1550250300 ns, p95:1590166900 ns, p99:1601716800 ns, max:1616188600 ns
svc->p50:1751800 ns, p95:2086900 ns, p99:2150500 ns, max:12897900 ns
q->p50:1548777400 ns, p95:1588589100 ns, p99:1600206700 ns, max:1614085900 ns
execQ->p50:9200 ns, p95:26900 ns, p99:48800 ns, max:2341300 ns
sample n = 42312
---------
COLD
e2e->p50:1910400 ns, p95:3350600 ns, p99:4005800 ns, max:4200400 ns
svc->p50:1213600 ns, p95:2068300 ns, p99:2149900 ns, max:2754900 ns
q->p50:157900 ns, p95:1873700 ns, p99:2057400 ns, max:2128200 ns
execQ->p50:156900 ns, p95:1873600 ns, p99:2057300 ns, max:2128100 ns
sample n = 330
18:38:42.326 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: pendingMax=0 rejectOverLimit=77688 tpe(pool=4 active=0 q=0)
```

GC log
```avroidl
2 GC events, largest 6.6ms
```
---

### With latency budge 50ms, let pendingLimit be 32 instead of 1024 which is too large

latency logs
```avroidl

20:00:05.716 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3: 
HOT
execQ->p50:12800 ns, p95:31000 ns, p99:56600 ns, max:2854100 ns
q->p50:48175900 ns, p95:52784000 ns, p99:54816800 ns, max:58148100 ns
svc->p50:1808500 ns, p95:2048400 ns, p99:2117100 ns, max:8577900 ns
e2e->p50:49784100 ns, p95:54535600 ns, p99:56632500 ns, max:60020200 ns
sample n = 10832
---------
COLD
execQ->p50:89900 ns, p95:210100 ns, p99:222700 ns, max:250700 ns
q->p50:90600 ns, p95:210200 ns, p99:222900 ns, max:250800 ns
svc->p50:1086900 ns, p95:2006900 ns, p99:2059700 ns, max:2063200 ns
e2e->p50:1240200 ns, p95:2187800 ns, p99:2347900 ns, max:2349300 ns
sample n = 80
---------
pendingMaxSoFar=32 rejectOverLimit=20695 tpe(pool=6 active=1 q=0)
...
20:00:30.718 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
execQ->p50:10700 ns, p95:26200 ns, p99:43000 ns, max:2854100 ns
q->p50:48582600 ns, p95:52928300 ns, p99:54822200 ns, max:58148100 ns
svc->p50:1834900 ns, p95:2057200 ns, p99:2124200 ns, max:8577900 ns
e2e->p50:49944400 ns, p95:54741400 ns, p99:56630700 ns, max:60020200 ns
sample n = 27195
---------
COLD
execQ->p50:100900 ns, p95:274700 ns, p99:368900 ns, max:444900 ns
q->p50:101900 ns, p95:274800 ns, p99:369000 ns, max:445000 ns
svc->p50:796300 ns, p95:1891800 ns, p99:2059700 ns, max:2084200 ns
e2e->p50:928600 ns, p95:1971700 ns, p99:2347900 ns, max:2616000 ns
sample n = 205
---------
pendingMaxSoFar=32 rejectOverLimit=52504 tpe(pool=6 active=1 q=0)
...
20:02:30.731 [stat-logger] d.p.o.DisruptorNettyOMSServer - partitionLatencyLogging - INFO  P3:
HOT
execQ->p50:11000 ns, p95:26700 ns, p99:43000 ns, max:3636600 ns
q->p50:48682500 ns, p95:53600600 ns, p99:62845000 ns, max:295227300 ns
svc->p50:1839500 ns, p95:2059600 ns, p99:2136500 ns, max:77689200 ns
e2e->p50:50104600 ns, p95:55038800 ns, p99:65639300 ns, max:296857100 ns
sample n = 40646
---------
COLD
execQ->p50:138300 ns, p95:450000 ns, p99:757000 ns, max:866800 ns
q->p50:139700 ns, p95:450200 ns, p99:757700 ns, max:867000 ns
svc->p50:1031200 ns, p95:2002900 ns, p99:2104800 ns, max:2220300 ns
e2e->p50:1352800 ns, p95:2519900 ns, p99:5272900 ns, max:12113200 ns
sample n = 600
---------
pendingMaxSoFar=32 rejectOverLimit=79354 tpe(pool=6 active=0 q=0)


```

GC logs
```
1 GC event ~ 6.4 ms
```

## B5) Final deliverable (15 min): write your “mini report”

Template:

## Benchmark setup
- CPU: 12 cores (local dev machine)
- JDK: OpenJDK 25 LTS
- GC: G1
- Heap: -Xms512m -Xmx512m
- GC flags: -Xlog:gc*,safepoint:time,uptime,level,tags
- Logging: percentile snapshot every 5s (sorting-based), counters every 5s

### Scenarios
1) Baseline (steady)
    - corrId=11 @ 300 msg/s
    - cold families @ ~5 msg/s total
2) Allocation hotspot
    - corrId=11 @ 300 msg/s
3) Hot + cold overload (B4)
    - hot: corrId=11 @ 2000 msg/s
    - cold: 1003,1011,1019,1027,1035 @ 5 msg/s total
    - duration: ~120s

### Work cost
- Simulated downstream work: ~1–2 ms per task
- Single in-flight per family (sequential per family)

## Baseline results (ALLOC_HOTSPOT = false)
Steady-state (corrId=11 @ 300 msg/s):

- svc
    - p50 ≈ 1.1–1.2 ms
    - p99 ≈ ~2.0 ms
- q
    - p99 ≈ ~0.2 ms
- e2e
    - p50 ≈ ~1.3 ms
    - p99 ≈ ~2.2 ms

Observations:
- Service time dominates e2e
- Queueing negligible
- GC activity minimal (2 young GCs over ~4 minutes)

## GC experiment

### Hotspot change
- Added per-task allocation: new byte[256*1024] in hot path
- Heap unchanged (512 MB)
- Sustained steady load

### Observed GC pattern
- Baseline: 2 young GCs, ~8–10 ms pauses
- With allocation hotspot:
    - ~67 young GCs
    - frequent safepoints
    - allocation-driven evacuation pauses

### Effect on latency
- svc p99:
    - Baseline: ~2.1 ms
    - With hotspot: spikes up to ~9–48 ms
- e2e p99:
    - Significant wobble and tail inflation
- Queue depth remained small → latency increase driven by GC, not queueing

Conclusion:
- Allocation rate directly impacts svc p99 and therefore e2e p99
- Even “small” per-task allocations are catastrophic under sustained load

## Improvement applied

### Change 1 — Allocation mitigation
- Replaced per-task allocation with ThreadLocal<byte[]>
- Ensured allocation result consumed via sink to prevent JIT elimination

Before:
- ~67 young GCs
- svc p99 unstable, large max spikes
- e2e p99 degraded

After:
- 1–3 young GCs
- svc p99 back to ~2 ms
- e2e close to baseline

Metric justification:
- GC frequency dropped by >20x
- svc p99 stabilized

---

### Change 2 — Structural protection (pending cap)

Problem observed:
- With pendingLimit=1024 under hot load:
    - hot-family q p50 ≈ 1.5 seconds
    - e2e p99 ≈ ~1.6 seconds
    - Memory bounded, but latency unusable

Change:
- Defined latency budget ≈ 50 ms
- pendingLimit = floor(latencyBudget / svc_p99) ≈ 32

Results (corrId=11 @ 2000 msg/s):

Before (pendingLimit=1024):
- q p50 ≈ ~1.5 s
- e2e p99 ≈ ~1.6 s
- rejectOverLimit ≈ 0 initially

After (pendingLimit=32):
- q p50 ≈ ~48 ms
- q p99 ≈ ~55–63 ms
- e2e p99 ≈ ~56–66 ms
- pendingMaxSoFar = 32
- rejectOverLimit steadily increases
- cold families unaffected (e2e p99 ≈ 2–5 ms)

Metric justification:
- pendingMax plateau proves bounded backlog
- q/e2e p99 matches theoretical bound: pendingLimit × svc
- Cold-family latency preserved under overload


## Final conclusions

1) Allocation rate, not average CPU, determines svc p99.
2) GC shows up as svc tail latency even when queueing is low.
3) A pending cap must be derived from a latency budget, not memory size.
4) pendingLimit × svc ≈ worst-case queue latency.
5) Explicit rejection is required; silent drops are unacceptable.
6) Structural protection successfully isolates hot and cold families.


### Summary
With sustained overload(corrId = 11 at 2000 msg/s) and pendingLimit reduced from 1024 to 32 (≈ 50ms / svc_p99), 
the hot-family queueing time dropped from ~1.5s to ~50–66ms at p50–p99. 
pendingMaxSoFar plateaued at 32 and rejectOverLimit increased steadily, demonstrating bounded backlog and explicit load shedding. 
Cold families remained stable at millisecond-level e2e latency. Rare max outliers (~296ms) remained, likely due to non-GC stalls (only 1 young GC ~6.4ms).

---

# What you will have after 4–5 hours

You’ll be able to say, with confidence:

1. “My p99 is dominated by queueing under burst; service time p99 is stable.”
2. “Allocations move svc p99 and correlate with GC pauses.”
3. “My pending cap bounds memory and bounds worst-case latency.”

That’s real low-latency engineering.

---

# Implementation hints (so you don’t get stuck)

* Keep the recorder **per partition**, not global, to avoid contention.
* Don’t chase “perfect benchmarking”; we want **repeatable**.
* Don’t mix client networking latency with server latency initially:

    * measure inside server first (`t0` at netty read)
* Run with fixed CPU performance mode if you can (optional).

---

If you paste your current `TaskEvent` (or equivalent) fields and where “done” is recorded, I can tell you the **best 2 places** to insert `t2/t3` so you don’t accidentally measure the wrong boundary.


# 🎉 At the end of Day 7

You will have built:

```
            ┌───────────────────────┐
            │     Netty Gateway     │
            └──────────┬────────────┘
                       │ channelRead
                       ▼
              [ Disruptor RingBuffer ]
                       │
     ┌─────────────┬─────────────┬─────────────┐
     ▼             ▼             ▼             ▼
 [Parse]     [Validate]     [RiskCheck]   [Execute]
                       ▼
                 [Response]
                       ▼
                Netty write()
```

This is effectively a **mini OMS engine architecture**.
This project alone dramatically strengthens your profile for **low-latency trading roles**.

---

# Ready to begin?

If you're ready, just say **“Let’s start Day 1”** and I’ll guide you step-by-step exactly like your previous Netty days.
