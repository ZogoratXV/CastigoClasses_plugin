# CastigoClasses beta.12

Aggiornare entrambi i JAR: plugin per Purpur 26.2 con CastigoCore 2.1.0-beta.9 e mod per Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, Java 25.

## Installazione

1. Arresta il server e sostituisci il precedente CastigoClasses con `CastigoClasses-0.1.0-beta.12.jar` in `plugins`.
2. Sostituisci la mod nel client con `CastigoClasses-Fabric-0.1.0-beta.12.jar`, mantenendo Fabric API. Non lasciare due versioni dello stesso plugin/mod.
3. Riavvia server e client. Profili, equipaggiamento, icone e configurazioni esistenti vengono conservati.

I preset integrati si aggiornano automaticamente. Gli effetti personalizzati salvati con F8 o nella sezione `presentation` dei file classe hanno precedenza: se una skill mostra ancora il vecchio effetto, ripristina la fase interessata in F8 e controlla gli eventuali valori espliciti nel file della classe. Non è necessario cancellare configurazioni o profili.

## Corpo a corpo

- Nuova texture originale per lame di energia ampie, con bordi irregolari in stile Minecraft.
- Fendenti pesanti quasi verticali, tagli diagonali, contrattacchi inversi e falciate rasoterra.
- Tre lame/scie sovrapposte, bordo con spessore e frammenti cubici che si disperdono.
- Affondi con punta tridimensionale e due scie a spirale.
- Barriere con disegno distribuito sull'intero scudo e bordi più leggibili.
- Impatti più ampi per fendente pesante e spezzaguardia. Gli effetti restano collegati ai colpi realmente accettati dal server.

## Colori dell'arciere

Il colore è coerente fra lancio, scia e impatto; il modello fisico della freccia rimane quello di Minecraft.

| Skill | Colore |
|---|---|
| Tiro preciso | Ciano |
| Passo del cacciatore | Verde chiaro |
| Occhio del cacciatore | Giallo pallido |
| Disimpegno | Lavanda |
| Tiro ostacolante | Viola |
| Tiro di copertura | Arancio |
| Doppio tiro | Cremisi/rosa |
| Colpo del maestro | Oro |

## Bersaglio durante il tempo di lancio

Alla pressione di una skill con preparazione, il server seleziona il bersaglio valido sotto il mirino. Per le cure restano disponibili l'autocura e la selezione di un altro giocatore. L'aura segue il destinatario:

- Mago bianco: avorio `FFFFD8`.
- Mago nero: viola scuro `59209B`.
- Arciere: verde `43D66D`, indipendentemente dal colore della freccia della skill.

Girare la visuale non cambia il bersaglio selezionato. La preparazione si interrompe se il bersaglio muore, cambia mondo, esce dalla portata o dalla linea di vista, oppure non è più valido secondo le regole del server. Restano le interruzioni già esistenti per movimento del lanciatore, danni, cambio classe o equipaggiamento. L'aura termina insieme alla preparazione.

L'effetto compare solo con `preparation-seconds` maggiore di zero e un bersaglio selezionato. I preset del mago nero hanno attualmente valore zero: imposta un tempo nelle sue skill e ricarica il catalogo per abilitarne la preparazione. Le skill istantanee non mostrano l'aura; le aree a terra non selezionano un'entità. I tiri possono ancora essere scoccati a mira libera senza bersaglio. Se un bersaglio è stato selezionato, la freccia parte verso la sua posizione al termine del lancio e prosegue come proiettile normale, senza inseguimento automatico.

## Icone dalla GUI admin

Apri `/classe admin`, scegli classe e skill, quindi **Configura icona**.

- Clicca un oggetto vanilla nel tuo inventario per usarlo come icona. Non viene spostato o consumato e non cambia l'oggetto richiesto per lanciare la skill.
- **Texture PNG personalizzata**: clicca e scrivi in chat, entro due minuti, ad esempio `texture:castigo:textures/gui/skills/orison.png`. Questo input viene intercettato dal menu e non inviato nella chat pubblica. `annulla` torna al menu. Un percorso non valido non viene salvato e può essere corretto.
- **Ripristina icona originale** elimina solo la personalizzazione dell'icona.

L'esempio richiede il file `assets/castigo/textures/gui/skills/orison.png` nel resource pack client/server distribuito ai giocatori. Per il namespace della mod il percorso è `assets/castigoclasses/textures/gui/skills/nome.png` e l'identificatore è `texture:castigoclasses:textures/gui/skills/nome.png`. Usa PNG quadrati, per esempio 32×32 o 64×64, con sfondo trasparente. La GUI salva il riferimento, non carica immagini dal disco. Per un'icona ItemsAdder indica il PNG presente nel suo resource pack tramite il pulsante Texture PNG.

Il salvataggio aggiorna il catalogo inviato ai client e persiste in `plugins/CastigoClasses/skill-equipment.yml`. È richiesta la permission `castigo.classes.admin`.

## Verifiche

80 test plugin e 39 test mod superati, nessuno saltato. Verificati geometrie e colori con un'anteprima delle mesh effettive; questa non sostituisce il collaudo in Minecraft. Nei test di selezione il ray tracing non implementato da MockBukkit è sostituito da una selezione controllata e da una scena senza ostacoli. La resa finale in prima/terza persona va provata sul client.

Gli effetti conservano i limiti di geometria e il dettaglio ridotto a distanza. Audio locale già autorizzato incluso nella mod consegnata; nessun asset audio dei pacchetti commerciali viene pubblicato nei repository.
