# dumbtv home

A home screen for Android TV and Google TV: your apps in a glass dock, and a row of what is
trending this week on TMDB. Plain Java and Android views, no libraries.

## Install

You need `adb`, a JDK 17 and the Android SDK (`platforms;android-28`, `build-tools`), and a TV
with ADB debugging on (Settings → System → About, tap *Build* seven times; then Developer options →
USB debugging, and Network debugging or Wireless debugging).

```sh
brew install openjdk@17 android-platform-tools
brew install --cask android-commandlinetools
sdkmanager "platforms;android-28" "build-tools;34.0.0"

adb connect <tv-ip>:5555          # accept "Allow USB debugging?" on the TV, tick "Always allow"
./launcher/install.sh             # builds, installs, makes it the home screen
```

On Linux set `JAVA_HOME` and `ANDROID_HOME`; the defaults are Homebrew's paths. `build.sh` makes
`launcher/release.keystore` the first time: keep it, because an update must be signed with the key
the installed one was signed with (`install.sh` tells you if it was not).

**Movies row.** Make a free TMDB account, copy its *API Read Access Token* (themoviedb.org →
Settings → API) and give it to the installer, which stores it on the TV only, never in the APK or
this repo:

```sh
TMDB_TOKEN=<token> ./launcher/install.sh
```

Without a token the row is simply not shown and the app makes no network connection.

**Undo.** `./launcher/install.sh --undo` takes HOME back and switches the TV's own launcher on if
it had been switched off. `adb uninstall tv.dumbtv.home` removes the app.

## Using it

| Remote | Does |
|---|---|
| OK on an app | open it |
| OK on a movie | open or close its summary |
| ◀ ▶ on the movie row | the focused movie takes half the row; the row keeps going, page after page |
| Hold OK, or ☰ Menu, on an app | Move, Rename, Hide, App info |
| Up from the top | the two round buttons: Options and Settings |
| Back on the home screen | jump to the first tile |

**Options** (the sun button) cycles each setting with ◀ ▶ or OK: language (system, Türkçe, English), accent color,
app names, clock format, background, atmosphere and wallpaper rotation. **YouTube app** appears once SmartTube is
installed (`./launcher/install.sh --smarttube`) and makes the YouTube tile open it. Movie titles are always English.

## The settings file

One plain text file on the TV: `/sdcard/Android/data/tv.dumbtv.home/files/home.conf`

```
language=auto             # auto tr en
accent=pink                # white steel blue cyan green amber red pink violet, or #RRGGBB
youtube=stock              # stock smarttube
clock=auto                 # auto 24h 12h
inputs=off                 # on off
background=black           # black graphite midnight image
apps.order=com.nuvio.app,com.stremio.one
apps.hide=com.android.tv.settings
tmdb_token=...             # the TMDB read access token; never commit it
```

Back it up with `adb pull`, restore it with `adb push`; hand edits apply the next time the home
screen comes back. For a wallpaper, push `wallpaper.jpg` into the same folder and set
`background=image`; several files go in a `wallpapers/` subfolder, with `rotate=` to pick when they
change (`visit`, `hourly`, `daily`, `daytime`).

## How it is made

- `Home` is the screen; `Carousel` is the movies row, `Grid` the dock, `Tile` a card drawn in one
  pass, `IconButton` the round buttons, `Backdrop` the blurred color field behind everything and
  `Ui` the colors and the glass.
- Every color comes from `Ui`; the glass is translucent white with a rim light and no live blur,
  which would cost a TV's GPU every frame.
- `Movies` asks TMDB for `trending/movie/week` (English), caches lists and images in the app's
  cache folder, and loads pictures only for the cards around the focus.
- The only permission is `INTERNET`, for that row.
