# VFX e suoni gestiti dalla mod

Richiede plugin **0.1.0-beta.3**, CastigoCore **2.1.0-beta.9**, mod **0.1.0-beta.4**, Minecraft 26.2. Il plugin decide l'esecuzione delle skill e invia eventi; **la mod crea e anima tutte le particelle delle skill e riproduce i loro suoni**. Il server non invia particelle vanilla per queste animazioni. Il suono di avanzamento livello resta un normale suono server.

Il core beta.9 non espone più `Protection.interact`: il plugin preserva il controllo WorldGuard INTERACT tramite un adattatore, inclusi bypass e blocco in caso di errore. Non viene sostituito con BUILD. Salvataggi atomici e attività periodiche continuano a utilizzare CoreApi. ParticleStyle del core valida i preset; il rendering e i limiti effettivi sono nella mod.

## Configurazione

Dentro ogni voce di `skills` nei file `plugins/CastigoClasses/classes/*.yml` aggiungi `presentation`. Un esempio completo viene creato in `plugins/CastigoClasses/examples/presentation.yml.example`. Copia quella sezione sotto la skill da personalizzare, mantenendo l'indentazione YAML, e usa `/classe reload`.

```yaml
skills:
  dardo_arcano:
    # Mantieni name, effect, cost e gli altri parametri della skill.
    presentation:
      enabled: true
      trail:
        shape: LINE
        points: 32
        duration-ticks: 6
        particles:
          enabled: true
          particle: DUST
          count: 1
          spread: 0.03
          color: 'AA66FF'
          size: 1.1
      impact:
        shape: RING
        radius: 1.2
        duration-ticks: 8
        particles:
          enabled: true
          particle: DUST
          count: 1
          color: 'E8D8FF'
        sound:
          enabled: true
          id: minecraft:block.amethyst_block.chime
          category: PLAYERS
          volume: 0.8
          pitch: 1.4
```

Le proprietà omesse mantengono il preset dell'effetto della skill; i vecchi file continuano a funzionare. Un errore viene rifiutato al caricamento/reload, conservando il catalogo precedente in caso di reload fallito. Non vengono sovrascritti i file delle classi già presenti.

| Fase | Quando viene inviata |
|---|---|
| `cast` | Al lancio valido di qualsiasi skill, dalla posizione iniziale; disattivata nel preset |
| `trail` | Percorso di dardo/folgore/sfera/teletrasporto; anello della nova; discesa della meteora |
| `impact` | Contatto/effetto di tutte le skill; per Meteora dopo il ritardo di un secondo |
| `telegraph` | Preavviso a terra della Meteora |
| `hit` | Quando la barriera assorbe effettivamente danno |

Una fase non usata dall'esecutore della skill non parte solo perché è configurata. Il VFX di lancio non crea un tempo di preparazione: le durate qui sono solo cosmetiche. Un raggio animato non è un nuovo proiettile con collisione; le meccaniche restano quelle della skill.

Ogni fase accetta `enabled`, `shape`, `points`, `duration-ticks`, `radius`, `particles` e `sound`. `presentation.enabled: false` spegne tutto; `particles.enabled` e `sound.enabled` separano immagine e audio.

| Proprietà | Valori e significato |
|---|---|
| `shape` | `BURST` (emissione puntuale), `LINE` (percorso animato), `RING` (cerchio tracciato), `SPIRAL` (elica di due giri alta 2 blocchi) |
| `points` | 2–32 campioni geometrici; ignorato da BURST |
| `duration-ticks` | 1–40 tick client, distribuisce il tracciamento nel tempo; BURST emette subito. Non cambia la vita intrinseca delle singole particelle |
| `radius` | 0,1–12 blocchi, per anello/spirale. Non modifica il raggio dei danni |
| `particles.particle` | Nome Bukkit: `DUST`, `FLAME`, `SNOWFLAKE`, `HAPPY_VILLAGER`, `END_ROD`, ecc. DUST oppure tipi senza dati aggiuntivi accettati dal core |
| `particles.count` | 1–32 particelle per campione; consigliato 1 sulle forme |
| `particles.spread` | 0–2, dispersione casuale |
| `particles.color` / `size` | RGB a 6 cifre e dimensione 0,05–4, per DUST |
| `sound.id` | Identificatore `namespace:nome`, vanilla o presente nei resource pack/asset client |
| `sound.category` | Categoria audio Minecraft, normalmente PLAYERS |
| `sound.volume` / `pitch` | Volume 0–2, tono 0,5–2 |

Per introdurre nuove forme di rendering, texture esclusive o particelle con dati speciali serve un'estensione della mod e, dove necessario, del protocollo. La configurazione non scarica né esegue codice. Suoni personalizzati sono indirizzabili per ID, ma il file audio deve essere distribuito tramite mod o resource pack: un ID da solo non crea l'audio. Questa build usa gli asset Minecraft esistenti e non include nuovi file audio o texture. Vedi [documentazione suoni](https://docs.papermc.io/adventure/sound/) e [particelle Fabric](https://docs.fabricmc.net/develop/rendering/particles/creating-particles).

## Rete e prestazioni

La mod dichiara `clientVfx: 1` nel saluto iniziale. Il plugin invia un messaggio compatto per fase ai soli client compatibili nello stesso mondo entro 64 blocchi dal segmento; nessun pacchetto per ogni punto dell'animazione. I giocatori senza questa versione della mod non vedono né sentono gli effetti delle skill, ma danni/cure restano gestiti dal server. Le mod precedenti mantengono HUD/skill senza i nuovi effetti.

Limiti: 32 eventi per destinatario/tick server; 64 animazioni attive sul client; 512 emissioni di particelle e 8 suoni per tick client. In sovraccarico vengono scartati effetti/campioni cosmetici, non i risultati delle abilità. I contatori si riferiscono alle particelle emesse direttamente: alcuni tipi vanilla generano ulteriori particelle internamente. Usare DUST e count 1 per VFX frequenti.

Coordinate, identificatori e quantità sono validati. Il mondo deve corrispondere allo stato ricevuto; cambio mondo, disconnessione o stato scaduto eliminano le animazioni in attesa. Le impostazioni grafiche/audio Minecraft possono ridurre ciò che il giocatore vede o sente. I limiti riducono il lavoro previsto ma non sostituiscono una misurazione multiplayer.

## Aggiornamento e verifica

Sostituisci a server/client spenti Core con beta.9, plugin con beta.3 e mod con beta.4; conserva Fabric API. Non lasciare due versioni dello stesso componente. Profili, punti assegnati, classi personalizzate e HUD restano compatibili.

Le build e i test automatici verificano configurazione, protocollo, geometria e persistenza col nuovo core. La verifica grafica in Minecraft e la prova su un server con WorldGuard/anticheat reali restano da effettuare.
