# Barra di lancio ed editor VFX — beta.7

Installa **plugin e mod 0.1.0-beta.7**, mantenendo CastigoCore beta.9, Minecraft 26.2 e le dipendenze Fabric precedenti. Non serve installare Photon.

## Barra casting

Durante una preparazione appare una barra dorata da sinistra a destra, larga come la barra XP vanilla, sopra la hotbar. Sopra la barra appare il nome della skill. È visibile anche quando la hotbar non è in modalità skill.

Il server invia durata totale e tempo rimanente; il client interpola tra gli aggiornamenti per rendere fluido il riempimento. Completamento, interruzione per danno/movimento/equipaggiamento, cambio classe o mondo azzerano lo stato. In presenza di lag la barra può raggiungere il termine prima della conferma: il risultato della skill dipende sempre dal server. Le abilità istantanee non mostrano una preparazione inventata: Orison rimane istantaneo, mentre Grande intercessione ha 1,5 secondi di preparazione.

## Editor in gioco

Premi **F8**, rimappabile in Opzioni → Comandi → Castigo Classes, mentre sei connesso al server CastigoClasses. Il gioco continua durante l'editing: scegli un luogo tranquillo. Tutti possono provare localmente; per modificare i VFX condivisi serve `castigo.classes.admin`.

1. Prima di aprire l'editor, orienta la visuale dove vuoi osservare l'effetto; F5 è utile per vedere una cura su te stesso.
2. Nei pulsanti superiori scegli classe, skill e fase. I pulsanti scorrono le voci disponibili, comprese le classi aggiunte al catalogo.
3. Premi **Dal server** per caricare il preset effettivo. Cambiare selezione mantiene intenzionalmente la bozza: è utile per copiarla su un'altra skill, ma non carica automaticamente il nuovo preset.
4. Cambia forma e parametri nelle due pagine. Puoi modificare durata, raggio, punti, colore RGB, dimensione, dispersione, particella, quantità, ID del suono, volume e tono; tre interruttori abilitano fase, particelle e audio.
5. Seleziona **Anteprima: su me** oppure **mira**. In modalità mira, il bersaglio è un giocatore sotto il puntatore; in assenza di esso l'effetto viene mostrato tre blocchi davanti alla visuale.
6. Premi **Anteprima (3 s)**. Il pannello si nasconde, viene riprodotto un ciclo e poi i controlli tornano visibili. Le anteprime sono solo locali: non curano, non danneggiano, non consumano risorse e non vengono inviate agli altri giocatori. I normali effetti delle skill continuano a funzionare.
7. **Salva locale** conserva il progetto nel client. **Apri locale** riapre il file della selezione corrente.
8. **Applica al server** valida e salva la singola fase scelta. I lanci successivi la usano per tutti gli osservatori compatibili, senza copiare il JSON a ogni client. Il messaggio di risposta conferma il salvataggio o spiega il rifiuto.
9. **Ripristina server** rimuove l'override di quella fase e torna al preset del YAML/default. Le altre fasi restano come sono.

Il pulsante di applicazione richiede conferma del server: non fidarti di una semplice anteprima per considerare la modifica pubblicata. Le modifiche non salvate vengono perse chiudendo la schermata. Il salvataggio locale riutilizza lo stesso nome per classe/skill/fase: per varianti separate conserva copie dei JSON.

## Prova con Orison

Seleziona `mago_bianco` → `mago_bianco_orison` → `IMPACT`, poi **Dal server**. Troverai `HEALING_BEAM`, colore `55FF66`, raggio `0.85` e suono `castigoclasses:skill.orison`. Cambia per esempio il colore in `FFD56A`, prova localmente e, se ti piace, applica al server. Il rendering continuerà a seguire il destinatario della cura come nella beta.6.

## Fasi e possibilità

