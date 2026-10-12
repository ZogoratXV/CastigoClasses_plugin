# CastigoClasses beta.17 — Core beta.28, HUD e modelli delle skill

## Installazione
1. Arresta il server e sostituisci il vecchio CastigoCore con `CastigoCore-2.1.0-beta.28.jar`.
2. Sostituisci il plugin con `CastigoClasses-0.1.0-beta.17.jar` nella cartella `plugins`.
3. Sostituisci la mod su ogni client con `CastigoClasses-Fabric-0.1.0-beta.17.jar` nella cartella `mods`.
4. Riavvia server e client. Minecraft 26.2, Java 25, Fabric Loader 0.19.5 e Fabric API 0.161.0+26.2 restano i riferimenti di compilazione.

Il server resta Purpur: non richiede mod server, Better Combat, GeckoLib o ModelEngine.
Conserva le cartelle di configurazione e i dati dei giocatori. Gli effetti usano gli ID delle skill: vengono mantenuti anche nelle sottoclassi.

## HUD incluse nella mod
Tutti gli undici PNG precedenti sono ora nel JAR. Non occorre rigenerare il pacchetto ItemsAdder per queste cornici.
Ogni immagine ha una posizione del ritratto dedicata, testi centrati e ridimensionati, barre interne alla cornice. Il nome usa il sistema di identità di CastigoCore.

Le configurazioni HUD esplicite continuano ad avere precedenza. In assenza di configurazione, vengono riconosciuti i gruppi `archangel`, `arcangelo`, `admin`, `arcidemone`, `criminale`, `guardia`, `popolano`; altrimenti viene scelta la HUD della classe base, anche nelle sottoclassi. Per gruppi con nomi differenti usa `/classe hud` e seleziona il preset desiderato.
I vecchi riferimenti `castigo:textures/gui/hud/NOME.png` agli undici PNG vengono interpretati con il nuovo preset allineato. I PNG personalizzati con altri percorsi mantengono il proprio layout. I preset inclusi usano `castigoclasses:textures/gui/hud/NOME.png`.

## Modelli e animazioni
Sono integrate le geometrie e le texture dei 40 modelli Blockbench forniti, con animazioni gerarchiche, interpolazione lineare/Catmull–Rom/step e fogli texture animati. I manichini e le armi usati come riferimento nella scena non vengono disegnati.
Le cure compaiono sul destinatario, le aure persistenti seguono il bersaglio, le aree restano sul terreno. I proiettili e gli impatti sono separati dalla scena di anteprima e seguono il percorso effettivo; ogni freccia reale disegna un solo proiettile personalizzato. Il santuario usa entrata, ciclo e uscita. Restano attivi suoni e icone già presenti.

Il giocatore usa pose diverse per benedizioni, maledizioni, canalizzazione, scudo, arco, affondi, fendenti, cariche, riscossa e spazzate basse. Le animazioni restano cosmetiche: danni, risorse, bersagli e tempi di lancio sono gestiti dal plugin. Le calibrazioni delle armi restano disponibili in `/classe pose`.

La geometria è luminosa e trasparente nel renderer Minecraft. La fusione additiva dell'anteprima Blockbench è resa con trasparenza emissiva: non è un effetto bloom o un motore Unity. I limiti di distanza e geometria riducono il carico quando si accumulano effetti. Le skill personalizzate con ID nuovi conservano il motore precedente finché non viene aggiunta una mappatura.

## Verifica
Compilazione di entrambi i JAR riuscita; 179 test automatici superati, un test già esistente escluso perché MockBukkit non implementa l'operazione richiesta. Verificati i 40 asset, geometrie finite, texture presenti, animazioni di volo/impatto distinte, mappature e migrazione delle HUD. Non è stato eseguito un collaudo visivo con client Minecraft e server Purpur avviati.

Primo controllo in gioco: Orison su sé stessi e su un secondo giocatore; frecce su mob e parete; vincolo tra due giocatori; santuario completo; fendenti in prima e terza persona; HUD di classe e HUD admin.
