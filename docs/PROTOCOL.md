# Castigo Classes — protocollo v1

Canale PLAY bidirezionale: `castigo:classes`.
Payload: un oggetto JSON UTF-8 **senza prefisso di lunghezza**, newline, Java `writeUTF` o altra intestazione. Bukkit invia/riceve `byte[]`; Fabric usa un `CustomPacketPayload` con codec che legge/scrive i byte restanti.
Tutti i messaggi includono `"v":1` e `"type":"..."`. Limiti applicativi: 2.048 byte client→server, 30.000 byte server→client, massimo 256 classi. Non inviare il catalogo in un singolo pacchetto.

## Client → server

```json
{"v":1,"type":"hello"}
{"v":1,"type":"cast","slot":0}
{"v":1,"type":"reorder","slots":["dardo_arcano","sfera_di_fuoco","nova_glaciale","passo_dimensionale","barriera_arcana","ricucitura","folgore","meteora"]}
```

Gli slot sono zero-based (0–7). Il server risolve lo slot sull'ordine del profilo: non accetta danni, bersagli, costi o statistiche dichiarati dal client.
`reorder` deve essere una permutazione esatta delle otto skill della classe. Niente duplicati, skill estranee o slot mancanti.
Il server limita le richieste a una ogni 80 ms e gli invii di catalogo richiesti dal client a uno al secondo. Costi, cooldown e permessi vengono controllati separatamente.
Il client ritenta `hello` ogni 2 secondi quando non dispone di uno stato valido e quando il server ha registrato il canale.

## Server → client

1. `catalog_begin`: inizia un catalogo temporaneo.
2. `class`: un messaggio per classe con i campi qui sotto.
3. `catalog_end`: sostituisce il catalogo client.
4. `state`: stato completo del personaggio, inviato anche ogni 5 tick e dopo azioni/riordino.

Campi di `class` (nomi case-sensitive):

- `id`, `name`, `description`, `parent` (stringa vuota per classe base), `requiredLevel`.
- `resourceName`, `resourceColor` (intero RGB), `regenPerSecond`, `regenPerLevel`.
- `base`, `growth`: oggetti con `strength`, `dexterity`, `health`, `mana`, `intelligence`, `attack`, `defense`.
- `skills`: otto oggetti con `id`, `name`, `description`, `effect`, `icon`, `color`, `unlockLevel`, `cost`, `cooldownMs`, `power`, `intelligenceScale`, `range`, `radius`, `durationTicks`.

Campi di `state`:

- `name`, `classId`, `group` (stringa vuota se LuckPerms assente).
- `level`, `xp` (progressi nel livello), `xpNext` (0 al livello massimo).
- `health`, `maxHealth`, `resource`, `maxResource`.
- `stats`: i sette attributi calcolati.
- `slots`: gli otto ID nell'ordine attualmente autorizzato.
- `cooldowns`: mappa ID skill→millisecondi rimanenti. Il client usa un orologio locale per l'animazione e il successivo stato per correggerla.

Altri messaggi: `feedback` con campo `message`; `disabled` per rimuovere lo stato quando il plugin viene spento.
Il client considera lo stato scaduto dopo 10 secondi; il toggle abilità si spegne e torna la hotbar vanilla. Alla disconnessione vengono cancellati catalogo, stato e cooldown locali.
L'aggiunta di campi facoltativi è compatibile con v1; una modifica incompatibile richiede incremento di versione su entrambi i progetti.

## Estensione punti attributo (plugin beta.2 / mod beta.3)

Client → server: `{"v":1,"type":"allocate","attribute":"strength"}`. Attributi ammessi: strength, dexterity, health, mana, intelligence, attack, defense. Una richiesta spende esattamente un punto. Il server controlla permessi, saldo e limiti e salva prima di applicare il bonus; il client attende lo stato autorevole.

Campi facoltativi aggiunti a `state`:

- `statPoints`: `available`, `earned`, `everyLevels`, `pointsPerAward`; `allocated` è una mappa attributo→punti spesi; `perPoint` è una mappa attributo→bonus; `canAllocate` è un array degli attributi attualmente assegnabili.
- `combat`: `attackDamage`, `attackSpeed`, `meleeBonus`, `defenseReductionPercent`.
- `stats` contiene i totali comprensivi dei punti spesi.

La versione resta 1: i client precedenti ignorano i campi aggiunti; il nuovo client nasconde i controlli quando `statPoints` non è presente. Alla disconnessione o all'arrivo di uno stato senza questi campi vengono cancellati i dati precedenti. Il client limita localmente le assegnazioni a una ogni 250 ms; resta valido il limite server di 80 ms per le richieste.
