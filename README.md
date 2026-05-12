# ZAntiCheat
A lightweight and customizable anticheat, designed to detect common hacks.<br>
Supported MC versions: 1.8-1.21. Folia, Geyser and [other plugins](F-A-Q.md) are also compatible.

## Links
* [SpigotMC page](https://www.spigotmc.org/resources/zanticheat.112053/)
* [PaperMC page](https://hangar.papermc.io/Vekster/ZAntiCheat)
* [Modrinth page](https://modrinth.com/plugin/zanticheat)
* [Discord server](https://discord.gg/EQExhK8Ghm)
* List of checks: [CHECKS.md](CHECKS.md)
* Frequently asked questions: [F-A-Q.md](F-A-Q.md)
* More precise config: [MILKv2's config](https://github.com/MILKv2/zanticheat-config)

## You want to contribute?
Check out the [CONTRIBUTING.md](CONTRIBUTING.md) file for more information.

## Documentation
### Features:
* Accurate detection with rare false positives
* Multithreaded, optimized and stable code
* No additional libraries or plugins are required
* Most of the checks are compatible with Geyser
* ZAntiCheat can be installed on a server that runs Folia
* Supports Discord webhook for notifications
* Provides convenient utils for moderators

### Commands:
* /zac reload - reloads the plugin configuration
* /zac teleport - teleports to the flag location
* /zac checks - shows all the enabled and disabled checks
* /zac alerts - toggles alerts on and off
* /zac tps - shows the TPS calculated by this plugin
* /zac client - shows player's client brand
* /zac ping - shows player's ping and connection stablity
* /zac cps - shows player's CPS

### Permissions:
* zanticheat.checks - use /zac checks command
* zanticheat.reload - use /zac reload command
* zanticheat.alerts - grants all the alert permissions
* zanticheat.alerts.notify - see debug messages
* zanticheat.alerts.toggle - use /zac alerts command
* zanticheat.alerts.teleport - use /zac teleport command
* zanticheat.tps - use /zac tps command
* zanticheat.ping - use /zac ping command
* zanticheat.client - use /zac client command
* zanticheat.cps - use /zac cps command
* zanticheat.bypass - bypass the detection<br>
Specific bypass permissions can be enabled in the config
* zanticheat.* - all the above

## Maven/Gradle
You can add ZAntiCheat's API as a Maven dependency:
````xml
<dependency>
  <groupId>cn.jeyor1337</groupId>
  <artifactId>zanticheat</artifactId>
  <version>0.0.1</version>
</dependency>
````
Or use the maven dependency with Gradle:
```gradle
dependencies {
    compileOnly 'cn.jeyor1337:zanticheat:0.0.1'
}
```
