#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running Android TV emulator.
# D-pad presses can't be timed reliably through adb on a fresh emulator, so the sample opens each
# scene from the `scene` extra. Scenes read the bundled JSON fixtures and draw original cover art:
# no network, no real anime artwork.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample

# The hero for the current season's top show, with the "Top airing" rail below it.
fresh_launch --es scene home
capture tv-home "Top airing"

# "Harbor of a Thousand Bells": synopsis, score, episodes (the first one focused) and characters.
fresh_launch --es scene detail
capture tv-detail "The Bell That Rang Twice"

# The summer 2026 season grid with the season picker, the first poster focused.
fresh_launch --es scene season
capture tv-season "Summer 2026"
