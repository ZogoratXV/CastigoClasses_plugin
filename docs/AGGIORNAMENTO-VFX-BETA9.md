# Aggiornamento dalla beta.8 alla beta.9 — VFX di tutte le classi

Questa versione include i preset delle **40 abilità delle cinque discipline e delle 8 abilità del mago iniziale**, con geometrie e texture originali renderizzate nella mod Fabric. [Catalogo completo delle 48 abilità](VFX-48-ABILITA.md).

## Installazione

1. Sul server sostituisci CastigoClasses con `CastigoClasses-0.1.0-beta.9.jar`. Mantieni **CastigoCore 2.1.0-beta.9**.
2. Su ogni client sostituisci la mod con `CastigoClasses-Fabric-0.1.0-beta.9.jar`. Mantieni Minecraft **26.2**, Fabric Loader **0.19.5**, Fabric API **0.161.0+26.2** e Java **25**.
3. Riavvia server e client. Non tenere due versioni dello stesso plugin/mod insieme.

I nuovi preset sono inclusi nel plugin e si applicano anche alle classi già installate: non occorre cancellare profili, statistiche o file classe. Le personalizzazioni `presentation` nei file classe e gli override dell'editor mantengono precedenza. Se una fase mostra ancora il tuo vecchio effetto, in F8 seleziona classe, skill e fase, poi **Ripristina server** per rimuovere quell'override. Un'impostazione esplicita nel file classe resta comunque valida.

## Cosa vedrai

- Mago Bianco: guarigioni sul destinatario, purificazione, vincoli luminosi e santuario dorato.
- Mago Nero: raggi viola/rossi, sigilli sui nemici, zone oscure e vortici a tre nastri.
- Guerriero con scudo: affondi, urto, barriere frontali orientate col personaggio e collegamento al protetto.
- Guerriero a due mani: archi di taglio, affondi allungati, onde circolari e impatti per ogni colpo della combo.
- Arciere: scie lungo il percorso reale delle frecce, segni sui bersagli e tracce durante gli spostamenti.
- Mago iniziale: dardo, esplosione, onda glaciale, teleport, barriera, cura, folgore verticale e meteora con avvertimento.

Le abilità che già applicano il danno istantaneamente mantengono questo comportamento: i raggi sono lampi di collegamento, non nuovi proiettili di combattimento. Le frecce dell'arciere restano invece proiettili reali, con una scia per ciascuna freccia. Questa versione non modifica costi, cooldown, equipaggiamento, sblocchi o potenza delle skill.

## Durata e bersagli

Il plugin avvia e rinnova gli effetti persistenti finché esiste lo stato corrispondente; invia un arresto alla fine, alla rimozione o al cambio classe. La mod conserva la fase dell'animazione durante i rinnovi, evitando duplicazioni. Le cure seguono il destinatario, le maledizioni seguono anche i mob e le aree rimangono nel punto di lancio. Il collegamento protettivo viene nascosto mentre distanza, visibilità o equipaggiamento lo rendono inefficace, e può tornare al rinnovo successivo.

Gli effetti persistenti usano la fase **TELEGRAPH**. La fase **CAST** viene usata anche durante la preparazione, quando prevista dal preset. **TRAIL** gestisce raggi e scie; **IMPACT** l'impatto iniziale; **HIT** le pulsazioni di rigenerazione/danno periodico e i colpi sulle protezioni. I rinnovi persistenti non ripetono l'audio. Se i messaggi si interrompono, il client lascia scadere l'effetto entro due secondi anziché mantenerlo per sempre.

## Editor F8

Apri F8, seleziona una delle classi e una skill, scegli la fase, quindi **Dal server** e **Anteprima**. L'anteprima non esegue abilità né infligge danni. Per vedere il personaggio intero usa F5; per i raggi scegli Anteprima: mira. È possibile mirare anche a un mob.

Le nuove forme sono `MESH_SLASH`, `MESH_THRUST`, `MESH_SHIELD`, `MESH_BEAM`, `MESH_BURST`, `MESH_SIGIL`, `MESH_WAVE` e `MESH_VORTEX`. Restano disponibili `HEALING_BEAM`, `MESH_RING`, `MESH_COLUMN` e le forme a particelle precedenti.

Le quattro pagine dell'editor permettono di cambiare durata, raggio, altezza, colore, trasparenza, rotazione e texture. Ogni forma usa i parametri pertinenti: ad esempio il numero di anelli agisce sugli anelli/onde; la rotazione sulle rune e sui vortici; la direzione di scudi e affondi proviene dal personaggio. Il renderer decide la suddivisione delle superfici. La durata degli stati persistenti è governata dalla skill: il campo Durata del preset controlla l'anteprima e le emissioni finite, non prolunga una protezione o una maledizione.

**Salva locale** conserva la bozza sul PC. **Applica al server** richiede `castigo.classes.admin` e salva il preset che riceveranno gli altri client. Un effetto persistente già attivo recepisce le modifiche al rinnovo successivo.

## Texture e materiali

Le 11 texture originali sono già incluse: anello runico, colonna curativa, sigillo sacro, sigillo oscuro, sigillo naturale, nastro, taglio, lampo, pannello di scudo, onda e fulmine. Non serve installare Unity, Photon o il pacchetto `.unitypackage` sul client/server. Il pacchetto fornito è stato usato come riferimento per le famiglie di effetti, non importato come prefab eseguibile.

Puoi sostituire le immagini attraverso un resource pack: `assets/castigoclasses/textures/vfx/`. Per una nuova immagine, ad esempio `assets/miopack/textures/vfx/scudo.png`, inserisci nell'editor `miopack:textures/vfx/scudo.png`. Il pack deve essere disponibile su tutti i client; il plugin trasmette l'identificatore, non il file PNG. I dettagli sulla struttura dei pack e sui preset locali restano nella [guida al motore 3D](MOTORE-VFX-3D.md).

I materiali sono trasparenti ed emissivi. Non includono bloom, distorsione dello schermo o illuminazione dinamica dei blocchi. La resa è una ricostruzione per Minecraft, non una riproduzione identica degli shader Unity.

## Prestazioni e collaudo

Il server usa un unico ciclo per i VFX persistenti, fino a 512 istanze, rinnovi ogni 20 tick e un tetto di 32 messaggi VFX per osservatore per tick. Le scie delle frecce vengono aggiornate ogni due tick. Il client conserva al massimo 64 animazioni, disegna fino a 32 mesh contemporanee dando precedenza alle più vicine e riduce la geometria a distanza. Tutto il rendering rimane nel client.

I test automatici verificano copertura delle 48 skill, compatibilità dei preset con il client, presenza delle texture, limiti delle geometrie, conservazione dei preset e arresto/rinnovo dei VFX persistenti. **Non è stato eseguito un collaudo visivo in un client Minecraft collegato al server.**

Per il collaudo in gioco prova: Orison su sé stessi e su un amico; Benedizione fino a scadenza; Guardia seguita da cambio classe; maledizione su un mob; Santuario/Rovina/Vortice; Doppio tiro; combo a tre colpi; meteora. Verifica gli effetti anche da un secondo client aggiornato e, se usi shader pack, controlla separatamente la loro resa.
