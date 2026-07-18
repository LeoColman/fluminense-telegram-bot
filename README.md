

### Commands

`/quando` -> Responds with the date/time of Fluminense's next match in any competition tracked by TheSportsDB, e.g. `Hoje daqui 2h às 13:00 vs Grêmio (Brasileirão)`.

### Docker
Environment:

`TELEGRAM_TOKEN` -> The telegram bot private token

`THESPORTSDB_KEY` -> Optional [TheSportsDB](https://www.thesportsdb.com/) API key used by `/quando`. Defaults to the free public key `123` when unset.