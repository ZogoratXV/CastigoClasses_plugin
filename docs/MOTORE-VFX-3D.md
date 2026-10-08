# Motore VFX 3D — beta.8

Aggiornare **plugin e mod a 0.1.0-beta.8**. Server Purpur 26.2 con CastigoCore beta.9; client Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, Java 25.

## Cosa cambia

Orison (`HEALING_BEAM`) disegna due anelli con texture e una superficie cilindrica trasparente, con trama animata verso l'alto, rotazione e dissolvenza. Sono vere geometrie renderizzate dalla mod. Le poche scintille rimaste sono particelle opzionali: disattivare **Particelle** lascia visibili anelli e colonna.

Il plugin invia destinatario, durata e parametri agli osservatori entro 64 blocchi. Ogni mod anima localmente l'effetto, interpolando la posizione del destinatario. La cura su sé stessi segue il proprio personaggio; quella su un altro giocatore segue quel giocatore. Il suono parte dalla posizione iniziale del destinatario. Danno, cura, mana e cooldown rimangono sul server.

Le forme disponibili includono `MESH_RING` (solo anelli), `MESH_COLUMN` (solo colonna) e `HEALING_BEAM` (composizione dei due). BURST, LINE, RING e SPIRAL conservano il precedente comportamento a particelle.

## Prova in gioco

1. Sostituire entrambi i vecchi JAR, riavviare server e client.
2. Entrare con il Mago Bianco. Provare Orison con vita mancante e l'oggetto richiesto dalla skill: accovacciati per la cura personale, oppure mira a un altro giocatore ferito. Controllare l'effetto anche dalla mod di un secondo giocatore.
3. Per osservare il proprio effetto completo usare la terza persona (F5); in prima persona si è dentro la colonna.
4. Aprire **F8**, selezionare classe `mago_bianco`, skill `mago_bianco_orison`, fase `IMPACT`, quindi **Dal server**.
5. Scegliere `HEALING_BEAM` se un precedente preset aveva cambiato la forma. Non occorre cancellare le configurazioni esistenti.
6. Premere **Anteprima (3 s)**: il pannello si nasconde per tre secondi, mentre l'effetto dura quanto impostato (36 tick = 1,8 secondi per Orison). L'anteprima è locale e non esegue la cura.
7. **Salva locale** conserva una bozza sul proprio PC. **Applica al server** pubblica il preset della fase selezionata; richiede `castigo.classes.admin`. Dalla successiva attivazione gli osservatori ricevono i parametri salvati.

## Pagine dell'editor

| Pagina | Controlli |
| --- | --- |
| 1 | Durata, raggio, colore delle particelle, quantità di punti, dimensione e dispersione delle particelle |
| 2 | Tipo/quantità delle particelle, suono, volume e tono |
| 3 | Altezza, opacità, rotazione, scorrimento della texture, numero e distanza degli anelli |
| 4 | Dissolvenza in entrata/uscita, raggio della colonna, texture degli anelli e della colonna, colore mesh |

Rotazione in gradi/secondo: valori negativi invertono il verso. Gli anelli alternano il verso fra loro. Scorrimento in ripetizioni/secondo: positivo sale, negativo scende. Raggio colonna è un moltiplicatore del raggio principale. Dissolvenze sono frazioni della durata: `0.15` significa il primo 15% della vita dell'effetto. Gli anelli possono essere da 0 a 4; zero lascia la sola colonna in HEALING_BEAM.

**Colore mesh** accetta sei cifre RGB, ad esempio `55FF66`, `FFD880`, `AADDFF`, oppure `auto`. Auto prende il colore delle particelle DUST, altrimenti bianco. Il colore mesh esplicito permette di usare scintille di altro tipo senza perdere il colore della geometria. Texture bianche conservano meglio questa possibilità di tintura.

Dimensione, punti e dispersione delle particelle non modificano la risoluzione delle mesh: il motore la sceglie automaticamente. Il pulsante Effetto spegne l'intera fase, mentre Particelle e Suono agiscono solo su quei componenti.

## Dove mettere le texture

Sono incluse nella mod:

```
assets/castigoclasses/textures/vfx/rune_ring.png
assets/castigoclasses/textures/vfx/healing_column.png
```

Gli identificatori nell'editor sono rispettivamente:

```
castigoclasses:textures/vfx/rune_ring.png
castigoclasses:textures/vfx/healing_column.png
```

Per aggiungere una texture senza ricompilare, usa un **resource pack compatibile con Minecraft 26.2**, attivato su tutti i client. Dentro quel pack, per esempio:

```
pack.mcmeta
assets/miopack/textures/vfx/anello_sacro.png
assets/miopack/textures/vfx/colonna_sacra.png
```

Nell'editor inserisci `miopack:textures/vfx/anello_sacro.png` e `miopack:textures/vfx/colonna_sacra.png`. Puoi anche sovrascrivere i due asset inclusi usando il namespace `castigoclasses`. Le texture devono essere PNG RGBA con trasparenza; dimensioni consigliate 128 o 256 pixel. Anello centrato nel quadrato con sfondo trasparente; colonna con trama ripetibile sui bordi. Lo scorrimento anima le coordinate della texture sul cilindro.

Il pulsante Applica invia **identificatori e parametri**, non carica file PNG dal tuo computer. Per distribuire nuove immagini, distribuisci il resource pack, anche attraverso il resource pack del server. Texture assenti sul client producono la texture mancante di Minecraft. Non mettere i PNG nella cartella dei preset JSON.

Bozze locali: `.minecraft/config/castigoclasses/vfx/`. Preset pubblicati: `plugins/CastigoClasses/vfx-overrides.json`. Un vecchio preset senza la sezione `mesh` riceve i valori predefiniti automaticamente. Ripristina server rimuove l'override della sola fase selezionata.

Anche nei file classe si può aggiungere `presentation.impact.mesh` con le chiavi `height`, `opacity`, `rotation`, `scroll`, `fadeIn`, `fadeOut`, `rings`, `ringGap`, `columnRadius`, `ringTexture`, `columnTexture`, `tint`. I preset dell'editor hanno precedenza sui file classe finché non vengono ripristinati.

## Prestazioni e limiti di questa versione

Il renderer usa le fasi di estrazione e disegno di Fabric 26.2: il disegno legge copie immutabili delle geometrie e non accede direttamente alle entità. Massimo 32 effetti mesh per fotogramma, selezionando prima i più vicini, tetto di 8192 quadrilateri e meno segmenti oltre 24 blocchi. Le animazioni hanno durata massima di 40 tick, opacità e dimensioni validate. Il server invia eventi iniziali, non vertici a ogni fotogramma.

Materiale trasparente emissivo: resta luminoso al buio, rispetta gli ostacoli, non illumina realmente i blocchi e non aggiunge bloom. L'effetto non richiede shader pack. L'interazione visiva con shader pack esterni richiede un collaudo dedicato.

È una prima base di composizione di anelli e colonne con animazione temporale. Non include ancora un editor a nodi, una timeline trascinabile, importazione di modelli Unity/FBX, shader personalizzati o un sistema arbitrario di emettitori. I prefab e gli shader Unity non possono essere copiati direttamente nella mod.

## Verifica

Build Java e test automatici coprono generazione e limiti delle geometrie, dissolvenze, UV, qualità a distanza, validazione e persistenza dei preset, invio di Orison sul destinatario. Le texture originali sono riproducibili con `tools/generate_vfx_textures.py`. Il risultato visivo completo richiede la prova in un client Minecraft: la compilazione non sostituisce quel collaudo.
