# Day-5 Mini-OMS Pipeline Spec (v1)
Date: 2025-12-24  
Focus: single-in-flight for correlated tasks only  
N partitions: 4  
Partition impl: 1 Disruptor + 1 consumer thread per partition

---

## 1) Architecture
```
Client (TCP)
|
v
Netty Server (NIO EventLoops)
- decode inbound message -> TaskEvent
- compute partition
- publish to partition Disruptor
  |
  +--> Partition 0: Disruptor0 -> ConsumerThread0 -> Scheduler/OMS -> channel.writeAndFlush(response)
  +--> Partition 1: Disruptor1 -> ConsumerThread1 -> Scheduler/OMS -> channel.writeAndFlush(response)
  +--> Partition 2: Disruptor2 -> ConsumerThread2 -> Scheduler/OMS -> channel.writeAndFlush(response)
  +--> Partition 3: Disruptor3 -> ConsumerThread3 -> Scheduler/OMS -> channel.writeAndFlush(response)
```
Responses may interleave across partitions on the same connection. Ordering guarantees apply only per correlated id.

---

## 2) Correlation rules

- If `correlationId > 0`: tasks are correlated
    - must enforce FIFO and single-in-flight for that correlationId
- If `correlationId <= 0`: tasks are uncorrelated
    - may be routed to any partition
    - no FIFO / single-in-flight guarantee required

---

## 3) Partitioning & routing (N=4)

### For correlated tasks (correlationId > 0)
- `partition = hash(correlationId) % 4`

Invariant: the same correlationId always routes to the same partition.

### For uncorrelated tasks (correlationId <= 0)
- `partition = roundRobin()` (or random)
- goal: spread load across partitions

---

## 4) Must-never-break invariants (v1)

For `correlationId > 0` only:
1) FIFO per correlationId (defined by enqueue order into the per-correlation queue)
2) Single-in-flight per correlationId

For `correlationId <= 0`:
- best-effort processing; no ordering constraints

Bounded resources:
- 4 consumer threads
- bounded ring buffers; reject on overflow

---

## 5) Event model

### TaskEvent (inbound)
- correlationId (long)
- payload (minimal DTO / raw bytes / string)
- Channel (or ChannelHandlerContext) reference for reply
- t0 (optional timestamp)

### Response (outbound)
- correlationId
- partitionId
- decision (route/reject/target)
- timing fields (optional)

---

## 6) Partition internals (single writer, correlated only)

Per correlated id (correlationId > 0):
- `queue`: FIFO pending tasks
- `inFlight`: boolean
- `enqueued`: boolean (in readyQ)

Scheduler:
- `readyQ`: FIFO of correlationIds with pending work

Single-writer rule:
- only the partition consumer thread mutates state.

Uncorrelated tasks (correlationId <= 0):
- processed immediately on arrival (no per-id state)
- OR placed into a simple FIFO queue local to the partition (no inFlight tracking)

---

## 7) Scheduling & completion (v1)

### On event arrival (partition thread)
- If correlated (id > 0):
    - append to per-id queue
    - if transition empty->non-empty and not enqueued: enqueue id into readyQ
- If uncorrelated (id <= 0):
    - process directly (no single-in-flight constraint)

### Dispatch (correlated)
- pop id from readyQ
- if not inFlight and queue not empty:
    - task = queue.poll()
    - inFlight = true
    - decision = miniOMS.process(task)   (non-blocking in v1)
    - channel.writeAndFlush(response)
    - completion(v1): after writeAndFlush invoked
        - inFlight = false
    - if queue not empty: re-enqueue id

---

## 8) Backpressure
- If partition ring buffer full: reject fast (`REJ busy`).
- Optional: bound per-id queue size; reject if too large.

---

## 9) Observability
Response should include:
- correlationId
- partitionId
- decision
- t0/t1 (optional)
  Purpose:
- verify correlated id FIFO + single-in-flight
- inspect distribution of uncorrelated tasks
