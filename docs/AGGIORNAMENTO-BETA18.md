# Aggiornamento beta.18 — animazioni e tempi VFX

## Installazione
Sostituire entrambi i JAR beta.17 con la beta.18. Riavviare client e server.
Server: Purpur Minecraft 26.2, Java 25, CastigoCore 2.1.0-beta.28.
Client: Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2.
Player Animation Library 1.2.5+mc.26.2 è inclusa nella mod: non occorre installarla separatamente. Non aggiungere il JAR Better Combat completo a questa configurazione: il collegamento usa i suoi asset, non il suo sistema di combattimento.
Nessuna mod da installare sul server. Configurazioni, classi, HUD e progressione esistenti vengono conservate.

## Animazioni
La build locale include i 31 file di animazione originali, non modificati, estratti dal Better Combat 3.2.2 per 26.2 fornito dall'utente. Il nuovo livello client li riproduce tramite Player Animation Library, lo stesso runtime richiesto da Better Combat. Le vecchie pose procedurali non vengono sovrapposte quando è attivo questo livello.

Spade: tagli alternati. Spade a due mani: tagli a due mani; fendente pesante e affondo lungo hanno i relativi movimenti dedicati. Asce standard: colpo a una mano. Sequenza del campione: due tagli alternati e un colpo pesante finale, con VFX separati per i tre colpi confermati dal server.

Sono presenti otto sequenze originali per benedizione, maledizione, canalizzazione, lancio magico, arco, scudo, scatto e riscossa. Better Combat non contiene queste animazioni di magia. Anche le nuove sequenze usano fotogrammi completi di braccia, mani e busto, riprodotti dal nuovo runtime.

Prima persona: braccia e oggetti vengono renderizzati con il modello animato, invece di muovere soltanto l'oggetto vanilla. Il vecchio braccio aggiuntivo a due mani viene escluso durante la riproduzione. Mano sinistra e scudo in secondaria vengono gestiti tramite specchiatura del rig. Le gambe mantengono il movimento del giocatore durante camminata, nuoto e cavalcatura.

La riproduzione è cosmetica: danni, portata, cooldown e validazione rimangono su Purpur. Gli attacchi normali mantengono la previsione locale e l'acknowledgment non riavvia l'animazione. Cambio arma, morte, uso di oggetti, interruzione e cambio mondo terminano il movimento. Il rilascio parte dalla sezione di attacco della clip, senza introdurre il ritardo di preparazione del combattimento Better Combat.

## VFX corpo a corpo
Rimossi i tratti iniziali invisibili dei modelli quando arrivano come effetti di rilascio. La velocità dei fotogrammi successivi resta quella del modello. Guardia e aure persistenti conservano la propria comparsa. Nella combo viene visualizzato soltanto il segmento relativo al colpo corrente, senza rilanciare ogni volta tutta la sequenza di tre colpi. Rimane il normale tempo di trasmissione della conferma server; non vengono simulati colpi o consumi di risorsa non confermati.

## Compilazione da sorgente
Gli asset Better Combat forniti dall'utente restano esclusi dal repository pubblico e dal sources JAR. Il repository contiene il ponte di integrazione e un importatore:
`python tools/import_weapon_animations.py percorso/al/bettercombat-fabric-3.2.2+26.2.jar`
L'output viene scritto in `local-animations/`, esclusa da Git. Le clip rimangono attribuite a Daedelus/Better Combat e soggette alla licenza del progetto originale; non vengono presentate come creazioni CastigoClasses.
Riferimenti: https://github.com/ZsoltMolnarrr/BetterCombat e https://github.com/ZigyTheBird/PlayerAnimationLibrary .

## Verifica
Compilazione e test automatici di plugin e mod; caricamento delle clip con il runtime PAL, interpolazione e arresto delle animazioni, geometria immediata dei VFX corpo a corpo e segmentazione della combo. La resa visiva in prima/terza persona e con gli oggetti ItemsAdder specifici richiede una prova in Minecraft: non è stata verificata in una sessione di gioco live.
