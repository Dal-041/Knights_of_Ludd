## v1.4.0
- Updated for Starsector 0.98
- Compiled for Java17 and Kotlin 2.0
- Made KOL and [REDACTED] fleets unlockable for the simulator.
- Added rare main menu setpieces.
- Fixed a crash on Mac & Linux with the Alysse AI.
- Dusk EM anomalies nerfed: unlike vanilla, these will now respect hardflux levels when determining shield penetration
- Improved some [VERY REDACTED] flavor descriptions.
- Kiyohime sprite reworked
- Seiryu sprite touchups
- Nian is now much more aggressive as an enemy.
- Improved Linggui's normal maps
- Wani - improved slot types to encourage build freedom
- Wani, Suzaku System:
  - Its system is now much more responsive.
  - Its system can now be used to dodge through attacks.
- Added some visual effects
- Fixed a few crash that could occur while using the special Dusk AI.

## v1.3.6
- The Null Gate can now be selected as a travel option when interacting with a gate, if its been scanned.
- The Null Gate now has custom travel VFX.
- The Null Gate should now be fully scanned when traveled to, it was only partialy scanned before.
- The Null Gate glitch can now only happen once per save, and only after the main story has been completed.

## v1.3.5
- Fixed Mimosa Lidar AI for real this time
- Fixed Crash on Ninaya Boss fight

## v1.3.4
- Linggui:
  - Fully Resprited
  - [Buff] +2 Medium missile turrets
  - [Buff] Ordnance Points 310 -> 330
- Drastically improved Ninmah boss fight AI
- Drastically improved Dusk Core AI 
- Defeating both Elysian Boss's no longer shuts down all the Dawn and Dusk fleets
- Defeating the Dusk Boss no longer shuts down all the Dawn fleets
- Dawntide fleets no longer effected by its system's star terrain effect
- Mount and AI tag changes to fix Mimosa's Lidar system and Nian's facing
- Fixed rare CTD on Alysse's AI when switching to and from autopilot while piloting one

## v1.3.3
- Re-Added the VFX Shader to the location it was removed from, will only be in affect if you have GraphicsLib shaders enabled now.
- Fixed the Dawntide Builtin not applying the solar shielding effect.
- Fixed a crash that can occur when a Nian-Class ship takes damage.

## v1.3.2
- Removed some VFX that due to an issue caused some peoples screen to turn mostly black in a certain location
- Fixed the Nians Built-in weapon being able to spawn independently of the ship

## v1.3.1
-Fixes linking intel location not showing up after fighting Ninaya

## v1.3.0

### Knights of Ludd
- New custom KOL hullmod icon
- Fixed Libra being buildable by the player
- Several misc bugfixes, code refactor & optimization pass

Ships
- Mimosa:
  - Partially resprited
  - [Buff] Upgraded a small mount to a medium mount
  - [Buff] Ordnance Points 105 -> 115
- Alysse:
  - [Buff] Augumented combat AI will now attempt to angle itself to keep armor modules between the Alysse and its target
  - [Nerf] Hull 18000 -> 13000
  - [Nerf] Armor 1150 -> 1000
    - Originaly set higher than typical KOL hull/armor values due to how awful vanilla AI was at piloting the ship
- Lunaria:
  - Lunete SFX adjustments
  - Rear modules now visualy react to control inputs
  - [Buff] Lunete can now be fired while ship system is in use
- Blaze: Fully resprited

### Exploration Content [Spoilers ahead!]
- New portraits added to Yukionna, Nian, Amaterasu
- New unique rewards added to Yukionna, Nian, Corrupting Heart
- New AI faction and TT boss ship hullmods/tooltips
- Custom Ninmah phase AI improvements to facilitate new rewards
- Fixed ships not correctly hidden from Tripad when appropriate
- Fixed double recovery of limited ships
- Fixed Conformal Shields jank
- Fixed Delta Site not being cutoff from hyperspace
- Fixed some encounters trying to talk to you

### Tri-Tachyon Bosses
- Improved uncloak behavior on all phase vessels to improve responsiveness when overlapping
- Reinforcements during boss fights now spawn with their systems on cooldown

Ships
- Nineveh:
  - Fully resprited
  - New custom minestrike visuals
  - New fancy phase glows
  - [Buff] Motes now actively seek to system mines
  - [Nerf] Armor 1300 -> 900
  - [Nerf] Hull 12000 -> 9000
  - [Nerf] Max Flux 14000 -> 12000
  - [Nerf] Flux Diss 1100 -> 1000
  - [Nerf] Ordnance Points 245 -> 220
  - [Nerf] Top Speed 50 -> 40
- Ninmah:
  - UNIQUE tag removed
  - [Buff] Motes become red (HF) when using system
  - [Nerf] Armor 1250 -> 900

### Dawn
- New shield SFX
- New vent SFX

Ships
- Tianma: [Nerf] removed B-Deck hullmod
- Bixi: System changed to Charge Drive, a short-duration burn drive that allows turning
- Ao:
  - New system SFX
  - Main rotational engines are now separate destructable modules. Destruction reduces mobility and top speed.
  - [Nerf] Omni shield emitter replaced with a front shield emitter
- Linggui: New system SFX
- Qilin: New system SFX
- Nian:
  - Main cannons are now player-controllable!
  - AI update

