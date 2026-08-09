# DeploySync

A Windows desktop tool for patching and deploying updates to an Oracle ADF / WebLogic
trade-management application, packaged as a client `.ear` file. It replaces a manual
WinRAR/7-Zip + WebLogic-console workflow with a small, self-contained GUI app — no
CI/CD, no server-side install, nothing to trust but a folder you can copy anywhere.

Two people use this tool for two different jobs:

- **Dev team** — after a build, package the changed files into a deployable patch
  (a folder of files plus a manifest) using **Patch Management**.
- **Support team** — apply that patch to the client's live `.ear` using
  **EAR Deployment**.

## Download

Grab the latest release from the [Releases page](../../releases) — download
`DeploySync-vX.Y.Z-win64.zip`, extract it anywhere, and run `DeploySync.exe`. No
installer, no admin rights, no Java installation required — the app ships with its own
bundled runtime.

## Features

### Patch Management
Open a master archive (`.ear`/`.war`/`.jar`/`.zip`) and browse its contents — including
archives nested inside archives — in a lazy-loaded tree that only unpacks what you
expand. Select any files, extract them plus an auto-generated `manifest.json` into a
destination folder, ready to hand off as a patch package.

### EAR Deployment
Pick a master `.ear` and a folder of new/changed files, then apply the patch. Every
file is validated against the manifest before the live archive is touched. Deployment
runs on a background thread with live per-file progress and a clear success/failure
result, so the UI never freezes.

### Safety model
The patch engine (built directly on Java's `java.nio.file` zip filesystem support)
carries over the safety guarantees proven in the team's original PowerShell/Python
tooling:
- The live `.ear` is only modified after the **entire** patch batch validates — no
  partial writes.
- An automatic timestamped backup is taken before any change.
- Every patched entry is verified by **SHA-256 hash**, not just a "success" return code.
- A manifest entry that looks like a nested archive but doesn't resolve is a hard
  failure, never silently skipped.

## Requirements

- Windows 10/11 (64-bit). No other runtime dependencies — the released `.zip` bundles
  its own JVM.

## Architecture

Multi-module Maven reactor with the MVC boundary enforced by the module graph itself:

| Module              | Contains                                                        |
|---------------------|------------------------------------------------------------------|
| `deploysync-model`  | Spring `@Service` beans + domain records. Zero JavaFX dependency — unit-testable with plain JUnit. |
| `deploysync-view`   | FXML/CSS + Spring-managed FXML controllers (depends on `-model`). |
| `deploysync-app`    | Composition root: boots Spring (`web-application-type=none`, no embedded server) and launches the JavaFX `Application` (depends on `-view`). This is the module `jlink`/`jpackage` target. |

- **Stack**: Java 25 (LTS), Spring Boot 4.0.x used purely for DI/configuration (no open
  port), JavaFX for the UI wired to Spring via
  `FXMLLoader.setControllerFactory(springContext::getBean)`.
- **Pattern**: strict MVC — controllers read widgets, call a model service, write the
  result back; all validation/business logic lives in the model layer.
- **Packaging**: `jlink` builds a minimal custom JVM runtime, `jpackage --type app-image`
  produces the self-contained app folder, zipped for distribution. No installer by
  design — this must run on a locked-down server with nothing pre-installed.

## Building from source

Requires JDK 25 on your `PATH`.

```powershell
mvn clean install
```

To produce a release-ready, self-contained `DeploySync-v<version>-win64.zip` (runs
`mvn clean install`, detects required JDK modules, builds a custom runtime with
`jlink`, and packages + zips the app with `jpackage`):

```powershell
.\package.ps1
```

The output lands at `deploysync-app\target\dist\`.

## Known limitations

- WebLogic redeploy/restart is **not** integrated yet — after applying a patch via EAR
  Deployment, redeploy/restart WebLogic using the existing separate scripts, same as
  before.
- Windows only; no macOS/Linux build.
- No automated test suite yet — verified by manual end-to-end testing.

## License

[MIT](LICENSE) — see the `LICENSE` file for the full text.
