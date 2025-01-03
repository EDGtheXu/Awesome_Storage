
**Read this in other languages: [English](README.md), [中文](README_zh.md).**

# Awesome Storage
## Overview
Welcome to the simplified storage system mod for Minecraft, inspired by the Magic Storage mod from Terraria! 
This mod introduces an intuitive and user-friendly storage solution to Minecraft, 
allowing players to efficiently manage items, query stored items, 
and use them in crafting recipes. With advanced features such as upgradeable storage capacity, 
customizable recipe types, and a comprehensive recipe display system, 
this mod aims to enhance your gaming experience by simplifying inventory management.

***
## Key Features
### Crafting Integration
* Centralized Storage: Store all items in one system, eliminating the need to search through multiple chests.
* Item Query: Easily search for stored items using an intuitive interface.

### Upgradeable Storage
* Recipe Access: Directly access stored items for crafting without manually transferring materials.
* Recipe Display: View all recipes that can be crafted with current materials, 
as well as recipes you have materials for but haven't crafted yet.

### Customizable Recipe Types
* Capacity Expansion: Upgrade the storage system to increase capacity, ensuring your item collection is never limited by space constraints.

### Configuration Files
* Recipe Addition: Add custom recipes through configuration files, offering endless possibilities and compatibility with other mods.

### Configuration Files
Fully Customizable: Customize the mod through easily editable configuration files, providing ultimate flexibility. 
A default configuration file will be generated upon first launch, which can be modified before subsequent launches.
* Storage Subsystem: Upgrade storage space using items and customize the required items and quantities for each level, 
* as well as the corresponding expansion amount for each level
```json

{
  "1": {
    "material": {
      "id": "minecraft:iron_ingot",
      "count": 10
    },
    "extend": 10
  },
  "2": {
    "material": {
      "id": "minecraft:gold_ingot",
      "count": 10
    },
    "extend": 10
  },
  "3": {
    "material": {
      "id": "minecraft:diamond",
      "count": 10
    },
    "extend": 10
  },
  "4": {
    "material": {
      "id": "minecraft:emerald",
      "count": 10
    },
    "extend": 10
  },
  "5": {
    "material": {
      "id": "minecraft:netherite_ingot",
      "count": 10
    },
    "extend": 10
  }
}

```
* Crafting Subsystem: Add workbench IDs corresponding to recipe type IDs, activating the recipe type when the workbench is placed.
```json
{
    "minecraft:crafting_table": "minecraft:crafting"
}
```

***
## Support and Contributions
For support, bug reports, or suggestions, please leave a message in the issues or contact the author:
* QQ mailto __448537509@qq.com__

## License
This mod is licensed under the MIT License. For more information, please refer to the LICENSE file.

Enjoy the smooth storage and crafting experience brought by the Awesome Storage mod!

