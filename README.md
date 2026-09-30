# Echo
# 🧠 Minecraft Memory

**Echo is an intelligent, lightweight server history and forensics plugin that lets your Minecraft server remember what happened.**

Instead of endlessly storing massive amounts of raw data, Memory intelligently prioritizes important events, compresses repetitive activity, and automatically removes outdated information.

### 🔎 Search Your Server's History

Quickly investigate questions like:

* Who took the diamonds from this chest?
* Who destroyed this build?
* Where did this item go?
* Who killed this player?
* What happened at this location?
* When was this block broken?
* What did a player do before they were banned?

### ⚡ Built for Performance

Memory is designed with large servers in mind.

* Lightweight event tracking
* SQLite support out of the box
* MySQL/MariaDB support for larger servers
* Configurable data retention
* Automatic cleanup
* Event aggregation to reduce storage
* Only records information that actually matters

Instead of storing thousands of identical events:

> Bryan broke stone
> Bryan broke stone
> Bryan broke stone
> Bryan broke stone

Memory can summarize them as:

> **Bryan mined 47 stone blocks in this area over 3 minutes.**

### 🧠 Intelligent History

Memory separates events into different importance levels.

**Critical**

* Container transactions
* Valuable item transfers
* Player deaths
* Economy transactions
* Important administrative actions

**Normal**

* Block placement
* Block destruction
* Crafting
* Interactions

**Ignored**

* Movement
* Looking around
* Unimportant repetitive actions

### 🛠️ Example

```text
/memory search diamonds
```

> 💎 **Diamond activity found**
>
> Player: Steve
> Time: September 29, 9:42 PM
> Action: Removed 14 diamonds
> Location: Bryan's storage room
> Destination: Steve's inventory

Minecraft doesn't just log your server anymore.

**It remembers it.**
