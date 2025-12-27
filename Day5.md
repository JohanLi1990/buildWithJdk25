
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
    * Uncorrelated Task: `partition = floorMod(System.identityHashCode(ctx.channel()), N);`
---

## Session 3 (1–2h): Minimal-C skeleton — partitioned ingress + busy-gated scheduler + worker pool

**Goal:** get the *real* architecture running with minimal features.

✅ Checklist

* Netty inbound extracts `familyKey`, routes to `partition = smearHash(familyKey) & (N-1)`
* Per partition: Disruptor consumer thread is **dispatcher only** (non-blocking)
* Per partition: `FamilyScheduler` with:

    * `FamilyState.busy` (0/1)
    * `FamilyState.pending` FIFO
* Worker pool executes simulated 10–20ms work and calls `scheduler.onComplete(familyKey)`
* Ring buffer full → fast reject `REJ:BUSY`

**Deliverable:** end-to-end “dispatch → execute → complete” works, with logs showing:

* same family serialized
* different families concurrent

- **Key Takeaway**
  - Disruptor Events must not escape the consumer thread
    - RingBugger resuses TaskEvent objects
    - Queuing them into `pending` causes data corruption.
  - **Recursive drain on completion**
    - Every dispatched task **must schedule the next one, or release busy**
    ```java
    dispatchAndDrain(state, task):
      async work
      onComplete:
        next = pending.poll()
        if next != null:
           dispatchAndDrain(state, next)
        else:
           busy = 0
           race fix:
             if new task arrived → re-acquire busy → dispatch
    ```
    - This guarantees exactly 1 in-flight task per family
    - FIFO preserved
    - no family can wedge permanently
  - **Lost wakeup prevention pattern**
    ```java
    onComplete():
        task = queue.poll()
        if (task != null) {
            run(task)
            return
        }
    
        flag = IDLE
    
        // ---- race fix ----
        task = queue.poll()
        if (task != null && CAS(flag, IDLE, BUSY)) {
            run(task)
        }
    ```
    - Problem Statement:
      - 2 threads coordinates via a `flag` and a `work queue`
      - The operations `check queue` and `release flag` are not atomic
    - Pattern solution:
      - After releasing the flag, recheck the queue and reclaim ownership if work exist
---

## Session 4 (1–2h): Correctness proof — ordering assertions + race hardening

**Goal:** prove UBS constraint rigorously (and catch the nasty races early).

✅ Checklist

* Add `seqInFamily` to test events (monotonic per family)
* Assert **completion order** per family (strictly increasing)
* Implement the **race-safe completion** logic (poll-after-idle + CAS reclaim)
* Deterministic test: A(1..10), B(1..10), plus mixed interleaving
* “One hot family + others” test: hot family serialized, others progress

**Deliverable:** a repeatable test suite (even if just a main method) that fails loudly on reordering.

> Note: At this point you don’t need `readyFamilies` at all — busy-gating *is* your readiness.

---

## Session 5 (1–2h): Add the 4 OMS stages (mock) on the worker side

**Goal:** make work look like OMS without breaking the scheduler.

✅ Checklist

* Worker executes Stage1→Stage4 pipeline (pure functions or mocked)

    * Stage1 parse → can reject `BAD_MSG`
    * Stage2 risk → can reject `RISK_*`
    * Stage3 exec sim → only if ACCEPT
    * Stage4 response build → exactly one response
* Exactly one response per inbound request (ACK/REJ)
* Netty write-back is safe:

    * marshal back to event loop (`ctx.channel().eventLoop().execute(...)`)
    * or use a dedicated outbound queue per channel (later)

**Deliverable:** client receives correct ACK/REJ for mixed workloads.

---

## Session 6 (1–2h): Stress scenarios + instrumentation (baseline numbers)

**Goal:** validate the scenarios that actually break OMS-like systems and record baseline counters.

✅ Checklist

* Scenario 1: **30k same family** + other families

    * verify others progress (no global stall)
    * record `maxPendingDepthObserved` for hot family
* Scenario 2: **all correlationId = -1 burst**

    * spreads across partitions
    * no accidental serialization
* Scenario 3: ring buffer small

    * more `REJ:BUSY` but system stays responsive
* Add counters per partition:

    * tasksIn / dispatched / completed
    * enqueuedDueToBusy
    * maxPendingDepthObserved
    * rejectsBusy (ring buffer full)
* Add 2–3 periodic log lines (every X ms) rather than per-task spam

**Deliverable:** baseline throughput + reject rate + “looks healthy under stress” evidence.

---

## Session 7 (1–2h): Optimization / realism step (pick 1–2 only, guided by counters)

**Goal:** apply one practical optimization that *measurably* improves either tail latency or robustness.

Pick **one** track based on what Session 6 shows:

### Track A — Fairness knob (tail latency under hot family)

Upgrade policy so hot family can’t hog worker capacity:

* `maxEventsPerSlice` per family (e.g., 8/16/32) **OR**
* time-slice budget

✅ Checklist

* cold families’ tail latency improves when hot family exists
* throughput doesn’t collapse
* you can tune slice size and see predictable changes

### Track B — Bounded per-family pending (robustness under pathological hot key)

Add safety limits:

* if `pending.size > limit` → reject new tasks for that family (or coalesce)

✅ Checklist

* hot family doesn’t OOM you
* rejection is localized to the hot family (others keep running)

### Track C — Hot-path hygiene (only if scheduler overhead shows up)

If you see dispatcher/scheduler dominating (unlikely with 10–20ms work, but possible later):

* reduce allocations in pending queues
* reduce contention in `FamilyState`
* (later) consider padding / `@Contended` only if profiling shows it

✅ Checklist

* measured reduction in scheduler CPU or improved p99 (with your counters)

**Deliverable:** one measurable improvement, plus a note of what metric justified it.

---

## Why this fits 1–2h/day

Each session is focused on *one layer*:
Netty routing → scheduler correctness → OMS stages → stress tests.

No single session requires deep refactors, which is the real killer in 1-hour chunks.

---

Perfect. Here’s a **clean, paste-ready “Definition of Done” card for Day 5** — concise, OMS-realistic, and aligned with everything you discovered at UBS.

Yep — since **Minimal-C collapses old Session 3+4 into one “scheduler + worker + ordering proof” build**, we should rewrite Sessions 3–7 so each session produces a clean, testable milestone *and* we insert an explicit **optimization / hygiene** step (but only after we have counters showing it matters).

Here’s an updated plan you can paste into your notebook.

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

