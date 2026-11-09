# RivalSMP

A lightweight Paper server utility plugin.

## Included features
- Configurable join, quit, and welcome messages
- `/rivalsmp help`
- `/rivalsmp info`
- `/rivalsmp reload`
- `/announce <message>`
- Permission checks

## Requirements
- Paper server
- Java 21
- Maven (only for local builds; GitHub Actions can build it for you)

## Build with GitHub Actions
1. Upload the complete project to a GitHub repository, preserving the folder structure.
2. Open the **Actions** tab.
3. Select **Build RivalSMP** and run it, or push a commit.
4. Open the successful workflow run.
5. Download the artifact named `RivalSMP-1.21.1-to-1.21.11`.
6. Put the downloaded JAR in the server's `plugins` folder and restart the server.

The workflow checks that both `plugin.yml` and `RivalSMP.class` are inside the built JAR.

## Compatibility note
This project compiles against Paper 1.21.1 API and targets Java 21. Forward compatibility with every later Paper release, including 1.21.11, is not guaranteed; test on your exact server build.

## Commands
- `/rivalsmp help`
- `/rivalsmp info`
- `/rivalsmp reload` (admin)
- `/announce <message>` (admin)

## Permissions
- `rivalsmp.use` — default: true
- `rivalsmp.admin` — default: op
