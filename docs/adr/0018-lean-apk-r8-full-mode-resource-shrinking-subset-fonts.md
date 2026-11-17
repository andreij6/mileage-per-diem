# 0018. Lean APK: R8 full mode, resource shrinking, subset fonts

- Status: accepted
- Date: 2026-11-16

## Context
Size affects install conversion and update cost.

## Decision
R8 full mode with `-repackageclasses` and `-allowaccessmodification`, resource shrinking, English-only resources, Lucide icons as vector drawables, Inter and Space Grotesk variable fonts subset to Latin (880 KB to 112 KB), no AppCompat, no Material icons library, and no JSON library (a 60-line flat JSON codec for analytics).

## Consequences
Small download and fast class loading.

## Alternatives considered
Shipping full fonts and icon packs.
