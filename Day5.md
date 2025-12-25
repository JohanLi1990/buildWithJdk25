
## Session 1 (1–2h): Architecture skeleton + invariants

**Goal:** lock down the rules so implementation doesn’t drift.

✅ Checklist

* Decide **N partitions** (e.g., 4/8) and the routing formula:

    * correlated: `hash(correlationId) % N`
    * `-1`: derived family key then hash
* Define the “must never break” invariants:

    * FIFO per family
    * single-in-flight per family
    * bounded threads/resources
* Decide scheduler policy for v1:

    * start with **round-robin, 1 event per family per turn**
* Decide where sequencing is assigned:

    * `sequenceInFamily` assigned on ingress (Netty side) or partition side (pick one)

Deliverable: a short markdown spec for your Day-5 design.

---

## Session 2 (1–2h): Netty ingress → partition publish + busy reject

**Goal:** Netty can route and apply backpressure.

✅ Checklist

* Event has `correlationId` + derived key for `-1`
* Correct partition selection
* tryPublish success path increments `published`
* ring full path returns `REJ:BUSY` quickly
* basic logs: `partition, correlationId, seqInFamily`

Deliverable: you can send 1 message and get 1 response.

* **Key takeaway**
  * Multilple Netty evenloop threads can publish into the same partition
    * use `ProducerType.MULTI` instead of `ProducerType.SINGLE`
  * Logging Strategy:
    * Sample logging to avoid hot-path overhead
    ```java
      if ((totalCounts & 0x3FFF) == 0) {
        log.info("published={}, rejected={}", published, rejected); 
      }
    ```
  * Routing Strategy:
    * Correlated Task: `partition = floorMode(correlationId, N)`
    * Uncorrelated Task: `partition = floorMod(System.identityHashCode(ctx.channel()), N);
`

---

## Session 3 (1–2h): Partition inbox → familyQueues + readyFamilies

**Goal:** your FamilyScheduler data structures work.

✅ Checklist

* Events land in `familyQueues[familyKey]` FIFO
* `readyFamilies` contains each family at most once
* Families become “not ready” only when queue empty or currently in-flight
* A single worker loop can pop a family and then pop its next event

Deliverable: you can “observe” fair interleaving in logs even before OMS stages exist.

---

## Session 4 (1–2h): Single-in-flight enforcement + ordering assertions

**Goal:** prove the UBS constraint.

✅ Checklist

* When a family is selected, it is marked **IN_FLIGHT**
* Next event from same family is not processed until release
* Assert `sequenceInFamily` monotonic per family at Stage1 entry (dev mode)
* Test: create A(1..10) and B(1..10), verify ordering per family

Deliverable: correctness proof via logs.

---

## Session 5 (1–2h): Add the 4 OMS stages (mock)

**Goal:** full pipeline end-to-end.

✅ Checklist

* Stage1 parse: can reject BAD_MSG
* Stage2 risk: can reject RISK_*
* Stage3 exec sim: only on ACCEPT
* Stage4 response: exactly one response
* Netty write-back is done safely (event-loop marshalling)

Deliverable: ACK/REJ responses come back correctly.

---

## Session 6 (1–2h): Stress scenarios (the ones that matter)

**Goal:** validate hot-key + -1 behavior.

✅ Checklist

* **30k same correlationId** + some other families:

    * others still make progress (no starvation)
* **all -1 burst**:

    * spreads across partitions, no accidental serialization
* ring buffer small → more BUSY rejects but system stays responsive

Deliverable: basic throughput/latency counters + sanity checks.

---

## Session 7 (optional, 1–2h): Improve fairness knob

**Goal:** make it feel “real”.

Upgrade policy from “1 event per family” to:

* `maxEventsPerSlice` (e.g., 16/32)
  or
* time-slice budget

✅ Checklist

* cold families’ tail latency improves when hot family exists
* switching overhead doesn’t crush throughput
* you can tune slice size and see a predictable change

---

### Why this fits 1–2h/day

Each session is focused on *one layer*:
Netty routing → scheduler correctness → OMS stages → stress tests.

No single session requires deep refactors, which is the real killer in 1-hour chunks.

---

Perfect. Here’s a **clean, paste-ready “Definition of Done” card for Day 5** — concise, OMS-realistic, and aligned with everything you discovered at UBS.

---

## ✅ Day 5 — Mini OMS + FamilyScheduler

**Definition of Done**

### 1. Correlation correctness (non-negotiable)

* [ ] Every task has a `correlationId`
* [ ] Tasks with the same `correlationId` are processed in **strict FIFO**
* [ ] At any moment, **only one thread processes a given correlation family**
* [ ] `sequenceInFamily` is strictly monotonic (asserted in dev mode)

---

### 2. Bounded concurrency (no resource explosions)

* [ ] Fixed number of partitions (no per-family threads or disruptors)
* [ ] Same correlation family always maps to the same partition
* [ ] Partition threads are long-lived and bounded

---

### 3. FamilyScheduler fairness

* [ ] Each partition has a FamilyScheduler
* [ ] Scheduler prevents a hot family (e.g. 30k tasks) from starving others
* [ ] Scheduler never enqueues the same family twice concurrently
* [ ] Scheduling policy is explicit (round-robin / slice-based)

---

### 4. Special handling of `correlationId = -1`

* [ ] `-1` tasks do **not** serialize accidentally
* [ ] `-1` tasks spread across partitions using derived family keys
* [ ] `-1` tasks never break FIFO guarantees of correlated families

---

### 5. OMS pipeline completeness

* [ ] Stage 1: Parse (can reject BAD_MSG)
* [ ] Stage 2: Risk check (can reject with deterministic reason)
* [ ] Stage 3: Execution simulation (only on ACCEPT)
* [ ] Stage 4: Prepare response
* [ ] Exactly **one response per request**

---

### 6. Netty ↔ Disruptor boundary safety

* [ ] Netty event loop does minimal work
* [ ] Backpressure handled via fast reject (`REJ:BUSY`)
* [ ] All responses are written on the correct Netty event loop
* [ ] No ByteBuf leaks in steady state

---

### 7. Stress & realism checks

* [ ] 30k tasks from same correlationId process sequentially
* [ ] Other families still make progress during hot-key flood
* [ ] All-`-1` bursts achieve high throughput
* [ ] Small ring buffer triggers rejects, not latency collapse

---

### 8. Observability (minimal but sufficient)

* [ ] Counters: published / rejectedBusy / accepted / rejected
* [ ] Logs can show: `partition, correlationId, seqInFamily`
* [ ] Logging can be disabled cleanly for perf runs

---

### 9. Mental model clarity (the real win)

* [ ] Can explain why FamilyScheduler is needed
* [ ] Can explain why “1 disruptor per correlationId” doesn’t scale
* [ ] Can point to where FIFO, fairness, and backpressure are enforced

---

### ⭐ Bonus (optional)

* [ ] Slice-based scheduling (events or time)
* [ ] p99 / p999 latency measurement
* [ ] Clean shutdown with no lost events

---

This card is **exactly the kind of artifact senior low-latency engineers keep** — not code, but *system truth*.

When you finish Day 5, you’ll be able to reason about:

* SOR queues
* OMS engines
* hot-key fairness
* bounded concurrency

…without hand-waving.

When you’re ready, next step after Day 5 is usually **“where do I deliberately break this system?”** (GC pressure, slow stage, unfair scheduling) — that’s where mastery kicks in.