### Dusk
- New visuals added to Nullspace
- New custom minestrike variant with new sound & visuals
- Yomogi Plasma Autogun: [Buff] Damage 60 -> 75, Flux/Shot 60 -> 70
- Reduced Center radius of all phase ships to improve uncloak responsiveness
- Mote logic overhauled for better performance and more fluid behaviour
- Fixed Ninaya & Ninmah turning all dusk motes on-screen red (HF)
- Fixed motes keeping their red-state (HF) damage after turning blue again

Ships
- Ikiryo: Fully resprited
- Onibi: [Nerf] Shield efficiency 0.5 -> 0.6
- Shiryo: Fully resprited
- Furaribi:
  - Fully resprited
  - New system SFX
  - [Buff] Shields now stay up after system usage
  - [Buff] Armor 300 -> 400
  - [Buff] Hull 3000 -> 3500
  - [Buff] Max Flux 6000 -> 6500
  - [Buff] Top Speed 100 -> 120
- Onryo/Ayakashi/Yukionna: Phase Skip system now returns to initial shield state, instead of forcing shield online after usage

### Elysia
- New custom Hypershunt art
- The Hypershunt in the Elysian Abyss can now be brought online by the player
- Fixed rare Twin-Layer Shielding crash

Ships
- Wani/Suzaku: [Buff] System now reduces energy weapon flux
- Amaterasu:
  - Fully resprited
  - Fixed mount locations not being symmetrical
  - [Buff] Sun AOE damage increased by 400% and switched to FRAG damage type
  - [Nerf] System can no longer store charges
  - [Nerf] System cooldown increased from 4 to 8 seconds
- Corrupting Heart:
  - Fully resprited
  - [Buff] Bubble shield drone added to player version of the ship.
  - [Nerf] System is no longer charge-based
## v1.2.0:
- Fresh sprites for some Knights armor modules and visuals touchups for Knights ships
- Awesome sprite upgrade for all [VERY REDACTED1] ships
- Added a new game mechanic where [VERY REDACTED1] hulls trade a range bonus for a firerate bonus as they take damage
- Fixed an issue with extreme reputation changes
- New [VERY REDACTED2] beam weapon visuals courtesy of Nia Tahl
- New intel entries added when engaging [VERY REDACTED1] and [VERY REDACTED2] fleets for the first time
- Gave Unknown Location (1) boss a smaller arena to clown on chumps in
- Exiled Space support courtesy of Pure Tilt
- Added smaller fleets of [VERY REDACTED3] to their home (new game required)
- Adjusted [VERY REDACTED2] spawn weights to reduce small ship spam
- Reduced maximum recoil for the Caelia & Llyr weapons
- Made Bricoles appear 5x more often at KOL markets
- Mimosa: added Rugged Construction
- Tamarisk: Hellfire buffed from 30% to 40% extra damage
- Lunaria: LIDAR recoil buffed from -75% to -85%
- Refined Knights ship and modules bounds for slightly improved performance
- [VERY REDACTED3] boss 1 has a smaller flux pool if attained by players
- [VERY REDACTED1] boss cannon is more lenient when targeting
- Two bosses have had their OP reduced
- fixed Targeting Beam system
- various improvements, tweaks, and polish
## v1.1.1:
- Added KOL portraits to character creation
- Fixed bug preventing more than one Luddic Path boss from being recovered per save
- New [VERY REDACTED1] sprite and visuals x2
- Lunaria set to 50 dp
- Fixed Invictus (KOL) OP & FP values
- Improved some weapon visuals
- Improved [VERY REDACTED3] boss visuals
- Misc polish for Blaze and its weapon, Brimstone
- Corrected an issue with KOL's composite bonus hullmod
- Module hardflux is now synced to parent ship for Polarized Armor
- Fixed armor module armor grid UI scaling issue
- Fixed armor module related crash
- Fixed blinker deco on [VERY REDACTED3] other boss
- AI code optimizations
- Various minor fixes and improvements
## v1.1.0:
- Subsystems have been moved to MAGICLIB 1.4.0 - Make sure you're updated!!
- Added the Church of Galactic Redemption starting alliance, updates to the Larkspur Sprite (Nia Tahl), various tweaks and improvements.
## v1.0.8:
- Fixed an issue where any reputation loss with the church would end a Knights commission and vice versa.
- Made first boss more aggressive.
- Tweaked certain enemy ships to expose their hulls from their shields more.
- Corrected a devastating typo.
## v1.0.7:
- Fixes a crash when hovering over Battlestar Libra's station industry, unsets a debug flag.
## v1.0.6:
- Balance adjustments, boss behavior improvement.
- Fixed an incompatible-hullmods crash, further loop-prevention checks on the shield AI.
## v1.0.5:
- Added Rugged Construction to shieldless Knights ships.
- Fixed an issue with Knights shield AI that could bloat memory when faced with exotic weapons.
- Added Battlestation Libra to new randomsector games. Misc polish.
## v1.0.4:
- Fixed an AI issue that was making certain boss fights much less deadly than intended. Also rebalanced them slightly.
## v1.0.3:
- Adjusted Pather Boss spawns to hopefully be more reliable, misc polish.
## v1.0.2:
- Fixes a crash when starting Random Sector games, some polish, including fresh FX from Amazigh, and another NPE fix.
## v1.0.1:
- Fixes an industry issue and an esoteric crash.
## v1.0.0:
- Initial public release.
