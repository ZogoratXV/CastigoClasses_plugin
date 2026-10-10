# Aggiornamento beta.15 — gioco RP e strumenti di regolazione

Aggiornare insieme plugin e mod. Le skill continuano a essere ereditate dalle sottoclassi: nessun ramo alternativo o perdita di abilità. Livelli, punti, quote giornaliere e consumabili della beta.14 restano in uso.

## Installazione

1. Arresta il server e salva una copia della cartella `plugins/CastigoClasses`.
2. Sostituisci il JAR CastigoClasses in `plugins` con la beta.15, evitando doppioni. Rimane necessario CastigoCore beta.9.
3. Sostituisci il JAR Fabric con la beta.15 in `mods` sui client. Minecraft 26.2, Fabric Loader 0.19.5 e Fabric API restano invariati.
4. Avvia il server. Non eliminare le cartelle delle classi o dei giocatori. Gli aggiornamenti non applicano automaticamente nuovi preset alle HUD personalizzate.

## Cure e protezioni nella ripartizione XP

Il totale di XP dell'uccisione rimane identico. Il danno e il supporto contribuiscono alla suddivisione tra giocatori presenti, vivi, nello stesso mondo e entro 128 blocchi. La finestra è di 30 secondi.

Le cure delle discipline contano solo per la vita effettivamente recuperabile su un altro giocatore e collegata a ferite recenti inflitte dal mob. Cure eccedenti, autocure, danno da giocatori o danno ambientale non producono credito di cura. La rigenerazione naturale consuma il debito delle ferite senza dare credito. Sono conteggiate anche parate con scudo, riduzioni di Guardia/Santuario e danni trasferiti da Legame; un evento annullato non dà credito.

`config.yml` → `xp.support-max-ratio: 0.35`: il peso del supporto è limitato al 35% del peso del danno diretto, quindi a circa il 25,9% dell'XP complessiva quando raggiunge il limite. Valori ammessi 0–1; 0 disattiva il supporto. I punti XP interi vengono distribuiti senza aumentare il totale. Quote giornaliere e cap classe sono poi applicati individualmente: XP rifiutata dal cap non viene redistribuita.

## Pose delle armi

`/classe pose`, oppure `/classe admin` → **Calibra animazioni armi**. Clicca l'arma nel tuo inventario; non viene consumata. Sono accettati oggetti vanilla e ItemsAdder. Clicca un parametro e inserisci il valore in chat: il messaggio resta privato. Scrivi `annulla` per tornare.

- Prima persona: spostamenti X/Y/Z da -2 a 2, angoli da -180 a 180 gradi.
- Mano di supporto: spostamento della seconda mano nell'impugnatura a due mani.
- Braccio principale e supporto: angoli aggiuntivi dell'impugnatura a due mani in terza persona.
- Intensità prima/terza persona: 0–2, predefinito 1. Per l'arco parti da 0.5 in prima persona e ritocca altezza/profondità.

La calibrazione è associata all'identificativo dell'oggetto, salvata in `weapon-poses.json` e inviata agli osservatori dalla successiva animazione; per l'impugnatura a due mani si aggiorna entro circa mezzo secondo. **Ripristina posa dell'arma** rimuove tutti gli override di quell'oggetto. Occorre comunque configurare quali armi sono a due mani nella GUI Progressione; la calibrazione non trasforma un oggetto in arma a due mani.

## HUD e preset

`/classe hud` → gruppo LuckPerms → uno degli 11 pulsanti **Preset**. Il preset sostituisce layout, colori e coordinate del gruppo mantenendone la priorità. Sono presenti Arcangelo (`archangel`), Arcidemone, Popolano, Mago, Mago bianco, Mago nero, Guerriero scudo, Guerriero due mani, Arciere, Criminale e Guardia.

I preset usano i PNG già consegnati nel pacchetto CastigoHUD ItemsAdder: `castigo:textures/gui/hud/<nome>.png`. Non scaricano immagini e non modificano il resource pack. Occorre che il client abbia caricato il pack del server dopo `/iazip`.

Le coordinate sono compatte, su 172×64 GUI. Nuovi campi: scala testo 50–100%, scala testo barre 50–100%, altezza barre 4–12 pixel. I preset partono da testo 70%, testo barre 50%, barre alte 4 pixel; sono tutti regolabili dalla stessa GUI. Le barre con PNG non aggiungono una seconda cornice sopra quella disegnata. I temi esistenti mantengono la scala 100% e le barre alte 8 pixel.

Questi preset sono coordinate iniziali calcolate sulle immagini: la resa con scala GUI, font/resource pack e modello effettivo va verificata in gioco.

## Feedback di combattimento e progressione

Durante un lancio con bersaglio agganciato, sopra la barra di casting appaiono nome, distanza e portata. Il bersaglio mostrato è quello realmente memorizzato dal server. Le interruzioni distinguono movimento, cambio equipaggiamento, bersaglio morto/non disponibile, distanza e ostacoli.

Nella schermata personaggio (K), quando l'altezza disponibile è almeno 260 pixel GUI, compaiono quota XP giornaliera e prossimo reset in ora italiana. Il cap classe mantiene priorità e richiede il consumabile di promozione. Nessuna modifica alla successione delle otto skill ereditate.

## Strumenti admin

Richiedono `castigo.classes.admin`.

- `/classe diagnosi`: controlla presenza LuckPerms/ItemsAdder, cap parenti/figli e consumabili di promozione mancanti; la mod segnala in chat texture HUD e icone mancanti per la classe attuale. Non verifica tutte le risorse di ogni classe o la correttezza degli oggetti remoti ItemsAdder.
- `/classe prova crea`: bersaglio zombie immobile davanti all'admin, senza bottino/XP; dura 10 minuti, viene rimosso alla disconnessione, allo scaricamento del chunk o alla disattivazione del plugin. Richiede due blocchi d'aria davanti al giocatore. Solo il creatore può danneggiarlo; le protezioni delle regioni restano valide.
- `/classe prova report`: numero di colpi accettati, danno effettivo e DPS dal momento di creazione/reset. Il DPS include le pause tra i colpi. I colpi estremi sono limitati a 900 danni base per conservare il bersaglio: non è uno strumento per misurare danni oltre tale soglia.
- `/classe prova reset`: azzera le misure; non prolunga la durata.
- `/classe prova rimuovi`: elimina il bersaglio.

La GUI admin contiene anche i pulsanti Diagnosi e Bersaglio di prova.

## Verifica

Build plugin e mod completate con Java 25. Suite: 157 test superati; un test completo di creazione del bersaglio non eseguibile perché MockBukkit non implementa `setCanPickupItems`. Verificati tramite test anche ripartizione XP con cura reale, conservazione del totale, scadenza e limiti del supporto, GUI pose, persistenza, preset e reset durante il cambio di ora legale.

Non è stata effettuata una prova con server Purpur e client Minecraft aperti: animazioni dei modelli ItemsAdder, allineamento finale HUD e bersaglio di prova richiedono verifica in gioco.
