---
trigger: always_on
---

"Always write Java 25 compatible code."

"Target NeoForge for Minecraft 26.1."

"Use official Mojang parameter names, as NeoForge 26.1 removes code obfuscation."

"Register items, blocks, and entities using NeoForge's DeferredRegister system rather than standard vanilla registries."

"Prioritize using NeoForge's Event Bus for modifying behavior. Only fall back to writing Mixins if a suitable event does not exist."