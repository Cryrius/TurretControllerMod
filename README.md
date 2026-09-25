# Turret Controller

A standalone Minecraft 1.7.10 Forge mod that adds a high-control turret controller and an optional reflective adapter for [HBM's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT).

## Design goals

* **No hard HBM dependency.** HBM is not on `build.gradle`'s compile, runtime, or packaged dependency paths. The project compiles with Forge/Minecraft alone and the jar loads when HBM is absent.
* **Reflection at the boundary.** `HbmReflection` is the only compatibility class. It identifies turret tile entities by runtime names and accesses optional fields and methods with reflection. There are no `com.hbm.*` imports or link-time references in this project.
* **Server-authoritative control.** GUI actions are small validated packets, and controller policy is evaluated on the server.
* **Version-tolerant failure.** If an HBM field changes, that field is skipped instead of preventing the controller mod from loading. A missing or replaced target can be repaired by linking a new turret.

## Features

* Turret Controller block with persistent owner, link, enable state, range, target mode, and redstone policy.
* Turret Linker item: right-click a supported HBM turret, then right-click a controller.
* Hostile mobs, all living entities, players, animals, or mob-only target filters.
* Nearest-target selection within an independent 8–96 block controller range.
* Redstone modes: ignored, powered-to-run, and unpowered-to-run.
* Native turret target lock, enable flag, target-category flags, and live telemetry when those HBM members are available.
* Dedicated-server-safe GUI and no client-only classes on the common/server loading path.
* Missing HBM is a supported state, not an error: the controller and linker simply report that no compatible target is present.

## Building

This is a ForgeGradle 1.2 project for Forge `1.7.10-10.13.4.1614-1.7.10`.

```text
./gradlew setupDecompWorkspace
./gradlew build
```

The output is written to `build/libs/TurretControllerMod-1.0.0.jar`. No HBM checkout, jar, or source tree is required for either command. Java 7/8 is recommended for the original 1.7.10 toolchain.

## Using it in a pack

1. Install Forge 1.7.10 and place the built jar in `mods`.
2. Place a Turret Controller and hold the Turret Linker.
3. Right-click an HBM turret, then right-click the controller.
4. Open the controller to change policy. Sneak-right-click toggles the controller quickly.

The adapter currently targets HBM's `TileEntityTurretBaseNT` family through runtime reflection. It does not copy or embed HBM code and does not make the controller a subclass of an HBM class.
