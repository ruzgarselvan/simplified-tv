# Simplified TV

A calmer home screen for Android TV and Google TV: a glass dock with your apps, a row of the films
trending this week, and round buttons for options, settings and HDMI inputs. It also comes with an
optional debloat that switches off the TV maker's promo packages, nothing uninstalled.

<p>
  <img src="docs/screenshots/home.jpg" width="49%" alt="The home screen: movies row and app dock">
  <img src="docs/screenshots/movies.jpg" width="49%" alt="The focused movie takes half the row">
  <img src="docs/screenshots/summary.jpg" width="49%" alt="OK on a movie opens its summary">
  <img src="docs/screenshots/inputs.jpg" width="49%" alt="The inputs button opens HDMI sources to its left">
</p>

Works on any Android TV or Google TV with ADB debugging, whatever the brand (Android 9 and newer).

## Install

### 1. Connect to your TV

The computer and the TV must be on the same Wi-Fi. The TV's IP address is in Settings → Network &
Internet → your network.

On the TV:

1. Settings → System → About: tap **Build** (or *Android TV OS build*) seven times until it says you
   are a developer.
2. Settings → System → **Developer options**: turn on **USB debugging**.
3. Then turn on one of these (your TV has one or both):
   - **Network debugging** (also called *ADB debugging over network*): the TV listens on port 5555.
     ```sh
     adb connect <tv-ip>:5555
     ```
   - **Wireless debugging** (Android 11 and newer): open it, choose **Pair device with pairing code**.
     The TV shows an address with a pairing port and a six-digit code:
     ```sh
     adb pair <tv-ip>:<pairing-port> <code>
     adb connect <tv-ip>:<port>      # the port on the main Wireless debugging screen, not the pairing port
     ```
4. The TV asks **Allow USB debugging?**: tick *Always allow* and accept.
5. `adb devices` should list the TV as `device`.

The connection drops when the TV sleeps or when you switch the setting off and on again (Wireless
debugging picks a new port every time): connect again. Install `adb` with
`brew install android-platform-tools` (macOS) or your package manager.

### 2. Install

**Option 1: with Claude Code**

```sh
git clone https://github.com/ruzgarselvan/simplified-tv
cd simplified-tv
export TMDB_TOKEN=<your read access token>     # optional, see below
claude
```

Then tell it: **"Install Simplified TV on my TV at 192.168.1.50."** Claude Code reads
[`AGENTS.md`](AGENTS.md), installs what is missing (a JDK, the Android SDK), builds the app, asks
before it changes anything on the TV, and never prints your token.

*The movies row needs a TMDB token.* Make a free account at themoviedb.org, then Settings → API and
copy the **API Read Access Token**. Exporting it in your own terminal before `claude` keeps it out of
the chat. Without a token everything works except that row.

**Option 2: by hand**

You need a JDK 17 and the Android SDK (`platforms;android-28`, `build-tools`):

```sh
brew install openjdk@17
brew install --cask android-commandlinetools
sdkmanager "platforms;android-28" "build-tools;34.0.0"
```

```sh
TMDB_TOKEN=<read access token> ./launcher/install.sh      # the token is optional
```

`install.sh` builds the app, installs it and makes it the home screen. The token is stored on the TV
only, never in the app or this repo. On Linux set `JAVA_HOME` and `ANDROID_HOME`. Keep
`launcher/release.keystore` (it is made on the first build): updates must be signed with the same key.

*Typeface:* the original look uses Satoshi (free from fontshare.com). Put `Satoshi-Variable.ttf` in
`launcher/assets/` before building; without it the TV's own font is used.

## Languages

**Options → Language**: *System* (follows the TV), *Türkçe* or *English*. The choice also sets the
language of the movie titles, genres and summaries (a film with no Turkish translation falls back to
English).

## Debloat (optional)

```sh
./scripts/discover.sh               # read-only: what is on this TV, and what could go
./scripts/debloat.sh --dry-run      # what would be switched off, and what is protected
./scripts/debloat.sh                # asks, then does it
```

Only packages listed in `data/androidtv/packages.txt` are touched, and only disabled for the user.
Anything essential (the framework, TV input and tuner stack, remote and Bluetooth, home screens,
setup, installers), anything that holds the home screen or the keyboard, and anything that owns saved
Wi-Fi networks is skipped. If the TV ends up without a home screen, it is restored at once.

## Undo

`./scripts/restore.sh` turns the disabled packages back on. `./launcher/install.sh --undo` gives the
home screen back to the TV's own (and switches that on if it had been off).
`adb uninstall tv.dumbtv.home` removes the app.

## Credits

MIT. Forked from [humblemuzzu/dumbtv](https://github.com/humblemuzzu/dumbtv) and cut down to Android
TV and Google TV. Movie data and images come from [TMDB](https://www.themoviedb.org); this product uses
the TMDB API but is not endorsed or certified by TMDB.
