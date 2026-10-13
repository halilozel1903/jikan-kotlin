#!/usr/bin/env bash
# Runs the open source Jikan server (https://github.com/jikan-me/jikan-rest) with MongoDB in Docker, for the README
# screenshots. api.jikan.moe drops connections from GitHub-hosted runners, while MyAnimeList itself is reachable, so
# CI runs the same /v4 API itself: jikan-rest scrapes MyAnimeList live on each lookup.
#
# A fresh server has an empty database, and answers lists such as /seasons/now or /top/anime from it. So this script
# then requests every anime of the lists the sample's screens show (scripts/jikan-seed-ids.php reads them from
# MyAnimeList), which makes the server scrape and store them, as its own indexers do.
#
# Serves http://localhost:8080/v4/ (http://10.0.2.2:8080/v4/ from an Android emulator).
set -euo pipefail

# jikan-rest v4.2.4, the latest release, pinned by digest.
JIKAN_IMAGE="docker.io/jikanme/jikan-rest:v4.2.4@sha256:505f06101617c337db5b301701258de63a55f6593b0181cf2d84c92ff49cee9d"
# The season queries use $dateDiff, which needs MongoDB 5 or newer.
MONGO_IMAGE="docker.io/library/mongo:7.0.14"
API="http://localhost:8080/v4"
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"

docker network create jikan > /dev/null
docker run -d --name mongodb --network jikan "$MONGO_IMAGE" --wiredTigerCacheSizeGB 1 > /dev/null
docker run -d --name jikan --network jikan -p 8080:8080 \
  -v "$SCRIPTS:/seed:ro" \
  -e APP_ENV=production \
  -e APP_URL=http://localhost:8080 \
  -e LOG_LEVEL=info \
  -e DB_CONNECTION=mongodb \
  -e DB_HOST=mongodb \
  -e DB_PORT=27017 \
  -e DB_DATABASE=jikan \
  -e DB_ADMIN=jikan \
  -e DB_CACHING=true \
  -e CACHING=false \
  -e MICROCACHING=false \
  -e SCOUT_DRIVER=none \
  -e GITHUB_REPORTING=false \
  -e REPORTING=false \
  "$JIKAN_IMAGE" > /dev/null

echo "Waiting for jikan-rest"
for i in $(seq 1 90); do
  if curl -fsS -o /dev/null --max-time 5 "http://localhost:8080/"; then
    echo "jikan-rest answers after ${i}s"
    break
  fi
  if [ "$i" = 90 ]; then
    echo "jikan-rest did not start" >&2
    docker logs jikan 2>&1 | tail -n 60 >&2
    exit 1
  fi
  sleep 1
done
docker exec jikan php /app/artisan migrate --force

# The first lookup scrapes MyAnimeList; it shows whether the server can read it from this runner.
status=""
for i in 1 2 3 4 5; do
  status="$(curl -sS -o /tmp/jikan-5114.json -w '%{http_code}' --max-time 90 "$API/anime/5114" || echo failed)"
  echo "GET /v4/anime/5114: $status"
  [ "$status" = 200 ] && break
  sleep $((i * 5))
done
if [ "$status" != 200 ]; then
  echo "jikan-rest cannot load anime from MyAnimeList:" >&2
  head -c 2000 /tmp/jikan-5114.json >&2 || true
  docker logs jikan 2>&1 | tail -n 60 >&2
  exit 1
fi
python3 -c "import json; d = json.load(open('/tmp/jikan-5114.json'))['data']; print(d['title'], d['score'])"

echo "Reading the lists to load from MyAnimeList"
docker exec jikan php /seed/jikan-seed-ids.php > /tmp/jikan-ids.txt
echo "$(wc -l < /tmp/jikan-ids.txt) anime to load"

# Loads one anime; prints its id when it fails (MyAnimeList answers some bursts with errors).
load() {
  local code
  code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 90 "$API/anime/$1" || echo 000)"
  case "$code" in
    200 | 404) ;;
    *) echo "$1" ;;
  esac
}
export -f load
export API

: > /tmp/jikan-failed.txt
xargs -P 3 -I{} bash -c 'load {}' < /tmp/jikan-ids.txt > /tmp/jikan-failed.txt
for round in 1 2 3; do
  [ -s /tmp/jikan-failed.txt ] || break
  echo "Retrying $(wc -l < /tmp/jikan-failed.txt) anime (round $round)"
  sleep 20
  mv /tmp/jikan-failed.txt /tmp/jikan-retry.txt
  : > /tmp/jikan-failed.txt
  while read -r id; do
    load "$id" >> /tmp/jikan-failed.txt
    sleep 2
  done < /tmp/jikan-retry.txt
done
total="$(wc -l < /tmp/jikan-ids.txt)"
failed="$(wc -l < /tmp/jikan-failed.txt)"
echo "Loaded $((total - failed)) of $total anime"
if [ "$failed" -gt $((total / 10)) ]; then
  echo "Too many anime failed to load: $(tr '\n' ' ' < /tmp/jikan-failed.txt)" >&2
  docker logs jikan 2>&1 | tail -n 40 >&2
  exit 1
fi

# The lookups the scenes make, so the app does not wait for MyAnimeList on them.
for path in anime/5114/full anime/5114/episodes anime/5114/characters anime/5114/recommendations seasons; do
  echo "GET /v4/$path: $(curl -sS -o /dev/null -w '%{http_code} in %{time_total}s' --max-time 90 "$API/$path" || echo failed)"
done

# What the screens will show.
show() {
  curl -fsS --max-time 60 "$API/$1" | python3 -c '
import json, sys
data = json.load(sys.stdin)["data"]
print("%s: %d entries" % (sys.argv[1], len(data)))
for a in data[:6]:
    print("  %s (%s)" % (a.get("title"), a.get("score")))
' "$1"
}
show "seasons/now?sfw=true&limit=24"
show "top/anime?filter=airing&sfw=true&limit=20"
show "seasons/upcoming?sfw=true&limit=20"
show "anime?genres=10&order_by=score&sort=desc&sfw=true&limit=20"
show "anime?genres=24&order_by=score&sort=desc&sfw=true&limit=20"
show "seasons/2024/summer?sfw=true&limit=24"
