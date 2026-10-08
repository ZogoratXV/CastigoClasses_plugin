# CastigoClasses beta.10 — VFX più ricchi

## Installazione

Sostituisci il JAR del plugin con `CastigoClasses-0.1.0-beta.10.jar` sul server e quello della mod con `CastigoClasses-Fabric-0.1.0-beta.10.jar` sui client. Riavvia entrambi, senza lasciare copie delle vecchie versioni nelle rispettive cartelle.

Restano Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, Java 25 e CastigoCore 2.1.0-beta.9.

Non cancellare classi, statistiche o profili. Le impostazioni personalizzate mantengono precedenza: se vedi ancora un vecchio preset, apri **F8**, seleziona classe, skill e fase, quindi **Ripristina server**. Una voce `presentation` esplicita nel file classe rimane comunque prioritaria rispetto ai preset incorporati.

## Cosa cambia

- Sigilli personali di lancio: raggio minimo 1,45 blocchi, rune più articolate e anelli sovrapposti. Gli indicatori delle zone conservano il raggio reale configurato della skill.
- Orison: sigillo di raggio 1,65 blocchi, verde giada, colonna, vapori luminosi e frammenti cubici ascendenti. Segue chi riceve la cura, anche quando è un altro giocatore. Grande intercessione usa un sigillo di raggio 2,15 blocchi e luce dorata.
- Scie: tre nastri intrecciati, nucleo chiaro, addensamenti e particelle lungo il percorso. Le frecce seguono la traiettoria reale; gli incantesimi a danno istantaneo conservano il loro tempo di impatto.
- Impatti: lampo iniziale, masse colorate in espansione, centro chiaro, onda al suolo e frammenti con dispersione tridimensionale. Le onde decorative non indicano un nuovo raggio di danno.
- Tagli: bordo interno chiaro sovrapposto alla lama colorata. Aure, onde, sigilli e vortici hanno ulteriori particelle in movimento.
- Colori più saturi e texture pixelate; nessuna dipendenza da Unity o Photon. I preset di tutte le 48 abilità sono aggiornati.

Il plugin continua a decidere bersagli, danno e durata; la mod disegna l'intero effetto. Non vengono aggiunti danni di esplosione, modifiche ai blocchi o nuove restrizioni di combattimento.

## Personalizzazione e prestazioni

F8 mantiene raggio, altezza, colore, durata, trasparenza e texture. Le particelle cubiche e le masse luminose fanno parte delle forme mesh: il pulsante delle particelle controlla l'emissione Minecraft aggiuntiva, non questi dettagli della mesh. Per nascondere un effetto disabilita la fase o azzera l'opacità della mesh.

Le texture sono incluse in `assets/castigoclasses/textures/vfx/`; puoi sostituirle tramite resource pack. `cloud.png` controlla la forma delle masse, `shard.png` le facce dei frammenti. Le texture degli anelli, dei nastri e del lampo rimangono personalizzabili dall'editor. Non è ancora presente un editor separato per ogni emettitore della composizione.

Restano 32 effetti mesh visibili contemporaneamente, priorità a quelli più vicini, limite globale di 8.192 quad e geometria ridotta oltre 24 blocchi. Le particelle mesh sono calcolate sul client senza nuovi pacchetti di rete per ogni frammento. I materiali sono emissivi: non illuminano dinamicamente i blocchi e non includono bloom.

## Verifiche

Compilazione riuscita per plugin e mod; test automatici su tutte le 48 skill, asset, limiti geometrici, animazione, dispersione, compatibilità e ciclo degli effetti. Controllata anche un'anteprima WebGL delle geometrie e texture effettive, in scala rispetto a una sagoma alta 1,85 blocchi. Questa anteprima non riproduce esattamente materiali e trasparenze di Minecraft.

**Resta da eseguire il collaudo visivo nel client Minecraft collegato al server**, soprattutto con eventuali shader pack. Prova prima Orison su te stesso e su un amico, un dardo, una meteora e Doppio tiro. In F5 si vedono meglio dimensioni e posizione dei sigilli.

Per sviluppatori: dopo i test della mod, `tools/vfx-preview.html` legge `build/vfx-preview.json` tramite un server HTTP locale e mostra esempi del renderer con controllo dei fotogrammi. È un ausilio tecnico con parametri dimostrativi, non un video registrato nel gioco.

Riferimenti artistici indicati dall'utente: [portfolio di Caleb W. Davidson](https://www.youtube.com/watch?v=dyYnofAdxQc) e [tutorial Pixel Art VFX di Gabriel Aguiar](https://www.youtube.com/watch?v=JZDlCuIpq9I). Texture e geometrie di questa versione sono originali.