| Fase | Uso |
|---|---|
| CAST | Emessa al rilascio riuscito di tutte le skill, comprese guardie/scatti/tiri delle discipline; dopo l'eventuale preparazione |
| IMPACT | Effetto sul bersaglio/posizione negli esecutori che lo emettono, per esempio Orison e attacchi mirati |
| TRAIL | Percorsi già previsti dagli esecutori, per esempio dardi, teletrasporto e meteora del Mago originale |
| TELEGRAPH | Preavviso della meteora |
| HIT | Assorbimento della barriera del Mago originale |

Se una skill non emette una fase, configurarla non ne crea l'aggancio: l'anteprima può mostrarla, ma il lancio reale non la chiamerà. CAST è il punto comune disponibile per progettare un effetto su ogni skill. Non indica l'inizio della preparazione: viene emesso quando la tecnica si esegue. Un lancio che manca il bersaglio o viene interrotto non equivale a un'esecuzione riuscita.

Forme disponibili: **BURST, LINE, RING, SPIRAL, HEALING_BEAM**. Ogni fase contiene una forma e un suono; non c'è ancora una timeline con più emettitori simultanei, un editor di shader/mesh, curve, collisioni delle particelle o importazione dei progetti Photon. HEALING_BEAM usa la sua geometria dedicata: i campi punti, quantità e dispersione non la modificano; colore, dimensione, durata e raggio sì, nei limiti del renderer.

Per le altre forme: durata 1–40 tick, punti 2–32, raggio 0,1–12, quantità 1–32, dispersione 0–2, dimensione 0,05–4, volume 0–2 e tono 0,5–2. Colore: sei cifre esadecimali senza `#`. Particella: nome Bukkit, per esempio `DUST`, `FLAME`, `END_ROD`, `HAPPY_VILLAGER` o `SNOWFLAKE`. Il server accetta DUST e particelle senza dati speciali secondo il core; tipo e colore vengono validati. I suoni sono nella categoria Giocatori; un ID audio personalizzato necessita dell'asset nella mod/resource pack. I budget del renderer restano attivi anche nell'anteprima.

## Dove si salvano i progetti

Nel client, nella cartella dell'istanza Minecraft:

```text
config/castigoclasses/vfx/
  mago_bianco__mago_bianco_orison__IMPACT.json
```

Nel server:

```text
plugins/CastigoClasses/vfx-overrides.json
```

Gli override sono separati dalle statistiche e dai YAML delle classi e hanno precedenza sulla fase selezionata. Applicare un override abilita la presentazione della skill e sostituisce quella fase; per disabilitarla usa l'interruttore Effetto OFF e applica. Il salvataggio è atomico e i preset restano dopo il riavvio. `/classe reload` rilegge anche il file server. Le modifiche non terminano gli effetti già partiti, che mantengono i parametri precedenti fino alla fine.

Per lavorarci insieme puoi condividere il JSON salvato localmente oppure indicarne il percorso: possiamo modificarlo e ricaricarlo con **Apri locale**. Per portare una stessa bozza su più abilità, salva/applica ogni assegnazione esplicitamente. Nessun file contiene codice eseguibile o comandi server.

## Rapporto con Photon e verifiche

La [pagina ufficiale Photon](https://www.curseforge.com/minecraft/mc-mods/photon) elenca Fabric tra i loader, ma la release 26.2 mostrata al momento della verifica è NeoForge. Questo editor usa il renderer CastigoClasses già disponibile su Fabric 26.2; non incorpora Photon né ne replica tutte le funzioni avanzate.

Build Java 25 completate, 64 test plugin e 23 test mod superati senza test saltati. Verificati avanzamento/azzeramento casting, permessi sul messaggio di modifica, persistenza e ripristino per classe/fase, limiti dei parametri e file locali. Nei test è emerso anche un invio non valido durante lo spegnimento: ora i messaggi non vengono inviati dopo la disabilitazione del plugin.

La schermata e la resa in gioco richiedono ancora collaudo con Minecraft reale: non è stata eseguita una verifica visiva nel client. Prova prima Orison IMPACT, poi Grande intercessione per la barra, interruzione con movimento/danno, applicazione come admin e rifiuto senza permessi; verifica il mantenimento dei preset dopo riavvio e cambio classe.
