#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running Android TV emulator.
# D-pad presses can't be timed reliably through adb on a fresh emulator, so the sample opens each
# scene from the `scene` extra. Scenes load live data from the Jikan API and MyAnimeList's cover
# images, and log when everything on screen has loaded; a scene that fails to load fails the run.
#
# JIKAN_API_URL points the scenes at another Jikan server: CI runs jikan-rest (scripts/jikan-server.sh)
# and passes http://10.0.2.2:8080/v4/, the runner as seen from the emulator.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
API_URL="${JIKAN_API_URL:-https://api.jikan.moe/v4/}"
echo "Jikan API: $API_URL"
wait_for_network cdn.myanimelist.net "https://cdn.myanimelist.net/images/anime/1208/94745.jpg"

launch_scene() {
  fresh_launch --es scene "$1" --es api "$API_URL"
}

# The hero for the current season's best rated show, with the "Top airing" rail below it.
launch_scene home
wait_for_scene home 240
capture tv-home "Top airing"

# Fullmetal Alchemist: Brotherhood: synopsis, score, episodes (the first one focused) and characters.
launch_scene detail
wait_for_scene detail 240
capture tv-detail "Fullmetal Alchemist"

# The summer 2024 season grid with the season picker, the first poster focused.
launch_scene season
wait_for_scene season 240
capture tv-season "Summer 2024"
