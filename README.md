<!-- Improved compatibility of back to top link: See: https://github.com/othneildrew/Best-README-Template/pull/73 -->

<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
<p align="center">
  <a href="https://github.com/StoryTime-Productions/Stweaks/graphs/contributors">
    <img src="https://img.shields.io/github/contributors/StoryTime-Productions/Stweaks.svg?style=for-the-badge" alt="Contributors">
  </a>
  <a href="https://github.com/StoryTime-Productions/Stweaks/network/members">
    <img src="https://img.shields.io/github/forks/StoryTime-Productions/Stweaks.svg?style=for-the-badge" alt="Forks">
  </a>
  <a href="https://github.com/StoryTime-Productions/Stweaks/stargazers">
    <img src="https://img.shields.io/github/stars/StoryTime-Productions/Stweaks.svg?style=for-the-badge" alt="Stargazers">
  </a>
  <a href="https://github.com/StoryTime-Productions/Stweaks/issues">
    <img src="https://img.shields.io/github/issues/StoryTime-Productions/Stweaks.svg?style=for-the-badge" alt="Issues">
  </a>
  <a href="https://github.com/StoryTime-Productions/Stweaks/blob/master/LICENSE.txt">
    <img src="https://img.shields.io/github/license/StoryTime-Productions/Stweaks.svg?style=for-the-badge" alt="License">
  </a>
</p>

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/StoryTime-Productions/Stweaks">
    <img src="https://i.imgur.com/zlGbFm5.png" alt="Logo" style="max-width: 100%; height: auto;">
  </a>

  <p align="center">
    A plugin introducing vanilla-based tweaks.<br/>
    <br />
    <a href="https://github.com/StoryTime-Productions/Stweaks/wiki"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/StoryTime-Productions/Stweaks">View Demo</a>
    &middot;
    <a href="https://github.com/StoryTime-Productions/Stweaks/issues/new?template=bug_report.md">Report Bug</a>
    &middot;
    <a href="https://github.com/StoryTime-Productions/Stweaks/issues/new?template=feature_request.md">Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->

## About The Project

**Stweaks** is a Minecraft plugin developed for <a href="https://papermc.io/">PaperMC</a> servers. This plugin introduces various silly custom items, mechanic modifications and additional add-ons to make playing Vanilla a bit more fun.

### Why Stweaks?

- We're looking to balance game time and promote more social gameplay through time-based mechanic implementations.
- We're looking to make our users laugh with inside-joke custom recipes that still add practical value to the game.
- We're looking to express ourselves both creatively through custom texturing, programming through datapacks, and scripting through plugin implementation.

Contributions, ideas, or feature requests are always welcome!

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

- [![Java 17](https://img.shields.io/badge/Java-17-007396?style=flat&logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
  — Core language used to develop the plugin.
- [![Spigot API](https://img.shields.io/badge/Spigot-API-FABF2D?style=flat&logo=minecraft&logoColor=black)](https://www.spigotmc.org/wiki/spigot-plugin-development/)
  — Minecraft server API used for plugin development.

- [![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat&logo=gradle&logoColor=white)](https://gradle.org/)
  — Build automation tool used for compiling and packaging.

- [![Spotless](https://img.shields.io/badge/Spotless-Format-4B32C3?style=flat&logo=prettier&logoColor=white)](https://github.com/diffplug/spotless)
  — Used for automatic code formatting during builds.

- [![Batch Script](https://img.shields.io/badge/Batch%20Script-Windows-4D4D4D?style=flat&logo=windows&logoColor=white)](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/windows-commands)
  — Windows batch scripting for automating plugin deployment.

- [![PowerShell](https://img.shields.io/badge/PowerShell-5391FE?style=flat&logo=powershell&logoColor=white)](https://learn.microsoft.com/en-us/powershell/)
  — Used for zipping assets and other deployment tasks.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->

## Getting Started

This is an example of how you may give instructions on setting up your project locally.
To get a local copy up and running follow these simple example steps.

### Prerequisites

To build and deploy ks locally, ensure the following tools are installed:

- [Java 21+](https://adoptium.net/en-GB/temurin/releases/)
  Required to compile and run the plugin.
- [Gradle](https://gradle.org/install/) _(or use the included `gradlew` wrapper)_  
  Used to build the project and apply formatting.

- [PowerShell 5.1+](https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell) _(for Windows users)_  
  Required for deployment zip compression.

- `.env` file in the project root  
  Must define:
  ```env
  SERVER_PATH=path\to\your\server
  RESOURCE_PATH=path\to\your\resourcepacks
  ```

### Installation

1. Clone the repository locally.

2. Assuming the current structure of the project, you may add-on functionality right away.

3. For resource pack implementation, you can refer to the `src/main/resources/st-respack` folder.

4. For datapack implementation, you can refer to the `src/main/resources'st-datapack` folder.

5. The `deploy.bat` script, located in root, can be used to update the plugin, resource pack, and datapack respectively, assuming correct paths are provided in the `.env` file.

6. Run the following command in your terminal to enable project-specific Git hooks:
   ```sh
   ./set-hooks-path.sh
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->

## Roadmap

- [x] Add Illegal Water
- [x] Add Lebron James armor set
- [x] Add FBI Disc
- [x] Add Cow Skinner
- [x] Add 1-hour Timer
- [ ] Add Nature Compass

See the [open issues](https://github.com/StoryTime-Productions/ks/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Top contributors:

<a href="https://github.com/StoryTime-Productions/Stweaks/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=StoryTime-Productions/Stweaks" alt="contrib.rocks image" />
</a>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- LICENSE -->

## License

See `LICENSE.txt` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->

## Contact

Nirav Patel - [@Niravanaa](https://github.com/Niravanaa) - niravp0703@gmail.com

StoryTime Productions: [Portfolio Link](https://storytime-productions.github.io/)

<p align="right">(<a href="#readme-top">back to top</a>)</p>
