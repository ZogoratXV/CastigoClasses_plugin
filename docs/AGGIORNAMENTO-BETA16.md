# CastigoClasses beta.16 — animazioni per skill e attacchi

## Installazione

Aggiorna insieme plugin e mod. Il server resta Purpur Minecraft 26.2 con CastigoCore beta.9: non richiede mod. Il client resta Minecraft 26.2, Fabric Loader 0.19.5 e Fabric API.

Arresta il server, salva una copia di `plugins/CastigoClasses`, sostituisci il vecchio JAR con `CastigoClasses-0.1.0-beta.16.jar` e riavvia. Sui client sostituisci la vecchia mod con `CastigoClasses-Fabric-0.1.0-beta.16.jar`. Non lasciare due versioni di CastigoClasses nella stessa cartella.

Better Combat non è una dipendenza e non va aggiunto per utilizzare queste animazioni. Le nuove pose sono originali: nessun codice o file di animazione del JAR allegato è stato incorporato. La convivenza con altri mod che sostituiscono le animazioni del giocatore non è stata collaudata.

## Cosa cambia

Spade e asce ricevono automaticamente movimenti di combattimento. I fendenti alternano direzione; una pausa superiore a un secondo o un cambio arma/slot fa ripartire l'alternanza. Le variazioni sono esclusivamente visive: niente bonus combo, ritardi al danno, nuove hitbox o modifiche a portata, cooldown, mana e XP. Lo scavo mantiene il comportamento vanilla.

Le skill usano movimenti dedicati per lancio magico, arco, scudo, affondo, colpo pesante e fendente. I colpi successivi delle combo seguono gli eventi già previsti dal plugin. Casting interrotto, morte, cambio mondo e cambio arma fermano le pose; l'usura normale dell'arma non interrompe l'animazione. I VFX d'impatto rimangono collegati ai risultati reali delle skill.

La terza persona anima busto, testa e braccia; le gambe conservano la camminata vanilla. La prima persona usa spostamenti più contenuti e non ruota la telecamera. I movimenti includono ritorno graduale alla posa normale e una breve interpolazione quando si passa da preparazione a rilascio. Gli attacchi normali iniziano visivamente dal colpo e dal suo seguito, poiché il danno vanilla non viene ritardato per introdurre una preparazione.

## Configurazione in gioco

Apri `/classe pose`, oppure `/classe admin` → **Calibra animazioni armi**. Clicca l'oggetto nel tuo inventario senza consumarlo. La nuova riga di pulsanti permette di scegliere:

| Profilo | Uso |
|---|---|
| Automatico | Spada o arma pesante dal materiale; altri oggetti mantengono vanilla negli attacchi normali |
| Movimenti vanilla | Disattiva i gesti personalizzati di quell'oggetto, salvo la precedenza delle armi già configurate a due mani |
| Spada a una mano | Fendenti alternati |
| Spada a due mani | Movimento ampio e impugnatura con seconda mano durante l'animazione, quando libera |
| Arma pesante | Colpo verticale di forza |
| Affondo | Spinta in avanti |
| Scudo | Gesto difensivo e spinta |
| Arco | Movimento contenuto per mantenere libera la visuale |
| Lancio magico | Gesti di incanalamento e rilascio |

Le assegnazioni si salvano in `weapon-poses.json`, nella proprietà `profile` dello stesso oggetto che contiene le calibrazioni numeriche. Gli oggetti ItemsAdder vengono identificati dal plugin: due oggetti con lo stesso materiale vanilla possono avere profili diversi. Senza override usano il profilo del materiale base.

La selezione del profilo conserva gli angoli e gli spostamenti già impostati nella beta.15. **Ripristina posa dell'arma** cancella sia il profilo sia i valori numerici dell'oggetto. Il pulsante **Automatico** cambia solo il profilo.

Per gli attacchi normali e i fendenti generici delle skill vale il profilo dell'arma; le skill di arco, scudo, magia, affondo e colpo pesante mantengono il gesto corrispondente alla propria funzione. Il profilo Movimenti vanilla costituisce l'eccezione esplicita.

Le armi a due mani già assegnate nella GUI Progressione mantengono la precedenza sul profilo automatico o manuale quando il giocatore appartiene alla relativa classe e ha la seconda mano libera. L'impugnatura continua a essere adattabile ai modelli ItemsAdder tramite gli angoli dei bracci e gli spostamenti della mano di supporto. La scelta del profilo nella GUI Pose modifica l'aspetto, non i requisiti della classe o le regole dell'oggetto.

## Sincronizzazione e compatibilità

Il client mostra subito il proprio attacco e invia al plugin una richiesta cosmetica. Il plugin controlla connessione, permessi, stato, arma, identificativo progressivo e frequenza massima di dieci richieste al secondo. Solo il profilo calcolato dal server viene inoltrato ai client compatibili entro 64 blocchi. Le richieste non invocano attacchi né infliggono danni.

Una conferma mantiene l'inizio dell'animazione locale anziché riavviarla. Le conferme vecchie o arrivate dopo la fine del movimento vengono scartate. Una skill ha precedenza sul gesto di un attacco normale. Gli utenti senza mod vedono le normali animazioni vanilla; i client Castigo precedenti conservano il protocollo delle skill precedente. Le nuove animazioni degli attacchi normali richiedono il client beta.16 anche sul giocatore che le avvia.

Per il primo attacco appena cambiato oggetto, il client può attendere il successivo aggiornamento del contesto dell'arma (normalmente entro circa 250 ms); questo evita di usare il profilo ItemsAdder dell'oggetto precedente. Il danno vanilla non viene bloccato durante l'attesa.

Estensione del protocollo: capability `weaponAnimations: 2` nell'hello; contesto arma nello stato; richiesta `attack_visual` con ID progressivo; evento `weapon_motion_v2` con sequenza server, profilo, variante, fase, durata, mano e calibrazione. I destinatari precedenti ricevono ancora `weapon_motion` per le skill.

## Verifiche e collaudo

Compilati plugin e mod con Java 25: **172 test superati**, zero fallimenti; resta un test della beta.15 non eseguibile per una funzione del bersaglio di prova non implementata da MockBukkit.

I nuovi test verificano richieste duplicate e troppo frequenti, conservazione di vita/mana/XP/cooldown, protocollo precedente, permessi di animazione, profili e persistenza della GUI, precedenza delle armi a due mani, continuità delle pose, alternanza e posizione contenuta dell'arco. Due verifiche sul bytecode di Minecraft 26.2 controllano i metodi e gli argomenti usati dagli agganci di rendering/input.

Non è stato avviato un server Purpur con due client Minecraft: la resa visiva finale, l'applicazione degli agganci a runtime e le impugnature dei modelli ItemsAdder restano da collaudare in gioco.

Prova consigliata: due giocatori con la beta.16, spada e ascia vanilla, poi la spada ItemsAdder a due mani; controlla visuale in prima e terza persona, fendenti alternati e scavo. Prova quindi arco, scudo, casting interrotto e combo; confronta vita e cooldown prima/dopo l'aggiornamento. Verifica anche cambio arma durante il movimento e osservazione da un secondo client.

Per tornare alla beta.15 ripristina i vecchi JAR e la copia di `weapon-poses.json` precedente all'aggiornamento: la beta.15 non riconosce il nuovo campo `profile`.
