export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { headers: cors() });
    if (url.pathname !== "/fixtures" || request.method !== "GET") {
      return json({ error: "Not found" }, 404);
    }
    if (!env.API_FOOTBALL_KEY) return json({ error: "API_FOOTBALL_KEY secret is not configured" }, 503);
    const date = url.searchParams.get("date");
    if (!date || !/^\d{4}-\d{2}-\d{2}$/.test(date)) return json({ error: "date must be YYYY-MM-DD" }, 400);
    const headers = { "x-apisports-key": env.API_FOOTBALL_KEY };
    const base = "https://v3.football.api-sports.io";
    try {
      const [fixturesRes, oddsRes] = await Promise.all([
        fetch(base + "/fixtures?date=" + date + "&timezone=Europe%2FIstanbul", { headers }),
        fetch(base + "/odds?date=" + date, { headers })
      ]);
      if (!fixturesRes.ok || !oddsRes.ok) return json({ error: "Football data provider request failed", providerStatus: [fixturesRes.status, oddsRes.status] }, 502);
      const fixturesData = await fixturesRes.json();
      const oddsData = await oddsRes.json();
      const oddsByFixture = new Map();
      for (const entry of oddsData.response || []) {
        const id = entry.fixture && entry.fixture.id;
        if (!id) continue;
        const list = [];
        for (const bookmaker of entry.bookmakers || []) {
          for (const bet of bookmaker.bets || []) {
            for (const value of bet.values || []) {
              const odd = Number(value.odd);
              if (Number.isFinite(odd) && odd > 1) {
                list.push({
                  bookmaker: bookmaker.name || "Bookmaker",
                  market: (bet.name || "Market") + " · " + (value.value || ""),
                  value: odd
                });
              }
            }
          }
        }
        oddsByFixture.set(String(id), list);
      }
      const events = (fixturesData.response || []).map(item => ({
        idEvent: String(item.fixture.id),
        strLeague: item.league.name + " · " + item.league.country,
        strHomeTeam: item.teams.home.name,
        strAwayTeam: item.teams.away.name,
        dateEvent: item.fixture.date.slice(0, 10),
        strTime: new Date(item.fixture.date).toLocaleTimeString("tr-TR", { timeZone: "Europe/Istanbul", hour: "2-digit", minute: "2-digit" }),
        odds: oddsByFixture.get(String(item.fixture.id)) || []
      }));
      return json({ events, source: "API-Football", date });
    } catch (error) {
      return json({ error: "Unable to retrieve fixtures and odds", detail: String(error) }, 502);
    }
  }
};
function cors() {
  return { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "GET, OPTIONS", "Access-Control-Allow-Headers": "Content-Type" };
}
function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { ...cors(), "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" } });
}
