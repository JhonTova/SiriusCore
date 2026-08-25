<div align="center">

# Sirius

### Una suite de plugins de Minecraft construida sobre un solo núcleo.

**Chat, GUIs, base de datos, colores y rendimiento — todo compartido, todo consistente.**

Compatible con **Spigot · Paper · Purpur** · Minecraft **1.21.11+** · Java 21

[Discord: ksirius](https://discord.gg/) · [GitHub: JhonTova](https://github.com/JhonTova)

</div>

---

## ¿Qué es Sirius?

Sirius no es una colección de plugins sueltos. Es un **ecosistema**: un núcleo (`SiriusCore`) que
todos los plugins comparten, con las herramientas que un servidor serio necesita —chat con
MiniMessage y gradientes, GUIs limpias, base de datos con pool, integraciones con
PlaceholderAPI y Vault, avisos de actualización— resueltas **una sola vez** y usadas por todos.

El resultado: plugins que se sienten como **un mismo producto**, ligeros y sin lag, que
arrancan juntos bajo un único banner en tu consola.

```
  ███  S I R I U S
  suite de plugins  ·  core v1.0.0
  ────────────────────────────────────────
   SGuilds        v1.0.0
   SOrbs          v1.0.0
   SWelcome       v1.0.0
  ────────────────────────────────────────
  3 plugin(s) · 3 activo(s) · listo en 82ms
```

## SiriusCore — el núcleo

Todos los plugins de la suite requieren **SiriusCore**. Sin él, ni arrancan. El core aporta:

- **Texto unificado** — MiniMessage moderno (`<gradient>`, hover, click) y códigos legacy
  (`&c`, `&#ff8800`) **mezclados en la misma línea**. Tus configs de siempre siguen funcionando.
- **GUIs** con `InventoryHolder` — sin bugs de clics cruzados, un solo listener para todo.
- **Base de datos** SQLite/MySQL con pool HikariCP y escrituras por lote.
- **Scheduler consciente de Folia** — el mismo código corre en Spigot, Paper y Folia.
- **Hooks** opcionales a PlaceholderAPI y Vault.
- **Avisos de actualización** — una sola petición para toda la suite.
- Comando **`/sirius`** para ver el estado del ecosistema en vivo.

### Instalación

1. **[Descargar SiriusCore.jar](https://github.com/JhonTova/SiriusCore/releases/latest)** y ponlo en tu carpeta `plugins/`.
2. Añade los plugins de Sirius que quieras (abajo, cada uno con su descarga).
3. Arranca. El core crea su configuración solo.

Requisitos: **Minecraft 1.21.11+** · **Java 21** · Spigot, Paper, Purpur o Folia.

## Catálogo

Toda la suite corre sobre este núcleo. Los plugins gratuitos son open source y se descargan
aquí mismo; los premium son de uso comercial y su código no es público.

### Gratuitos y open source

| Plugin | Qué hace | Descarga |
|---|---|---|
| **[SBlock](https://github.com/JhonTova/SBlock)** | Bloquea y limita encantamientos específicos. | [Descargar](https://github.com/JhonTova/SBlock/releases/latest) |
| **[SGuilds](https://github.com/JhonTova/SGuilds)** | Farmeo por gremios con recompensas por tiers. | [Descargar](https://github.com/JhonTova/SGuilds/releases/latest) |
| **[SiriusFFA](https://github.com/JhonTova/SiriusFFA)** | Arenas FFA y eventos programados con kits y estadísticas. | [Descargar](https://github.com/JhonTova/SiriusFFA/releases/latest) |
| **[SWelcome](https://github.com/JhonTova/SWelcome)** | Mensajes de bienvenida y salida con gradientes y LuckPerms. | [Descargar](https://github.com/JhonTova/SWelcome/releases/latest) |
| **[SDeathInventory](https://github.com/JhonTova/SDeathInventory)** | Ver el inventario de un jugador en el momento de morir. | [Descargar](https://github.com/JhonTova/SDeathInventory/releases/latest) |
| **[SCommunity](https://github.com/JhonTova/SCommunity)** | Misiones comunitarias diarias para todo el servidor. | [Descargar](https://github.com/JhonTova/SCommunity/releases/latest) |
| **[SDamage](https://github.com/JhonTova/SDamage)** | Suite modular de PvP: daños, cooldowns y más. | [Descargar](https://github.com/JhonTova/SDamage/releases/latest) |
| **[SSchedules](https://github.com/JhonTova/SSchedules)** | Programador de comandos y eventos por tiempo. | [Descargar](https://github.com/JhonTova/SSchedules/releases/latest) |
| **[SWave](https://github.com/JhonTova/SWave)** | Modo celebración para las compras de la tienda. | [Descargar](https://github.com/JhonTova/SWave/releases/latest) |
| **[SBooks](https://github.com/JhonTova/SBooks)** | Guarda libros y ábrelos con comandos propios (reglas, eventos…). | [Descargar](https://github.com/JhonTova/SBooks/releases/latest) |
| **[SBarDamage](https://github.com/JhonTova/SBarDamage)** | Daño en actionbar con seguimiento de objetivos. | [Descargar](https://github.com/JhonTova/SBarDamage/releases/latest) |
| **[SConsoleFix](https://github.com/JhonTova/SConsoleFix)** | Silencia el spam de la consola. | [Descargar](https://github.com/JhonTova/SConsoleFix/releases/latest) |
| **[SHealthPlaceholder](https://github.com/JhonTova/SHealthPlaceholder)** | Placeholders avanzados de vida y efectos. | [Descargar](https://github.com/JhonTova/SHealthPlaceholder/releases/latest) |

### Premium — uso comercial

Estos plugins **no son gratuitos** y su código fuente es privado. Requieren una licencia
válida emitida por Sirius para arrancar. Se distribuyen en **BuiltByBit**.

| Plugin | Qué hace |
|---|---|
| **SiriusSkills** | Motor de combate PvE: clases, habilidades, dungeons y bosses. |
| **SBank** | Sistema bancario: cuentas, ahorros, préstamos y transferencias. |
| **SDynamicQuest** | Sistema de quests competitivas por tiempo. |
| **STutorials** | Tutoriales por fases con recompensas. |
| **SRegionMobs** | Mobs personalizados por mundo con niveles y checkpoints. |
| **SpawnerPickaxe** | Picos para romper y mover spawners con protección. |
| **SOrbs** | Orbes de evento que aparecen por el mundo. |
| **SPokeballs** | Captura mobs con pokébolas — ¡incluso con sus modelos! |

> ¿Te interesa alguno de los premium? Escríbeme por Discord (**ksirius**) para conseguirlo.

---

<div align="center">

Hecho por **Sirius** · Minecraft 1.21.11+

</div>
