<?php
// Prints the MyAnimeList ids that scripts/jikan-server.sh loads into a fresh jikan-rest server before the README
// screenshots are captured. Runs inside the jikan-rest container and reads MyAnimeList with the jikan parser that
// jikan-rest itself uses.
//
// A fresh jikan-rest server answers lookups such as /anime/{id} by scraping MyAnimeList, but answers lists such as
// /seasons/now, /top/anime and /anime?genres= from its database, which its indexers fill over days. This loads only
// the lists the sample's screens show: the current season, summer 2024, top airing, upcoming, and the top rated
// anime (for the Fantasy and Sci-Fi rails).

require '/app/vendor/autoload.php';

use Jikan\MyAnimeList\MalClient;
use Jikan\Request\Seasonal\SeasonalRequest;
use Jikan\Request\Top\TopAnimeRequest;
use Symfony\Component\HttpClient\HttpClient;

$http = HttpClient::create(['timeout' => 60]);
$mal = new MalClient($http);

/** The anime ids linked from a MyAnimeList page, in page order, when the parser cannot read the page. */
function idsFromPage($http, string $url): array
{
    $html = $http->request('GET', $url)->getContent();
    preg_match_all('#myanimelist\.net/anime/(\d+)/#', $html, $matches);
    return array_values(array_unique(array_map('intval', $matches[1])));
}

/** The most followed anime of a season (all of MyAnimeList's seasonal page, continuing shows included). */
function season($mal, $http, ?int $year, ?string $season, int $count): array
{
    $request = new SeasonalRequest($year, $season);
    try {
        $anime = $mal->getSeasonal($request)->getAnime();
        usort($anime, fn($a, $b) => $b->getMembers() <=> $a->getMembers());
        $ids = array_map(fn($a) => $a->getMalId(), $anime);
    } catch (\Throwable $e) {
        fwrite(STDERR, "The parser could not read {$request->getPath()} ({$e->getMessage()}); reading its links\n");
        $ids = idsFromPage($http, $request->getPath());
    }
    return array_slice($ids, 0, $count);
}

/** One of MyAnimeList's top lists, 50 entries per page. */
function top($mal, $http, ?string $type, int $pages): array
{
    $ids = [];
    for ($page = 1; $page <= $pages; $page++) {
        $request = new TopAnimeRequest($page, $type);
        try {
            foreach ($mal->getTopAnime($request)->getResults() as $item) {
                $ids[] = $item->getMalId();
            }
        } catch (\Throwable $e) {
            fwrite(STDERR, "The parser could not read {$request->getPath()} ({$e->getMessage()}); reading its links\n");
            $ids = array_merge($ids, idsFromPage($http, $request->getPath()));
        }
        sleep(2);
    }
    return $ids;
}

$lists = [];
$lists['current season'] = season($mal, $http, null, null, 60);
sleep(2);
$lists['summer 2024'] = season($mal, $http, 2024, 'summer', 60);
sleep(2);
$lists['top airing'] = array_slice(top($mal, $http, 'airing', 1), 0, 30);
$lists['upcoming'] = array_slice(top($mal, $http, 'upcoming', 1), 0, 30);
$lists['top rated'] = top($mal, $http, null, 4);

$all = [];
foreach ($lists as $name => $ids) {
    fwrite(STDERR, sprintf("%-15s %3d ids\n", $name, count($ids)));
    if (count($ids) === 0) {
        fwrite(STDERR, "No ids for $name\n");
        exit(1);
    }
    $all = array_merge($all, $ids);
}
// Fullmetal Alchemist: Brotherhood, the detail scene.
$all[] = 5114;

echo implode("\n", array_values(array_unique($all))), "\n";
