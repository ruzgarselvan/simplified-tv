# dumbtv

Two things for an Android TV or Google TV, made to be run from a computer over adb:

- **A quiet home screen** ([`launcher/`](launcher/README.md)): a glass dock with your apps and a row
  of what is trending this week on TMDB. Plain Java, no libraries.
- **A debloat** ([`scripts/`](scripts/)): switches off the maker's promo, demo and feedback packages
  to free memory and stop background services. Nothing is uninstalled; every package is only
  disabled for the user, and one command turns them all back on.

Works on any Android TV or Google TV with ADB debugging, whatever the brand: Grundig, TCL, Philips,
Sony, Xiaomi, onn and the rest. The home screen runs on Android 9 and newer.

<p>
  <img src="docs/screenshots/home.jpg" width="49%" alt="The home screen: movies row and app dock">
  <img src="docs/screenshots/movies.jpg" width="49%" alt="The focused movie takes half the row">
  <img src="docs/screenshots/summary.jpg" width="49%" alt="OK on a movie opens its summary">
  <img src="docs/screenshots/inputs.jpg" width="49%" alt="The inputs button opens HDMI sources to its left">
</p>

## Use it

```sh
adb connect <tv-ip>:5555            # accept "Allow USB debugging?" on the TV, tick "Always allow"

./scripts/discover.sh               # read-only: what is on this TV, and what could go
./scripts/debloat.sh --dry-run      # what would be switched off, and what is protected
./scripts/debloat.sh                # asks, then does it

TMDB_TOKEN=<read access token> ./launcher/install.sh   # the home screen (the token is optional)
./launcher/install.sh --smarttube   # optional: SmartTube, an ad-free YouTube client
```

Undo: `./scripts/restore.sh` turns the packages back on, `./launcher/install.sh --undo` gives the
home screen back. The first thing `debloat.sh` checks, after every run, is that the TV still has a
home screen; if not, it restores at once.

## What the debloat will not touch

It only switches off packages listed in `data/androidtv/packages.txt`, and it skips a listed package
that is on the never-disable list or looks essential (the framework, TV input and tuner stack,
remote and Bluetooth, home screens, setup, installers), that holds the home screen, the keyboard or
an accessibility service, or that owns saved Wi-Fi networks, because turning those off makes
Android delete the networks. A package that is not installed is ignored, so one list serves every
brand.

A new TV starts with `discover.sh`. Its report is a set of hints, never a verdict: switch a package
off on your own TV, see that it still works, and only then add it to `packages.txt`.

## Needs

`adb`, and for the home screen a JDK 17 and the Android SDK; see [`launcher/README.md`](launcher/README.md).
MIT, forked from [humblemuzzu/dumbtv](https://github.com/humblemuzzu/dumbtv).
