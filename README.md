# AchievementsEngine

<div style='display:inline-block'>
<img src='https://img.shields.io/github/v/release/qWojtpl/achievementsengine'>
<img src='https://img.shields.io/github/languages/top/qWojtpl/achievementsengine'>
<img src='https://img.shields.io/github/repo-size/qWojtpl/achievementsengine'>
</div>
<div style='display:inline-block'>
<img src='https://img.shields.io/github/downloads/qWojtpl/achievementsengine/latest/total'>
<img src='https://img.shields.io/github/commit-activity/m/qWojtpl/achievementsengine'>
<img src='https://img.shields.io/github/commits-since/qWojtpl/achievementsengine/latest'>
<img src='https://img.shields.io/github/last-commit/qWojtpl/achievementsengine'>
</div>

<br>

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

`* means anything, eg. kill 10 * (means kill 10 any entity), drop 64 * (means drop 64 of any item)`
<br>

## Supported events:

```java
- join        // Join event, eg. join 1 server
- kill*       // Kill event, eg. kill 20 zombie named Super Zombie!!
- interact    // Interact block event, eg. interact 50 stone_button
- break       // Break block event, eg. break 64 dirt
- place       // Place block event, eg. place 128 spruce_log
- pickup*     // Pickup (how many items, not how many times) item event, eg. pickup 32 slime_ball
- T_pickup*   // Pickup (how many times, not how many items) item event, eg. T_pickup 5 dirt
- drop*       // Drop (how many items, not how many times) item event, eg. drop 64 stone
- T_drop*     // Drop (how many times, not how many items) item event, eg. T_drop 10 diamond_sword
- craft       // Craft item event, eg. craft 1 cake
- enchant*    // Enchant item event, eg. enchant 1 diamond_sword named Magic sword!
- fish        // Fish (using fishing rod) event, eg. fish 64 pufferfish
- catch       // Catch (using fishing rod) entity, eg. catch 10 wolf
- shoot*      // Shoot event, eg. shoot 20 bow
- command     // Send command event (without arguments), eg. command 30 /ae
- chat        // Send chat message event, eg. chat 10 Wiggle-Wiggle
- complete    // Complete achievement event, eg. complete 1 <other achievement key>
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
`showProgress` - If true shows the progress in GUI and when someone will progress in achievement plugin will send chat message<br>
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
    events:
    - command 1 /ae
    actions:
    - give {0} minecraft:diamond 1
```

</details>

<details><summary>messages.yml</summary>

## YML Fields:

<b>
Every field must be child of "messages"<br>
</b>
<br>

`prefix` - Commands prefix.<br>
`gui-title` - GUI title<br>
`gui-next` - Next GUI page<br>
`gui-previous` - Previous GUI page<br>
`complete-message` - Complete achievement chat message. {0} is achievement name<br>
`progress-message` - Progress achievement chat message. {0} is achievement name<br>
`progress` - Progress (text in GUI)<br>
`progress-field-prefix`: Prefix in GUI progress list<br>
`completed` - Completed (text in GUI)<br>

## Default configuration:

```yml
messages:
  prefix: '§2[§6AchievementsEngine§2] '
  gui-title: Achievements (Page {0}/{1})
  gui-next: §f§lNext page
  gui-previous: §f§lPrevious page
  complete-message: '§6§k--------------%nl%%nl%§a§lNew achievement!%nl%§a§lUnlocked:
    {0}%nl%%nl%§6§k--------------'
  progress-message: §aYou made progress in achievement {0}§a!
  progress: '§6§lProgress:'
  progress-field-prefix: §7§l- §b
  completed: §aCOMPLETED!
```

</details>

# Commands & Permissions

`/ae`                               `ae.use` - Show achievements list, permission to receive any achievement<br>
`/ae help`                          `ae.manage` - Permission that let using below commands (and /ae help)<br>
`/ae reload`                        `ae.reload` - Reload config<br>
`/ae achievements`                  `ae.achievements` - Print all achievements and requirements to complete<br>
`/ae complete <nick> <achievement>` `ae.complete` - Complete achievement for some player<br>
`/ae remove <nick> <achievement>`   `ae.remove` - Remove player's completed achievement<br>
`/ae reset <nick> <achievement>`    `ae.reset` - Reset player's achievement progress<br>
`/ae checkstate <nick>`             `ae.checkstate` - Check if player's state is initialized<br>
`/ae transfer <from> <to>`          `ae.transfer` - Transfer player's completed achievements and progress to other player<br>
