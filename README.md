<p align="center">
  <img src="https://cdn.discordapp.com/attachments/816647374239694849/1082429620911099994/68747470733a2f2f692e696d6775722e636f6d2f465244794d78762e706e67.png">
</p>

<div style="display:inline-block">
  <p align="center">
    <img src="https://img.shields.io/github/v/release/qWojtpl/achievementsengine">
    <img src="https://img.shields.io/github/languages/top/qWojtpl/achievementsengine">
    <img src="https://img.shields.io/github/repo-size/qWojtpl/achievementsengine">
  </p>
</div>
<div style="display:inline-block">
  <p align="center">
    <img src="https://img.shields.io/github/downloads/qWojtpl/achievementsengine/latest/total">
    <img src="https://img.shields.io/github/commit-activity/m/qWojtpl/achievementsengine">
    <img src="https://img.shields.io/github/commits-since/qWojtpl/achievementsengine/latest">
    <img src="https://img.shields.io/github/last-commit/qWojtpl/achievementsengine">
  </p>
</div>

<br>

# AchievementsEngine

<p>Add achievements to your Minecraft server.</p>
<p>Tested minecraft versions: </p> 

`Spigot 1.18.2`

# Installation

<p>Put achievementsengine.jar to your plugins folder and restart the server.</p>

# Configuration

`%nl% - new line`<br>
`Use § sign instead of & for colors`

<details><summary>achievements.yml</summary>
<br>

`* means anything, eg. kill 10 * (means kill 10 any entity), drop 64 * (means drop 64 of any item)`<br>
`*% means anything like, eg. break 64 *%ore (means break 64 any block which name contains ore)`
<br>

## Supported events:

```java
- join        // Join event, eg. join 1 server
- kill*       // Kill event, eg. kill 20 zombie named Super Zombie!!
- break       // Break block event, eg. break 64 dirt
- place       // Place block event, eg. place 128 spruce_log
- pickup*     // Pickup (how many items, not how many times) item event, eg. pickup 32 slime_ball
- T_pickup*   // Pickup (how many times, not how many items) item event, eg. T_pickup 5 dirt
- drop*       // Drop (how many items, not how many times) item event, eg. drop 64 stone
- T_drop*     // Drop (how many times, not how many items) item event, eg. T_drop 10 diamond_sword
- craft*      // Craft (how many items, not how many times) item event, eg. craft 1 cake
- T_craft*    // Craft (how many times, now how many items) item event, eg. craft 10 diamond_pickaxe
- enchant*    // Enchant item event, eg. enchant 1 diamond_sword named Magic sword!
- fish        // Fish (using fishing rod) event, eg. fish 64 pufferfish
- catch       // Catch (using fishing rod) entity, eg. catch 10 wolf
- shoot*      // Shoot event, eg. shoot 20 bow
- throw       // Throw event, eg. throw 64 snowball, throw 10 trident
- command     // Send command event (without arguments), eg. command 30 /ae
- chat        // Send chat message event, eg. chat 10 Wiggle-Wiggle
- breed       // Breed animals event, eg. breed 10 cow
- complete    // Complete achievement event, eg. complete 1 <other achievement key>
- sign        // Edit sign event, eg. sign 10 This is my house
- furnace     // Take from furnace event, eg. furnace 15 glass (glass is a product)
- eat         // Eat event, eg. eat 64 apple
```

<sup>* - event that supports names, eg. `kill 1 villager named Some Villager`</sup>

## YML Fields:

<b>
Every key must be child of "achievements"<br>
Before below fields add parent key<br>
</b>
<br>

`enabled` - Mark if achievemnt is enabled<br>
`name` - Achievement name<br>
`description` - Achievement description<br>
`item` - GUI item (default is bedrock)<br>
`showProgress` - If true shows the progress in GUI and e<br>
`announceProgress` - If true announce when player will progress in achievement by sending chat message to him<br>
`requiredProgress` - Is a number. You can specify what sum of progress player need to complete this achievement. Set to 0 to disable.<br>

In this case player needs to mine total 10 of these ores
Player can mine 5 iron ores and 5 gold ores to complete this achievement

```yml
requiredProgress: 10
events:
- break 10 iron_ore
- break 10 gold_ore
```

`world` - Specify in which world player have to be to make progress in this achievement<br>
`events` - List of events required to complete this achievement (syntax: {EVENT} {HOW_MANY_TIMES} {BLOCK/ENTITY/ITEM/TEXT} [named] [TEXT]), eg.<br>

```yml
events:
- break 64 dirt
- fish 10 cod
```

`actions` - List of actions (commands) which will be fired when player will complete achievement<br>

<b>
{0} - player nickname<br>
{1} - achievement name
</b>

```yml
actions:
- say {0} completed achievement {1}!
- give {0} minecraft:diamond 1
```

## Example configuration:

