### Day 4 — Netty → Disruptor Handoff (To-Do List)

#### 1) Stabilize the architecture (one-time setup)

* [x] **Start Disruptor once** at server bootstrap (not inside `ChannelHandler` ctor).
* [x] **Do not shutdown Disruptor per channel** (remove from handler `close()`).

#### 2) Fix timing correctness (`t0` per message)

* [x] Remove handler-level `volatile long t0` as the source of truth.
* [x] Record `t0` **inside the Disruptor event** (you already have `timestamp`).
* [x] For “end-to-end (server-side)” log, measure at a clean point:
    * [x] **Inside the EventLoop runnable** right before `writeAndFlush` (best for Day 4).
    * (Optional later) also add a `ChannelFuture` listener to measure “write completed”.

#### 3) Add/confirm the 4 timestamps + metrics

* [x] `t0`: when Netty receives a full frame (start of `channelRead`)
* [x] `t1`: immediately after publish attempt finishes (success or reject)
* [x] `t2`: start of Disruptor consumer `onEvent`
* [x] `t3`: inside EventLoop task right before `writeAndFlush`
* [x] Log these derived metrics:

    * [x] Publish cost = `t1 - t0`
    * [x] Backpressure/queue cost = `t2 - t1`
    * [x] Business cost = `t3 - t2`
    * [x] End-to-end (to write) = `t3 - t0`

#### 4) Implement reject policy (producer-side overload behavior)

* [x] Decide rule: **Reject when ring is full** (fast fail) rather than blocking Netty EventLoop.
* [x] Implement using **capacity check / try-publish**:

    * [x] If no capacity: respond `BUSY` (or `REJ`) back to client via EventLoop.
    * [x] Track a counter: `rejectedCount`.
* [x] Confirm behavior under load: EventLoop stays responsive, rejects increase.

---

## Experiments (must-do)

### Experiment A — Saturation + reject behavior

Goal: prove you control overload.

* [x] Set ring size small (e.g., 32 or 64).
* [x] Turn on `slow` business mode.
* [x] Run a client that sends continuously.
* [x] Observe & record:

    * [x] Backpressure cost grows as queue fills.
    * [x] Rejects begin once full (instead of EventLoop stalling).
    * [x] Publish cost stays small (no blocking).
    * [x] Client receives fast `BUSY/REJ`.

### Experiment B — Two clients (fairness / noisy neighbor)

Goal: see cross-connection coupling.

* [x] Run Client A at high rate, Client B at low/steady rate.
* [x] Keep consumer slow to induce queueing.
* [x] Observe & record:

    * [x] Does Client A cause Client B latency to spike?
    * [x] Do rejects happen to both equally or mostly to the noisy one?
    * [x] Note: single shared Disruptor = shared bottleneck (expected).

---

## Deliverables for your notebook (end of Day 4)

* [x] A short diagram: **EventLoop → Disruptor → EventLoop**
* [x] A small table of measured numbers (normal vs slow vs saturated)
* [x] One paragraph: “What causes latency (queueing) vs what causes throughput (slowest stage)”
* [x] One paragraph: “Reject policy prevents EventLoop stall; wait strategy is consumer-side only”

If you want, paste your current `publishToDisruptor(...)` method and I’ll point out the smallest change that implements reject correctly without blocking the EventLoop.
