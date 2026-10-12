#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running Android TV emulator.
# D-pad presses can't be timed reliably through adb on a fresh emulator, so the sample opens each
# scene from the `scene` extra. Scenes load live data from the Jikan API and MyAnimeList's cover
# images, and log when everything on screen has loaded; a scene that fails to load fails the run.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
wait_for_network api.jikan.moe "https://api.jikan.moe/v4/anime/5114"

# The hero for the current season's best rated show, with the "Top airing" rail below it.
fresh_launch --es scene home
wait_for_scene home
capture tv-home "Top airing"

# Fullmetal Alchemist: Brotherhood: synopsis, score, episodes (the first one focused) and characters.
fresh_launch --es scene detail
wait_for_scene detail
capture tv-detail "Fullmetal Alchemist"

# The summer 2024 season grid with the season picker, the first poster focused.
fresh_launch --es scene season
wait_for_scene season
capture tv-season "Summer 2024"
