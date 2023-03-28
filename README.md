# MapSync

Share map data with your friends, live, privately.
Supports Journeymap and Voxelmap.

## Community

### [Join the Discord for announcements, discussion, and support.](https://discord.gg/khMPvWjnKt)

## Downloads

### [Find the latest downloads here!](https://github.com/Protonull/MapSync/releases/latest)
## What are you looking for?

### [Read more about the server here.](https://github.com/Protonull/MapSync/blob/stable/server/README.md)

### [Read more about the client mod here.](https://github.com/Protonull/MapSync/blob/stable/mod/README.md)

### [Read more about the renderer here.](https://github.com/Protonull/MapSync/blob/stable/mod/README.md)

## How it works

When you connect, you'll receive all chunks that your friends have mapped since the last time you played (and were connected to the MapSync server).

Every time any of your friends load a chunk with MapSync installed (even if they don't use any map mods!), it gets mapped and the map data gets sent to the MapSync server. It will then send it to everyone else, and if you have a compatible map mod installed (Journeymap or Voxelmap), the mod will display your friends' chunks.

Map-Sync tracks a timestamp per chunk, so old data will never overwrite newer data.

## Development

This project uses [Just](https://github.com/casey/just) for easy commands between the different subprojects.

---

Copyright (C) 2022 Map-Sync contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
