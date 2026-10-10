# CastigoClasses beta.14 — progressione, esperienza e armi

Aggiorna entrambi i JAR: plugin sul server e mod nel client, poi riavvia. Restano Purpur/Minecraft 26.2, Fabric Loader 0.19.5, Fabric API e CastigoCore beta.9 già utilizzati. Le impostazioni HUD e le immagini ItemsAdder non cambiano.

## GUI di amministrazione

`/classe admin` → **Progressione e sottoclassi**, oppure `/classe progressione`. Permesso: `castigo.classes.admin`.

Nel menu principale puoi impostare il cap XP giornaliero e l'orario di reset, azzerare la quota di un giocatore online oppure di tutti (anche offline). Il reset globale richiede un secondo clic di conferma. Non cancella livelli o XP di avanzamento: libera soltanto la quota giornaliera. Valori iniziali: **100000 XP al giorno**, reset **00:00 Europe/Rome**. Zero disabilita il cap giornaliero. L'orario è modificabile in formato `HH:mm`, con ora legale italiana gestita automaticamente.

Selezionando una classe puoi impostare il cap di livello, il gruppo LuckPerms, il consumabile necessario per ENTRARE in quella sottoclasse e le spade con impugnatura a due mani. Le impostazioni sono salvate in `plugins/CastigoClasses/progression-settings.yml`. Il limite globale `max-level` di config.yml resta il massimo assoluto. La GUI controlla che i cap mantengano raggiungibile il percorso delle sottoclassi.

## Percorsi iniziali

| Classe base — cap 10 | Sottoclasse — cap 25 | Sottoclasse finale — cap 50 |
|---|---|---|
| Mago Bianco | Custode della Luce | Ierofante |
| Mago Nero | Occultista | Arcimago dell’Ombra |
| Guerriero con scudo | Difensore | Baluardo |
| Guerriero a due mani | Spadaccino | Maestro della Lama |
| Arciere | Esploratore | Maestro dei Venti |
| Mago originale | Incantatore | Arcimago |

Gli ID delle sottoclassi sono `<id_base>_esperto` e `<id_base>_maestro`. Dodici nuovi file YAML vengono installati se mancanti. I file delle classi esistenti non vengono sovrascritti. I nomi si possono modificare nei rispettivi YAML; `parent`, `required-level` e `level-cap` definiscono il percorso. Le sottoclassi ereditano le otto skill e gli attributi della disciplina; gli override di oggetti e icone della classe precedente si ereditano salvo personalizzazioni più specifiche.

Per le cinque discipline rimangono gli sblocchi originali: 2 skill al livello 5, 2 al 15, 2 al 30 e 2 al 50. Il Mago originale, quando non specifica unlock-level, usa 1, 5, 15, 15, 30, 30, 50, 50. Gli unlock-level espliciti personalizzati hanno precedenza. Le skill non apprese non vengono disegnate sulla barra; gli altri slot restano nella posizione scelta e non cambiano tasto. È ancora possibile spostare una skill appresa in uno slot vuoto dal menu K.

## Consumabile di promozione

1. Apri la GUI e seleziona la **sottoclasse di destinazione**, per esempio Custode della Luce.
2. Apri **Oggetto per entrare nella sottoclasse** e clicca un oggetto nel tuo inventario. Il menu ne registra l'identificativo senza consumarlo. Supporta oggetti vanilla e ItemsAdder attraverso la sua API.
3. Il giocatore deve raggiungere il cap della classe precedente e tenere il consumabile nella mano principale.
4. Clic destro: passa alla sottoclasse e consuma **un solo oggetto**. In alternativa `/classe sottoclasse mago_bianco_esperto` usa lo stesso controllo e consuma lo stesso oggetto.

Se più destinazioni usano lo stesso oggetto, il clic destro mostra gli ID tra cui scegliere tramite comando. Una sottoclasse senza consumabile configurato non è accessibile tramite promozione. Non sono stati scelti oggetti arbitrari al posto dei tuoi item personalizzati. Livello, punti attributo e quota XP giornaliera rimangono invariati. Il comando staff `/classe set` resta una assegnazione amministrativa e non richiede consumabili.

