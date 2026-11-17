# 0011. No map SDK: in-app route canvas

- Status: accepted
- Date: 2026-11-16

## Context
Map SDKs need API keys and network tiles, add Data-safety disclosures, and add megabytes to the APK.

## Decision
`core:ui` `RouteCanvas` projects track points onto a dot grid with a halo, pins, labels and a scale bar, and can draw on or replay a route.

## Consequences
Fully offline, no `INTERNET` permission, smaller APK, consistent look. Costs: no street context.

## Alternatives considered
Google Maps Compose; MapLibre with OSM tiles.
