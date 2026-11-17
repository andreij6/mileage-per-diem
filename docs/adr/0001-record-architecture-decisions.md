# 0001. Record architecture decisions

- Status: accepted
- Date: 2026-11-16

## Context
We need a lightweight, durable record of why the codebase looks the way it does, for people and for coding agents.

## Decision
Use MADR-style ADRs in `docs/adr/`, numbered and never rewritten; a reversed decision gets a new ADR that supersedes the old one.

## Consequences
Decisions are reviewable in PRs; agents read them before changing structure.

## Alternatives considered
Wiki pages (drift from code); commit messages only (hard to find).
