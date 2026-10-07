# Orison: raggio curativo — beta.6

Riferimento osservato: [CGHOW, Healing Beam in Unreal Engine Niagara](https://www.youtube.com/shorts/mFGkEniBWkY). La versione Minecraft riprende la colonna verde e i due anelli alla base con particelle animate. È una reinterpretazione per Minecraft, non una copia dello shader/mesh Unreal: densità, trasparenza e luminosità dipendono anche dalle impostazioni particelle del client.

Installa **plugin beta.6 e mod beta.6**. Mantieni core beta.9 e le dipendenze Minecraft/Fabric precedenti. Il suono originale è già incluso nel JAR della mod: non occorre creare un resource pack aggiuntivo.

## Comportamento

- Orison riuscito: doppi anelli verdi ai piedi, colonna chiara alta 3,4 blocchi e scintille ascendenti. Emissione per 1,8 secondi, poi le particelle terminano la loro dissolvenza.
- Cura su te stesso: bersaglio ed effetto sei tu. Accovacciati per selezionarti esplicitamente.
- Cura su un altro giocatore: mira a lui entro portata senza accovacciarti. La mod aggancia l'animazione al suo UUID e aggiorna la posizione mentre si muove. Le particelle già emesse possono lasciare una breve scia.
- Il suono parte una sola volta, nella posizione iniziale del destinatario. È una breve salita armonica con campane morbide, aria e coda riverberata: audio mono originale di 2,2 secondi, non estratto dal video.
- Cura annullata, bersaglio già a vita piena o requisiti mancanti: nessun effetto e nessun suono di guarigione.
- Gli osservatori con mod nel raggio previsto ricevono l'evento. I vecchi client con VFX ricevono una spirale semplificata e un suono vanilla; per la versione completa devono aggiornare la mod.

L'effetto predefinito si applica alla skill `mago_bianco_orison` con effetto `ALLY_HEAL`. Anche i YAML già esistenti lo ricevono se non hanno una personalizzazione `presentation` che lo sostituisce. Il plugin non sovrascrive i preset personalizzati. Se avevi già impostato un effetto diverso, rimuovi la sezione `presentation` di Orison per usare il nuovo default, oppure configurala così e usa `/classe reload`:

```yaml
presentation:
  enabled: true
  impact:
    enabled: true
    shape: HEALING_BEAM
    duration-ticks: 36
    radius: 0.85
    particles:
      enabled: true
      particle: DUST
      count: 1
      spread: 0
      color: '55FF66'
      size: 0.65
    sound:
      enabled: true
      id: castigoclasses:skill.orison
      category: PLAYERS
      volume: 0.75
      pitch: 1.0
```

Il renderer HEALING_BEAM usa geometria dedicata: 72 campioni ogni due tick, indipendenti da `points`, `count` e `spread`; il raggio è limitato a 1,6 blocchi. `color`, `size`, `duration-ticks`, `radius` ed enable/audio rimangono configurabili. Rispetta il budget globale client di 512 particelle per tick. Non crea entità sul server e non modifica i calcoli di cura. Se il destinatario sparisce, muore o cambia mondo, l'emissione si interrompe.

## Asset e sorgenti

Nel repository della mod:

- `src/main/resources/assets/castigoclasses/sounds/skills/orison.ogg`: audio incluso nel JAR.
- `src/main/resources/assets/castigoclasses/sounds.json`: evento `castigoclasses:skill.orison`.
- `tools/generate_orison_audio.py`: sintesi originale riproducibile con numpy e soundfile; non viene eseguita durante il gioco.
- `HealingBeam.java`: geometria dell'effetto, generata nel client senza texture aggiuntive.

Puoi sostituire l'audio con un resource pack usando gli stessi percorsi. Per modificare la forma oltre ai parametri indicati occorre ricompilare la mod.

## Collaudo

60 test plugin e 20 test mod superati, nessuno saltato: includono UUID/posizione del destinatario, cura personale, assenza di VFX a cura fallita, limiti geometrici e presenza dell'OGG registrato. OGG decodificato e verificato mono 44,1 kHz, senza clipping rilevato. Build Java 25 riuscite.

La resa visiva e sonora non è ancora stata provata in un client Minecraft reale. Per verificarla: impugna l'oggetto richiesto da Orison, perdi vita, accovacciati e lancia; poi cura un secondo giocatore ferito mentre si muove. Prova anche terza persona, particelle ridotte, volume Giocatori e due osservatori vicini. Cura, costo e cooldown devono essere identici a prima.
