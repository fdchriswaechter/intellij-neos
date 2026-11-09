IntelliJ IDEA / PhpStorm Neos Plugin
====================================
[![Build](https://github.com/cvette/intellij-neos/workflows/Build/badge.svg)](https://github.com/cvette/intellij-neos/actions?query=workflow%3ABuild)
[![JetBrains plugins](https://img.shields.io/jetbrains/plugin/d/9362-neos-support.svg)](https://plugins.jetbrains.com/plugin/9362-neos-support)

<!-- Plugin description -->
This plugin provides support for the Neos Fusion language, featuring configurable syntax highlighting, brace matching, breadcrumb navigation, and code folding. You can also leverage "Ctrl+Click" navigation to EEL helper references and prototype definitions, as well as find usages of prototypes and refactor prototype names with ease.

Additionally, this plugin supports node type definitions, offering completion for configuration keys and enabling "Go to definition" for node types, supertypes, and constraints.
<!-- Plugin description end -->

Usage
-----
You can install the plugin by going to `Settings > Plugins > Browse repositories` and searching for "Neos".

Custom build (fork)
-------------------
This fork contains additions that are not (yet) part of the official plugin:

- Signal/slot references: navigate from `$dispatcher->connect(...)` calls to the signal and slot methods
- Controller actions and CLI commands are no longer marked as unused, also when the controller extends a project specific base controller
- Magic repository finders (`findBy*`, `findOneBy*`, `countBy*`) are no longer reported as undefined methods and have proper return types (entity, `QueryResultInterface`, `int`)

### Installation
1. Download the latest `Intellij Neos-<version>-custom.<n>.zip` from the [releases of this fork](https://github.com/fdchriswaechter/intellij-neos/releases). Do not unpack it.
2. In PhpStorm open `Settings > Plugins`, click the gear icon and choose `Install Plugin from Disk...`.
3. Select the downloaded zip and restart PhpStorm.

The custom build requires PhpStorm 2026.3. It uses the same plugin ID as the official "Neos Support" plugin and replaces it. When the official plugin publishes a newer version, PhpStorm offers it as an update; installing that update removes the fork's additions.

### Building it yourself
```shell
./gradlew buildPlugin -PpluginVersion=1.28.0-custom.1
```
The zip is written to `build/distributions/`.

Contributing
------------
See [CONTRIBUTING.md](CONTRIBUTING.md)

License
-------
Copyright (C) 2016  Christian Vette

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
