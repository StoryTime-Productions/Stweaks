# Hunt Game System

A comprehensive game system for managing Prop Hunt, Imposter Hunt, and NextBot Hunt game modes.

## Features

### Game Modes

- **Prop Hunt**: Hide as props and avoid detection
- **Imposter Hunt**: Find the imposters among us
- **NextBot Hunt**: Survive the relentless pursuit

### Teams

- **Hunters**: Find and eliminate the hiders
- **Hiders**: Hide and survive until time runs out

### Hunter Classes

- **Brute**: Slower movement but higher damage (0.8x speed, 1.5x damage)
- **Nimble**: Faster movement but lower damage (1.3x speed, 0.7x damage)
- **Saboteur**: Normal stats but can set traps (1.0x speed, 1.0x damage)

### Hider Classes

- **Trickster**: Can set counter-traps against hunters
- **Phaser**: Can phase through walls temporarily
- **Cloaker**: Can become invisible for limited time

### Maps

- **Warehouse**: Industrial setting with lots of hiding spots
- **Mansion**: Spooky mansion with multiple floors
- **Village**: Small village with houses and shops
- **Factory**: Active factory with machinery and conveyor belts
- **School**: Abandoned school with classrooms and hallways

## Commands

### /hunt

Opens the main Hunt game lobby interface

### /hunt join

Same as `/hunt` - opens the lobby interface

### /hunt leave

Leaves the Hunt game lobby and closes any open menus

### /hunt status

Shows your current lobby status including:

- Selected team
- Selected class
- Preferred map
- Preferred game mode
- Ready status

## How to Use

1. Use `/hunt` to open the Hunt game lobby
2. Select your team (Hunters or Hiders)
3. Choose your class based on your selected team
4. Pick your preferred map
5. Select the game mode you want to play
6. Click "Ready" when you've completed all selections
7. Wait for other players to ready up to start the game

## GUI Navigation

The lobby uses an intuitive inventory-based GUI system:

- Click items to make selections
- "Back" buttons return to the main menu
- Your current selections are displayed in real-time
- The ready button changes color based on your status

## Future Enhancements

The system is designed to be extensible for future features like:

- Random abilities (Blink Crystal, Hologram Decoy, Smoke Orb)
- Game session management
- Spectator mode
- Statistics tracking
- Custom game settings
