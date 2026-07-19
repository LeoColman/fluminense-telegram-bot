# Fluminense Telegram Bot

A Telegram bot for Fluminense supporters. It shouts back torcida chants and pulls
live match info (next game, last result, league table, likely starting XI) from
public sources.

## Commands

### Torcida (chants)

Each of these replies with a chant or cheer. No external data, just pure Nense energy.

| Command | Reply |
| --- | --- |
| `/nense` | `NENSE!` followed by a random torcida chant |
| `/fabio` | A cheer for goalkeeper Fábio |
| `/vence` | Victory cheer plus a photo |
| `/louco` | The "Louco da cabeça" chant |
| `/libertadores` | The Libertadores chant |
| `/bencao` | The "A bênção, João de Deus" chant |
| `/xerem` | A shout-out to the Xerém youth academy |

### Match info

| Command | Reply |
| --- | --- |
| `/quando` | Date/time of the next match in any competition, plus where it airs when known, e.g. `Sáb 26/07 às 18:30 vs Grêmio (Brasileirão)` + `📺 Premiere, SporTV` |
| `/resultado` | Last match with the score and how long ago, e.g. `Fluminense 1 x 1 Bragantino (Brasileirão)` |
| `/tabela` | Current Brasileirão Série A standings |
| `/elenco` | Probable starting XI (the most recent match lineup) with shirt number, position and formation |

## Data sources

The bot scrapes public data. Nothing here needs a paid key.

- **[TheSportsDB](https://www.thesportsdb.com/)** feeds `/quando`, `/resultado` and `/tabela`.
- **[ge.globo.com](https://ge.globo.com/)** feeds the broadcast channel in `/quando` and the lineup in `/elenco`.
- **[fluminense.com.br](https://www.fluminense.com.br/)** provides the official squad, used to tell the Fluminense lineup apart from the opponent's in `/elenco`.

Some data is only available close to kickoff. `/quando` shows `canal ainda não anunciado`
when the broadcaster has not been published yet (usually more than a couple of days out),
and `/elenco` falls back to a friendly message when no recent lineup is available.

Results are cached in memory (30 minutes for match data, 3 hours for the lineup) to avoid
hammering the sources.

## Configuration

Environment variables:

| Variable | Required | Description |
| --- | --- | --- |
| `TELEGRAM_TOKEN` | yes | The Telegram bot token |
| `THESPORTSDB_KEY` | no | TheSportsDB API key. Defaults to the free public key `123` when unset |

## Running

With Docker Compose (pulls the published image from Docker Hub):

```bash
TELEGRAM_TOKEN=xxxx docker compose up
```

To build the image locally instead (the Dockerfile runs the Gradle build inside a
JDK 21 container, so no host JDK is needed):

```bash
docker build -t fluminense-telegram-bot .
docker run -e TELEGRAM_TOKEN=xxxx fluminense-telegram-bot
```

Or build and run the fat jar directly:

```bash
./gradlew installShadowDist
TELEGRAM_TOKEN=xxxx ./build/install/fluminense-telegram-bot-shadow/bin/fluminense-telegram-bot
```

## Build and test

Kotlin on Gradle (wrapper pinned, no local Gradle needed); the Docker build compiles
inside a JDK 21 image, so no host JDK is required to ship it.

```bash
./gradlew test              # run the Kotest suite
./gradlew installShadowDist # build the runnable distribution
```
