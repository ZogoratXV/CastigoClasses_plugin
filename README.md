# CastigoClasses — plugin

Versione di test **0.1.0-beta.10** per **Purpur Minecraft 26.2**, Java 25 e **CastigoCore 2.1.0-beta.9**.
Client: [CastigoClasses Fabric](https://github.com/ZogoratXV/CastigoClasses_fabricmod), Minecraft 26.2 / Fabric Loader 0.19.5.

## VFX di tutte le classi — beta.10

Preset per 48 abilità: sigilli ampliati, scie intrecciate, impatti con volume e frammenti cubici, tagli, scudi e vortici. [Installazione e utilizzo](docs/AGGIORNAMENTO-VFX-BETA10.md) · [Catalogo delle abilità](docs/VFX-48-ABILITA.md). Aggiornare plugin e mod alla beta.10.

## Motore VFX 3D beta.8

Orison con anelli e colonna a texture, animazione e quattro pagine nell'editor F8. Aggiornare entrambi i JAR alla beta.8: [guida al motore, texture e collaudo](docs/MOTORE-VFX-3D.md).

## Casting ed editor beta.7

Barra casting e editor VFX in gioco con F8: [guida completa](docs/EDITOR-VFX-CASTING.md). Aggiorna plugin e mod alla beta.7.

## Orison beta.6

Nuovo effetto di guarigione sul destinatario e audio incluso nella mod: [installazione, preset e collaudo](docs/ORISON-VFX.md).

## Aggiornamento beta.5

Cure senza party, requisiti per oggetto vanilla/ItemsAdder, `/classe admin` e icone PNG personalizzate: [guida completa](docs/OGGETTI-ICONE-VFX.md). Aggiorna sia plugin sia mod alla beta.5 per le icone.

## Installazione

1. Lascia CastigoCore nella cartella `plugins` del server.
2. Sostituisci il precedente CastigoClasses con `CastigoClasses-0.1.0-beta.10.jar` e riavvia.
3. LuckPerms è facoltativo: quando presente, il suo gruppo principale viene mostrato nel client.
4. Installa la mod e Fabric API sul client seguendo il repository della mod.

All'avvio vengono aggiunte le cinque discipline mancanti in `plugins/CastigoClasses/classes`: Mago Bianco, Mago Nero, Guerriero con scudo, Guerriero a due mani e Arciere, ciascuna con otto skill. Configurazioni e profili esistenti vengono conservati. Le nuove installazioni usano `default-class: mago_bianco`; quelle aggiornate mantengono il valore precedente. Il Mago originale e l'esempio di sottoclasse restano disponibili.

**[Guida alle 40 abilità, configurazione e prova rapida](docs/DISCIPLINE.md)**. La mod Fabric beta.5 riceve classi e icone dinamicamente; sostituisci la beta.4 per visualizzare i PNG personalizzati. Gli sblocchi iniziali sono ai livelli 5, 15, 30 e 50; al livello 1 si combatte con le azioni vanilla.

## Contenuti

- Classi YAML, sottoclassi con ereditarietà e requisito di livello, esattamente otto abilità per classe.
- Attributi iniziali e crescita per livello: forza, destrezza, vita, mana/risorsa, intelligenza, attacco e difesa.
- Risorsa nominabile (Mana, Vigore, Fede…), colore, capacità, rigenerazione e crescita della rigenerazione.
- Livelli MMO, esperienza, salvataggi atomici tramite CastigoCore, cooldown persistenti e disposizione delle skill personale.
- Il server verifica costi, classe, sblocco, cooldown e bersagli: il client invia solo intenzioni.
- Catalogo dinamico inviato alla mod: nuove classi basate sugli esecutori disponibili non richiedono un aggiornamento client. Un tipo di meccanica completamente nuovo richiede codice server.
- VFX e suoni delle skill configurabili: il plugin invia eventi ai giocatori vicini con la mod beta.4, che anima localmente raggi, anelli, spirali e particelle. Asset vanilla, senza resource pack obbligatorio.

| Abilità del mago | Effetto | Costo | Ricarica |
|---|---|---:|---:|
| Dardo arcano | Bersaglio in linea di mira | 8 | 1,5 s |
| Sfera di fuoco | Impatto ad area, senza danni ai blocchi | 18 | 5 s |
| Nova glaciale | Danno e rallentamento intorno al mago | 20 | 8 s |
| Passo dimensionale | Teletrasporto breve con controllo del terreno | 15 | 7 s |
| Barriera arcana | Assorbimento temporaneo dei danni | 22 | 15 s |
| Ricucitura | Cura personale | 25 | 12 s |
| Folgore | Colpo elettrico mirato | 24 | 9 s |
| Meteora | Segnale sul terreno e impatto dopo un secondo | 45 | 25 s |

## Comandi

Permesso giocatori: `castigo.classes.use` (attivo di default).

- `/classe`: riepilogo personaggio, attributi e otto slot.
- `/classe skill <1-8>`: lancia uno slot, anche senza mod.
- `/classe scambia <1-8> <1-8>`: scambia due skill e salva.
- `/classe lista`: classi e sottoclassi disponibili.
- `/classe sottoclasse <id>`: passa a una figlia diretta della classe corrente, se il livello richiesto è raggiunto. Il passaggio è permanente per il giocatore; lo staff può cambiarlo.

Permesso staff: `castigo.classes.admin` (op di default).

- `/classe set <giocatore-online> <id>`: cambia classe mantenendo livello e percentuale della risorsa. Non ripristina i cooldown.
- `/classe xp <giocatore-online> <quantità>`: assegna XP MMO.
- `/classe livello <giocatore-online> <1-100>`: imposta il livello (limite da config), azzera gli XP del livello e riconcilia i punti attributo. Abbassare il livello non revoca i punti già guadagnati; risalire non li duplica.
- `/classe reload`: valida l'intero catalogo prima di sostituirlo e lo reinvia ai client. Una classe utilizzata da un giocatore online non può essere rimossa.

Alias: `/classi`, `/cc`. Per cambiare una classe base in questa prima versione si usa il comando staff.

## Creare una classe

Copia `classes/mago.yml` con un altro nome e modifica `id` e `name`. Gli ID accettano lettere minuscole, numeri e `_` (massimo 40 caratteri).

```yaml
id: guerriero
name: Guerriero
resource:
  name: Vigore
  color: 'E0A24A'
  regen-per-second: 7
  regen-per-level: 0.2
attributes:
  base:
    strength: 12
    dexterity: 5
    health: 36
    mana: 80
    intelligence: 1
    attack: 3
    defense: 8
  per-level:
    strength: 1.5
    dexterity: 0.4
    health: 3
    mana: 3
    intelligence: 0.1
    attack: 0.5
    defense: 1
# Aggiungi esattamente otto voci skills come nel mago.
```

`mana` è la chiave tecnica della capacità della risorsa, anche quando l'etichetta visibile è Vigore o Fede.
I valori finali includono anche i bonus dei punti attributo assegnati (vedi sotto). La vita è limitata a 1–1024 punti; un cuore vanilla equivale a 2 punti.

Per ogni skill sono configurabili `name`, `description`, `effect`, `icon` (ID di un oggetto Minecraft), `color` RGB, `unlock-level`, `cost`, `cooldown-seconds`, `power`, `intelligence-scale`, `range`, `radius`, `duration-seconds`.
Gli effetti disponibili sono `BOLT`, `FIREBALL`, `FROST_NOVA`, `BLINK`, `WARD`, `HEAL`, `LIGHTNING`, `METEOR`. I parametri non pertinenti a un effetto vengono ignorati; METEOR ha un ritardo fisso di 1 secondo.
Il potere è `power + intelligenza × intelligence-scale` per danno, cura e scudo. Il costo resta quello configurato.
Gli ID skill condivisi tra classi condividono il cooldown per impedire azzeramenti cambiando classe. Usa ID diversi per abilità diverse.

Una nuova **meccanica** di abilità richiede un nuovo esecutore nel plugin. Un nuovo tipo di rendering esclusivamente client richiede anche un aggiornamento mod. Questa versione compone animazioni client con icone, particelle e suoni vanilla.

## Creare una sottoclasse

Rinomina `piromante.yml.example` in `piromante.yml`, poi esegui `/classe reload`:

```yaml
id: piromante
name: Piromante
parent: mago
required-level: 10
attributes:
  base:
    intelligence: 14
  per-level:
    intelligence: 2
```

La sottoclasse eredita gli attributi e le proprietà della risorsa non specificati. Se `skills` è assente, eredita tutte le otto abilità. Se presente, deve contenere tutte le otto abilità della specializzazione. I valori specificati sostituiscono quelli del genitore, non si sommano. Sono possibili più livelli di specializzazione; cicli e genitori mancanti vengono rifiutati.

## Regole di gioco della beta

- XP richiesti: `ceil(xp.base × xp.growth^(livello-1))`, con limite configurabile di livello. Gli XP vanilla ricevuti tramite `PlayerExpChangeEvent` diventano XP MMO. La conversione non recupera gli XP già consumati prima dell'evento da Ripristino/Mending.
- Barra e numero esperienza vanilla mostrano la progressione MMO. I comandi vanilla `/experience` non assegnano XP MMO: usa `/classe xp`.
- Morte: non si perdono livelli/XP MMO e non vengono lasciate sfere XP del giocatore. Respawn: vita e risorsa ripristinate; cooldown mantenuti.
- Incudini e tavoli da incantamento sono disattivati per i personaggi gestiti, così non spendono i livelli MMO. Una valuta separata per questi servizi è fuori da questa prima versione.
- Forza aggiunge `forza × combat.strength-melee-factor` all'attacco vanilla; Attacco aggiunge un contributo diretto. Destrezza aumenta proporzionalmente la velocità di attacco, fino a +200%. Intelligenza scala le skill. Difesa applica `danno × 100/(100+difesa)` ai danni da entità, prima delle altre riduzioni vanilla.
- Vita e attacco sono modificatori rimovibili: eventuali bonus di equipaggiamento o di altri plugin possono sommarsi. La vita configurata sostituisce il valore base convenzionale di 20 tramite un modificatore.
- Rigenerazione solo online, nessun guadagno della risorsa durante l'assenza. Cambio classe mantiene la percentuale disponibile, senza ricaricare gratuitamente.
- PvP delle skill disattivato di default. Le skill evitano giocatori creativi/spettatori, animali addomesticati, armor stand e NPC marcati. Danno e teletrasporto passano dagli eventi Bukkit; un adattatore preserva il controllo WorldGuard INTERACT rimosso dal core beta.9.
- Non vengono distrutti blocchi o creati incendi. Barriera assorbe danni grezzi dopo la difesa, prima di armatura/resistenze; non protegge dal vuoto.
- Salvataggio atomico ogni 60 secondi, su uscita e su arresto; classe e ordine salvati subito dai relativi comandi. Non eliminare classi usate da profili offline senza migrarli: al login viene impedito di sovrascrivere il profilo con una classe diversa.

## Compilazione e test

Java 25. Copia il JAR originale del core in `libs/CastigoCore.jar` (non viene incorporato o caricato su GitHub).

```powershell
.\gradlew.bat build
```

Linux/macOS: `./gradlew build`. Risultato: `build/libs/CastigoClasses-0.1.0-beta.10.jar`.
Dipendenza API fissata: `org.purpurmc.purpur:purpur-api:26.2.build.2632-stable`.

I test coprono crescita, curva XP, cap, risorse/cooldown, ordine degli slot, ereditarietà/validazione YAML e salvataggi reali attraverso CastigoCore. Build e test automatici verificati; prova multiplayer e compatibilità con gli altri plugin del server ancora da effettuare.
Protocollo comune: [docs/PROTOCOL.md](docs/PROTOCOL.md). Prove manuali: [docs/TEST-IN-GIOCO.md](docs/TEST-IN-GIOCO.md).

## Punti attributo

Apri **K → Attributi** e premi **+** accanto alla statistica, oppure usa `/classe assegna <attributo>` (forza, destrezza, vita, mana/risorsa, intelligenza, attacco, difesa). Ogni pressione spende un punto; il server verifica disponibilità e limiti e salva immediatamente. La scheda **Disposizione skill** conserva il riordino degli otto slot. In questa versione non è previsto il rimborso dei punti.

In `plugins/CastigoClasses/config.yml`:

```yaml
stat-points:
  every-levels: 2
  points-per-award: 1
  per-point:
    strength: 1
    dexterity: 1
    health: 2
    mana: 5
    intelligence: 1
    attack: 1
    defense: 1
```

Il valore predefinito assegna un punto ai livelli 2, 4, 6…; con `every-levels: 1` si inizia dal livello 2. `points-per-award: 0` disabilita le nuove ricompense; un bonus `per-point` pari a zero disabilita l'assegnazione a quell'attributo. Applica le modifiche con `/classe reload`.

I vecchi profili ricevono una sola volta i punti già maturati secondo il loro livello. Successivamente, le modifiche a frequenza e quantità valgono per i livelli futuri, senza ricalcolare ricompense precedenti. Abbassare e riguadagnare livelli non duplica i punti. Cambio classe, sottoclasse e riconnessione conservano punti e assegnazioni. Modificare `per-point` ricalcola invece il bonus di tutti i punti già spesi.

Ogni totale è `base + crescita × (livello - 1) + punti assegnati × bonus per punto`, entro i limiti previsti. Vita massima 1024; gli altri attributi arrivano a 1.000.000. L'aumento di vita o risorsa massima non cura e non ricarica istantaneamente.

| Attributo totale | Effetto effettivo |
|---|---|
| Forza e attacco | Bonus all'attacco corpo a corpo: `attacco + forza × combat.strength-melee-factor`, massimo 2047 |
| Destrezza | Bonus proporzionale alla velocità d'attacco: `destrezza × combat.dexterity-speed-factor`, massimo +200% |
| Vita | Aumenta la vita massima |
| Mana/risorsa | Aumenta la capacità di Mana, Vigore, Fede o altra risorsa della classe |
| Intelligenza | Aumenta danno, cura e barriera delle skill secondo `intelligence-scale` |
| Difesa | Riduce il danno da entità con `danno × 100/(100+difesa)`, prima delle riduzioni vanilla |

Le stesse formule usano sia la crescita automatica sia i punti assegnati. Il pannello mostra danno e velocità degli attributi Minecraft correnti, inclusi i modificatori applicabili: il danno finale di un colpo dipende anche da ricarica, critici, armatura e bersaglio. La riduzione mostrata riguarda solo la difesa MMO. Non si possono spendere punti quando il relativo contributo ha già raggiunto il limite.

## Aggiornamento core e VFX client

Usa la mod **0.1.0-beta.10** per vedere e sentire gli effetti delle skill. Configurazione e limiti: [docs/VFX-CLIENT.md](docs/VFX-CLIENT.md). L'esempio viene creato in `examples/presentation.yml.example` al primo avvio. I vecchi YAML ricevono preset compatibili senza essere sovrascritti.

Le cinque discipline del documento RP sono soltanto valutate in [docs/PROPOSTA-DISCIPLINE.md](docs/PROPOSTA-DISCIPLINE.md): le 40 abilità non sono incluse in questa build.


