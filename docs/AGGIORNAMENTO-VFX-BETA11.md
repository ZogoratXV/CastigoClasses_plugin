# Beta.11 — modelli articolati e gesti delle armi

## Cosa ho ricavato dai pacchetti

I sei archivi AWAKENED contengono 273 file Blockbench animati, comprese varianti e componenti di supporto. Nei file esaminati gli effetti combinano superfici con texture, modelli tridimensionali, parti con trasformazioni animate, suoni, spostamenti e tempi stabiliti nelle skill MythicMobs. Per esempio Brutal Combo alterna tagli, affondi e rotazioni; Sacred Combo contiene animazioni distinte per colpo laterale, onda e impatto al suolo.

Queste sequenze non sono direttamente eseguibili dalla nostra mod Fabric: la mod deve disegnare e animare i componenti, mentre CastigoClasses mantiene il controllo della skill. Ho usato i pacchetti come riferimento strutturale e stilistico, seguendo la tua scelta di creare nuova grafica e riutilizzare l'audio.

## Contenuto della revisione

- Sette composizioni tridimensionali originali: corona di guarigione, cristalli dei sigilli, schegge radiali d'impatto, pannelli di barriera, marchio orbitante, frammenti convergenti del vortice e creste dell'onda.
- Ogni composizione ha parti con posizione, dimensioni, rotazione, ritardo, scala e trasformazione finale. Tre texture originali distinguono superfici sfaccettate, piume e stemmi delle barriere. Le parti si aggiungono a rune, scie e particelle già presenti.
- Sei gesti dell'arma: magia, tiro con arco, scudo, affondo, fendente pesante e taglio. Vengono applicati all'oggetto realmente impugnato; non sostituiscono il suo modello e possono quindi accompagnare oggetti ItemsAdder già correttamente visualizzati dal client.
- Preparazione, rilascio e arresto provengono dal server. Una skill respinta per equipaggiamento non anima l'arma. Cambio classe e interruzione della preparazione inviano l'arresto. Sul client la posa termina anche cambiando oggetto, morendo, cambiando mondo o scollegandosi.
- Prima persona: trasformazione della mano e dell'oggetto. Terza persona: posa delle braccia e dell'oggetto impugnato, anche per gli osservatori vicini con mod aggiornata. Supporto a mano principale sinistra e oggetti in secondaria. L'uso vanilla dell'oggetto mantiene la precedenza.
- 24 eventi sonori importati localmente dai sei pacchetti SamusDev; i preset delle 48 abilità usano i suoni pertinenti. Non tutti i campioni sono usati da un preset iniziale: quelli aggiuntivi restano selezionabili nell'editor.

Questa revisione non aggiunge le classi Assassino o Negromante, non importa le skill MythicMobs e non replica le animazioni complete dei loro personaggi. I nuovi gesti muovono braccia e arma; non sono un sistema di animazione integrale di gambe e torso. Sono presentazione cosmetica: non generano attacchi vanilla, nuovi danni o frecce aggiuntive.

## Installazione

1. Sostituisci il plugin con `CastigoClasses-0.1.0-beta.11.jar`.
2. Sostituisci la mod con `CastigoClasses-Fabric-0.1.0-beta.11.jar` su ogni client.
3. Riavvia server e client senza mantenere due versioni dello stesso componente.

La mod consegnata include già i 24 suoni: non servono altri pacchetti per sentirli. Mantieni CastigoCore 2.1.0-beta.9, Minecraft 26.2, Fabric Loader 0.19.5 e Fabric API 0.161.0+26.2.

Non cancellare profili, classi o statistiche. Se un preset precedentemente salvato nasconde il nuovo suono o effetto, in F8 seleziona classe, skill e fase e usa **Ripristina server**. Le personalizzazioni `presentation` scritte direttamente nei file classe mantengono precedenza.

## Sorgenti e asset

Il codice, i modelli e la texture originali possono essere ricostruiti dal repository. I modelli sono in `assets/castigoclasses/models/vfx/choreography.json`, generati da `tools/author_vfx_models.py`; non sono file Blockbench copiati dagli archivi. Al momento la loro struttura si modifica nei sorgenti, non nell'editor F8. F8 continua a controllare i parametri globali dell'effetto.

Gli audio SamusDev sono mantenuti nella cartella locale esclusa da Git `local-audio/` e nel JAR consegnato; non vengono pubblicati nel repository né nel JAR dei sorgenti. Il comando `python tools/import_local_audio.py CARTELLA_ARCHIVI` li ricostruisce dai tuoi ZIP. Una compilazione priva di questi file usa un suono Minecraft sostitutivo. Crediti audio: SamusDev / samus2002, https://samusdev.com/ e https://www.youtube.com/@SamusDev.

## Verifiche e prova in gioco

Compilazione riuscita e 112 test superati. I test verificano le 48 abilità, asset, limiti geometrici, ritorno delle pose a riposo, selezione dei gesti, rilascio dopo preparazione e annullamento al cambio classe. L'anteprima tecnica delle nuove geometrie è stata controllata.

**Non è stato eseguito un collaudo in Minecraft delle nuove pose e dei punti di integrazione del renderer.** La compilazione non prova la compatibilità visiva con altre mod di animazione, shader o tutti i modelli ItemsAdder. Per il primo test usa Orison, la cura preparata, un affondo, Guardia, un fendente e un tiro, sia in prima persona sia in F5; verifica poi da un secondo client. Questa build permette di valutare concretamente la nuova direzione, non certifica ancora la qualità finale delle animazioni nel tuo modpack.
