# Releasing

All four artifacts — `dca-building-blocks`, `dca-archunit`, `dca-spring`, `dca-archunit-modulith` — go to
Maven Central (Sonatype Central Portal) under the namespace `dev.domaincentric`, independently versioned,
one tag per artifact (`building-blocks/v…`, `archunit/v…`, `spring/v…`, `archunit-modulith/v…`).

**Publishing runs on the maintainer's machine**, via [scripts/release.sh](scripts/release.sh). The GPG
signing key stays there — CI never sees it. GitHub Actions builds and tests every push, and on a release
tag it verifies the version is on Central and creates the GitHub release
([release.yml](.github/workflows/release.yml)).

## One-time setup

### 1. Central Portal namespace

1. Account on [central.sonatype.com](https://central.sonatype.com).
2. Register the namespace `dev.domaincentric`, verify it with the `TXT` record the Portal shows on the
   DNS zone of `domaincentric.dev`.
3. Generate a **user token** (Account → Generate User Token) — a username/password pair, both random
   strings. The Portal login credentials do not work for publishing. If the Portal shows the token as a
   `settings.xml` snippet, its `<username>`/`<password>` are the two values.

The token is deliberately **not** kept in `~/.gradle/gradle.properties`: that file is world-readable
plaintext for every process running as you. `scripts/release.sh` looks for it in three places, in order.

**Keychain (recommended).** Stored once as `username:password`, unlocked per access by macOS:

```bash
security add-generic-password -s dca-maven-central -a central-portal -w
# paste: <token username>:<token password>
security find-generic-password -s dca-maven-central -w    # check
```

Add `-T ""` to the `add` call to force a confirmation dialog on every read, or
`security delete-generic-password -s dca-maven-central` to remove it again.

**Environment**, for one shell session:

```bash
read -r  -p 'token username: ' u && read -rs -p 'token password: ' p && echo
export ORG_GRADLE_PROJECT_mavenCentralUsername="$u" ORG_GRADLE_PROJECT_mavenCentralPassword="$p"
unset u p
```

Do not put these in `.zshrc` — that is the same plaintext problem with a different filename.

**Prompt.** With neither of the above, the script asks for both values and keeps them in its own process
only.

### 2. GPG key

Central signs nothing itself: every file is signed locally and validated against public keyservers, so
the **public** key must be published. The private key never leaves the machine.

```bash
gpg --full-generate-key                     # RSA 4096, long or no expiry
gpg --list-secret-keys --keyid-format=long  # note the fingerprint
gpg --keyserver keys.openpgp.org     --send-keys <FINGERPRINT>
gpg --keyserver keyserver.ubuntu.com --send-keys <FINGERPRINT>
```

`keys.openpgp.org` strips the user id until the email address is confirmed, and `--send-keys` triggers
no confirmation mail. To get one, upload the exported public key through
<https://keys.openpgp.org/upload> and confirm the mail. Verify what the servers actually serve:

```bash
gpg --keyserver keys.openpgp.org --recv-keys <FINGERPRINT>   # must name the user id, not "no user ID"
```

The current signing key is `6CE5EF6C96B86413FC5E1F17C6773EDA846201AB` (RSA 4096, expires 2030-08-31).
`scripts/release.sh` uses it by default; `DCA_SIGNING_KEY=<fingerprint>` overrides it.

## Releasing an artifact

```bash
./scripts/release.sh building-blocks 0.1.0     # or: archunit 0.1.0 · spring 0.1.0 · archunit-modulith 0.1.0
```

The script refuses to continue unless everything is in order, then asks for the GPG passphrase, builds,
signs, checks the signatures locally, asks for a typed confirmation and uploads:

1. clean working tree, tag not taken, `## [0.1.0]` section present in that artifact's `CHANGELOG.md`,
   signing key in the keyring
2. for `dca-archunit` and `dca-spring`: `buildingBlocksVersion` in [gradle.properties](gradle.properties)
   names a released version; for `dca-archunit-modulith`: `archunitVersion` does — it becomes the
   dependency version in the published POM. Release order therefore: building blocks, then archunit
   and spring, then archunit-modulith
3. Central token from environment, keychain or prompt ([scripts/lib/central-token.sh](scripts/lib/central-token.sh),
   shared with `publish-snapshot.sh`); GPG passphrase prompt (the passphrase reaches
   `gpg` on stdin, never as an argument)
4. `build` of that subproject only
5. `publishToMavenLocal` and a count of at least four `.asc` files in `~/.m2`
6. `publishToMavenCentral` — uploads and polls until Central reports `VALIDATED`. The deployment is
   **not** released: it waits for "Publish" on the
   [Portal](https://central.sonatype.com/publishing/deployments), so a bad build can still be dropped

Afterwards, as the script prints:

1. Check the deployment on the [Portal](https://central.sonatype.com/publishing/deployments) and click
   **Publish** — or **Drop**, which throws the deployment away and frees the version number again.
2. Wait until `https://repo1.maven.org/maven2/dev/domaincentric/<artifact>/<version>/` answers.
3. Tag it:

   ```bash
   git tag building-blocks/v0.1.0
   git push origin building-blocks/v0.1.0
   ```

The tag comes **after** the release, so the workflow finds the artifact on Central instead of waiting for
a publish that has not happened. After a `dca-building-blocks` or `dca-archunit` release, set
`buildingBlocksVersion` / `archunitVersion` in `gradle.properties` to that version and commit — the
artifacts depending on it read their POM dependency from there. Central takes about ten minutes to serve
a published deployment; poll `repo1.maven.org` before tagging, or the workflow's wait runs out.

To release without the manual click, change `publishToMavenCentral()` in
[build.gradle.kts](build.gradle.kts) to `publishToMavenCentral(automaticRelease = true)`. Worth doing
once the first release has proven the setup; until then the click is the only chance to inspect what
Central validated.

## Manual equivalent

The script wraps these commands; nothing stops you from running them directly:

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=<token username>
export ORG_GRADLE_PROJECT_mavenCentralPassword=<token password>
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <FINGERPRINT>)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword='<passphrase>'

./gradlew :dca-building-blocks:publishToMavenCentral -PbuildingBlocksVersion=0.1.0
```

Every `ORG_GRADLE_PROJECT_<name>` variable becomes the Gradle property `<name>`, which is why the plugin
needs no configuration file at all. Gradle does not read `~/.m2/settings.xml` — a Maven token has to be
carried over by hand.

## Testing the token without releasing anything

A snapshot goes to the Central snapshot repository, not to Central, and can be repeated at will:

```bash
./scripts/publish-snapshot.sh                  # both artifacts, versions from gradle.properties
./scripts/publish-snapshot.sh dca-archunit     # one of them
./scripts/publish-snapshot.sh dca-building-blocks -PbuildingBlocksVersion=0.2.0-SNAPSHOT   # override
```

Every artifact that is actually requested must resolve to a `-SNAPSHOT` version — from
`gradle.properties` or from a `-P` override, which the script forwards to Gradle; it refuses otherwise,
before any credential is read. Both scripts take the artifact → project → version-property mapping from
[scripts/lib/artifacts.sh](scripts/lib/artifacts.sh), and both accept either spelling
(`building-blocks` / `dca-building-blocks`).

Snapshots must be enabled for the namespace first (Portal → the namespace's dropdown → *Enable
SNAPSHOTs*), otherwise the upload fails with `403 Forbidden` despite a verified namespace.
It resolves the token exactly like `release.sh` (environment, keychain, prompt — see
[scripts/lib/central-token.sh](scripts/lib/central-token.sh)) and needs no GPG key: snapshots are not
signed. A bare `./gradlew :dca-building-blocks:publishToMavenCentral` fails with missing credentials,
because Gradle reads neither the keychain nor `settings.xml`.

`401` means the username is wrong (Portal login instead of token); a namespace complaint means the DNS
verification of `dev.domaincentric` is still missing.

## Local checks without publishing

```bash
./gradlew publishToMavenLocal    # POM, sources jar, javadoc jar — signing off without a key
```

Signing is only wired in when a key is configured, so everyday local publishing and the sample's
composite build need no GPG. To rehearse a signed build:

```bash
gpgconf --kill all
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --export-secret-keys --armor <FINGERPRINT>)"
read -rs -p 'passphrase: ' ORG_GRADLE_PROJECT_signingInMemoryKeyPassword && echo
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword
./gradlew publishToMavenLocal
ls ~/.m2/repository/dev/domaincentric/dca-building-blocks/0.1.0-SNAPSHOT/*.asc
```

Pass secrets through the environment, not as `-P` arguments — arguments show up in `ps`.

## Snapshots

A version ending in `-SNAPSHOT` published with `publishToMavenCentral` lands in the
[Central Portal snapshot repository](https://central.sonatype.com/repository/maven-snapshots/)
immediately. Consumers must declare that repository explicitly.

## If publishing ever moves to CI

It would need `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE` as repo or
org secrets, mapped to `ORG_GRADLE_PROJECT_mavenCentralUsername`, `…Password`, `…signingInMemoryKey` and
`…signingInMemoryKeyPassword`. Export a dedicated **signing subkey** for that
(`gpg --export-secret-subkeys --armor <SUBKEY-ID>!`) rather than the primary key: a compromised runner
then costs a subkey revocation, not the identity.
