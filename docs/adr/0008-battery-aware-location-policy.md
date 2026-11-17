# 0008. Battery-aware location policy

- Status: accepted
- Date: 2026-11-16

## Context
GPS at full rate drains the battery during long drives.

## Decision
A pure `RecordingPolicyResolver` picks high accuracy (2 s/5 m) while moving, balanced (15 s/25 m) when stationary over 2 min, and balanced (10 s/20 m) in power-save, at ≤ 20% battery or in Saver mode. The service re-requests updates only when the policy changes, and writes points in batches.

## Consequences
Predictable battery use; the policy is unit-tested.

## Alternatives considered
A fixed interval; Activity-Recognition-driven sampling only.