Al cap di classe l'XP residua viene azzerata e ulteriori ricompense non si accumulano. La barra è piena e **verde**. Dopo la promozione riprende a crescere, salvo cap giornaliero già esaurito. Al cap giornaliero diventa piena e **rossa**, senza perdere l'XP di avanzamento già guadagnata. Se entrambi sono raggiunti prevale il verde. I personaggi già sopra un cap non perdono livelli: rimangono bloccati per l'XP fino a una classe con cap superiore.

## Esperienza diretta

L'XP vanilla delle uccisioni viene distribuita direttamente, senza sfere. Considera il danno effettivo inflitto negli ultimi **30 secondi**, inclusi skill e proiettili; il danno eccedente la vita rimasta non aumenta il contributo. Sono idonei i partecipanti online, vivi, nello stesso mondo ed entro 128 blocchi al momento della morte. Il totale è ripartito proporzionalmente, senza moltiplicarlo per il numero dei giocatori. Gli arrotondamenti conservano il totale. La parte bloccata dal cap di un giocatore non viene redistribuita.

La quantità di partenza rimane l'XP vanilla del mob, moltiplicata per `xp.vanilla-multiplier`. Una morte che non produce XP vanilla non crea una ricompensa aggiuntiva. Il PvP non premia XP: i giocatori non rilasciano esperienza alla morte, come nelle versioni precedenti.

Anche blocchi, estrazione dalle fornaci, pesca, bottiglie e allevamento assegnano direttamente l'esperienza quando il giocatore è identificabile. Per commercio e mola viene usato il giocatore responsabile riportato dall'orb. Le altre sfere vengono soppresse; quelle già presenti vengono rimosse anche al caricamento dei chunk. Le fonti custom senza un giocatore responsabile non vengono assegnate a un passante casuale. Il contatore giornaliero è unico per giocatore, persistente e non si azzera cambiando classe.

## LuckPerms

Assegnazione iniziale, rientro e cambio classe sincronizzano il gruppo configurato. Il gruppo predefinito coincide con l'ID della classe/sottoclasse. Un gruppo mancante viene creato vuoto. Viene rimosso soltanto il precedente gruppo gestito da CastigoClasses; i gruppi staff e gli altri ruoli rimangono. Il gruppo primario non viene forzato, per conservare la gerarchia dei ranghi. Per scegliere la HUD di classe usa la priorità nel menu HUD. Se LuckPerms non è installato il resto del plugin continua a funzionare.

## Arco e spade a due mani

La posa dell'arco durante le skill è stata spostata verso il basso e verso l'esterno, riducendo rotazione e contraccolpo che portavano il modello davanti al mirino.

Per la famiglia Guerriero a due mani, seleziona **Spade a due mani** nella GUI della classe base e clicca gli item da abilitare; cliccare nuovamente un item già registrato lo rimuove. Le sottoclassi ereditano la lista. Il personaggio deve avere la mano secondaria libera. La mod aggiunge il braccio di supporto in prima persona e applica una posa con entrambe le braccia in terza persona, anche mentre si impugna l'arma senza lanciare skill. Il server sincronizza la posa agli osservatori vicini e la mod la rimuove cambiando oggetto, mondo o stato di utilizzo.

Questa lista controlla l'impugnatura visiva: gli oggetti richiesti per lanciare ciascuna skill continuano a essere configurati nel menu skill. La posizione esatta della mano sull'elsa dipende dal modello e dalla trasformazione ItemsAdder dell'arma: la posa comune va collaudata nel client con le tue spade. Non è stato eseguito un collaudo visivo in Minecraft.

## Verifiche

Build plugin e mod e test automatici su limiti XP, persistenza, ora italiana e ora legale, contributi alle uccisioni, consumo oggetti, GUI, ereditarietà, ordine aggiornamenti LuckPerms, colori della barra e posa dell'arco. Gli item ItemsAdder sono riconosciuti tramite l'integrazione già presente; resta da provare sul server reale con gli ID e i modelli che sceglierai.
