# AGENTS.md: installing Simplified TV on someone's Android TV

You are about to change someone else's television. Keep it short and friendly, say what you are about
to do before you do it, and let them do the taps on the TV. One TV at a time: set `ADB_SERIAL` when
`adb devices` lists several.

## Order

1. **Ask** for the TV's IP address if they did not give it.
2. **Tools:** `adb`; for the home screen also a JDK 17 and the Android SDK (`platforms;android-28`,
   `build-tools`). Check what is already there and install only what is missing. The macOS `temurin`
   cask asks for a password you cannot type: have them run `! brew install --cask temurin`, or use `brew install openjdk@17`.
3. **On the TV** (they do it): Settings → System → About, tap *Build* seven times; Developer options →
   USB debugging, then either *Network debugging* (port 5555: `adb connect <ip>:5555`) or *Wireless
   debugging* (Android 11+: *Pair device with pairing code*, then `adb pair <ip>:<pairing-port> <code>` and
   `adb connect <ip>:<port>` with the port from the main Wireless debugging screen). They accept
   *Allow USB debugging?* and tick *Always allow*. adb over Wi-Fi drops (the TV sleeps; Wireless
   debugging changes port when toggled): reconnect and retry. Check with `adb devices`.
4. **Look first:** `./scripts/discover.sh` changes nothing. Show them the vendor packages it found.
5. **Debloat:** `./scripts/debloat.sh --dry-run`, show the list, and ask before the real run. Never run it
   without a yes. Add a package to `data/androidtv/packages.txt` only after the TV has worked without it.
6. **Home screen:** `./launcher/install.sh`. The movies row needs the user's TMDB *API Read Access Token*
   (themoviedb.org → Settings → API). It is theirs and secret. If `$TMDB_TOKEN` is already in the
   environment the installer uses it: never echo it. If it is not, ask whether they want the movies row; if
   yes, they export `TMDB_TOKEN` in their own terminal and start a new session, or run the installer
   themselves. If they paste it into the chat anyway, use it only in that one command and never repeat it.
   Never write it in a file in this repo, never commit it. Keep `launcher/release.keystore`: updates must
   be signed with the same key.
7. **Tell them the way back:** `./scripts/restore.sh` and `./launcher/install.sh --undo`.

## Rules

- Never touch `/system`, never factory-reset, never `pm uninstall` or `pm clear` a system app: only
  `pm disable-user --user 0`, through `scripts/debloat.sh`.
- Never disable anything the scripts protect, and never remove an app they said they watch.
- Never turn off an app that owns saved Wi-Fi networks (`debloat.sh` checks).
- Do not change the TV's picture, sound, network or account settings.
- Keep identifiers (IPs, serials, MACs, account names, Wi-Fi names) and `report/` out of the repo.
- Never commit `*.keystore`, `dumbtv.conf`, `home.conf` or any key.
- Keep the way back working: `restore.sh` and `install.sh --undo`.

## The code

Plain Java and bash, no Gradle, no libraries. `launcher/build.sh` must end with `built build/dumbtv-home.apk`.
Scripts stay bash 3.2 compatible (macOS). On-screen text goes through `Tr.t`, Turkish in `Tr.java`;
comments are short and in English. Colors and the glass live in `launcher/src/tv/dumbtv/home/Ui.java`.