```yml
achievements:
  '0':
    enabled: true
    name: '§6§lMarksman'
    description: '§aShoot 64 times from bow%nl%§aRewards:%nl%§b12 diamonds'
    item: BOW
    showProgress: false
    announceProgress: false
    events:
    - shoot 64 bow
    actions:
    - give {0} minecraft:diamond 12
  'fisherman':
    enabled: true
    name: '§1§lFisherman'
    description: '§aFish 50 cods%nl%§aRewards:%nl%§232 emeralds'
    item: FISHING_ROD
    showProgress: true
    announceProgress: tre
    events:
    - fish 50 cod
    actions:
    - give {0} minecraft:emerald 32
  'fame':
    enabled: true
    name: '§6§lIm fame!'
    description: '§aComplete all achievements'
    item: GOLD_BLOCK
    showProgress: false
    announceProgress: false
    events:
    - complete 1 0
    - complete 1 fisherman
    actions:
    - give {0} minecraft:gold_block 64
    - 'say {0} Completed all achievements! ;O'
```

## Default configuration:

```yml
achievements:
  '0':
    enabled: true
    name: §2§lSample Achievement
    description: §aUse /ae command and get 1 diamond.
    item: BEDROCK
    showProgress: false
    announceProgress: false
    events:
    - command 1 /ae
    actions:
    - give {0} minecraft:diamond 1
```

</details>

<details><summary>messages.yml</summary>

## YML Fields:

<br>

`prefix` - Commands prefix.<br>
`gui-title` - GUI title<br>
`gui-next` - Next GUI page<br>
`gui-previous` - Previous GUI page<br>
`complete-message` - Complete achievement chat message. {0} is achievement name, {1} is description, {2} is events, {3} is actions<br>
`progress-message` - Progress achievement chat message. {0} is achievement name<br>
`progress` - Progress (text in GUI)<br>
`progress-field-prefix`: Prefix in GUI progress list<br>
`completed` - Completed (text in GUI)<br>
`not-downloaded` - Let the user know that his data is not downloaded yet (mostly for SQL)<br>

<br>

`event-translation` - Syntax: `eventName: translation` - shows event differently in GUIHandler when showProgress is set to true<br> 

## Default configuration:

```yml
messages:
  prefix: '§2[§6AchievementsEngine§2] '
  gui-title: Achievements (Page {0}/{1})
  gui-next: §f§lNext page
  gui-previous: §f§lPrevious page
  complete-message: "§6§k--------------%nl%%nl%§a§lNew achievement!%nl%§a§lUnlocked:
    {0}%nl%%nl%§6§k--------------"
  progress-message: §aYou made progress in achievement {0}§a!
  progress: "§6§lProgress:"
  progress-field-prefix: §7§l- §b
  completed: §aCOMPLETED!
  not-downloaded: '§cWhoaa! Not too fast? Your data is not downloaded yet, please try again later!'

event-translation:
  named: "named"
  join: "Join"
  kill: "Kill"
  break: "Break"
  place: "Place"
  pickup: "Pickup"
  T_pickup: "Pickup (times)"
  drop: "Drop"
  T_drop: "Drop (times)"
  craft: "Craft"
  T_craft: "Craft (times)"
  enchant: "Enchant"
  fish: "Fish"
  catch: "Catch"
  shoot: "Shoot"
  throw: "Throw"
  command: "Command"
  chat: "Chat"
  breed: "Breed"
  complete: "Complete"
  sign: "Sign"
  furnace: "Furnace"
```

</details>

<details><summary>config.yml</summary>
<br>

`useYAML` - When set to true plugin will be using YAML to save data<br>
`useSQL` - When set to true plugin will be using SQL to save data<br>
`saveInterval` - Interval between data saves (in seconds)<br>
`logSave` - When set to true every save will send message to console<br>
`keepPlayersInMemory` - When set to true, all player's states (completed achievements, progress) etc. is saved in memory. When set to false it destroys when player left the server<br>
`disableOnException` - If set to true then when SQL exception appear the plugin will be disabled<br>

## Default configuration:

```yml
config:
  useYAML: true
  useSQL: false
  saveInterval: 300 # 5 minutes
  logSave: true
  keepPlayersInMemory: false
  disableOnException: true

sql:
  host: ''
  user: ''
  password: ''
  database: ''
  port: 3306
```

</details>

# Commands & Permissions

`/ae`                                - Show achievements list, permission to receive any achievement `ae.use`<br> 
`/ae help`                           - Permission that let using below commands (and /ae help) `ae.manage`<br> 
`/ae reload`                         - Reload config `ae.reload`<br>
`/ae achievements`                   - Print all achievements and requirements to complete `ae.achievements`<br>
`/ae complete <nick> <achievement>`  - Complete achievement for some player `ae.complete`<br>
`/ae remove <nick> <achievement>`    - Remove player's completed achievement `ae.remove`<br>
`/ae reset <nick> <achievement>`     - Reset player's achievement progress `ae.reset`<br>
`/ae transfer <from> <to>`           - Transfer player's completed achievements and progress to other player `ae.transfer`<br>
