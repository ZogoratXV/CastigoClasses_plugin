# CastigoClasses beta.13 — HUD LuckPerms, casting e icone

Sostituire entrambi i JAR: `CastigoClasses-0.1.0-beta.13.jar` sul server e `CastigoClasses-Fabric-0.1.0-beta.13.jar` nel client. Riavviare entrambi senza lasciare le versioni precedenti nelle cartelle. Restano Minecraft/Purpur 26.2, Java 25, CastigoCore 2.1.0-beta.9, Fabric Loader 0.19.5 e Fabric API 0.161.0+26.2.

## Editor rimosso

L'editor VFX in gioco e il suo keybind F8 sono rimossi. Il server ignora anche le richieste di modifica VFX provenienti dalle vecchie mod. Restano il menu personaggio K, la disposizione delle skill, i punti attributo e le GUI admin per equipaggiamento, icone e HUD. Gli effetti VFX continuano a funzionare. Gli override già salvati vengono conservati; per modificarli si possono ancora usare i file sul server.

## Casting time corretto

Il problema era nel percorso di esecuzione delle abilità originali (`BOLT`, `LIGHTNING`, `FIREBALL`, `FROST_NOVA`, `BLINK`, `WARD`, `HEAL`, `METEOR`): saltava la preparazione. Questo riguardava anche Dardo consacrato, che usa `BOLT`. Ora tutte le skill passano dalla stessa gestione del casting.

Imposta il valore **dentro la singola skill** in `plugins/CastigoClasses/classes/<classe>.yml`, non nella radice di `config.yml`:

```yaml
skills:
  mago_bianco_dardo_consacrato:
    # mantieni gli altri campi della skill già presenti
    preparation-seconds: 1.5
```

Usa l'ID reale presente nel tuo file. Salva, poi esegui `/classe reload`. Il messaggio deve confermare che le classi sono state ricaricate. Un errore di configurazione conserva l'ultima versione valida e segnala il problema.

Valori consentiti: **0–30 secondi**, con punto decimale, senza virgolette. Zero significa istantanea. Sono accettati anche `casting-time`, `casting-time-seconds` o `cast-time-seconds`, sempre in secondi: se presente un alias, prevale su `preparation-seconds` anche quando il file originale contiene zero. Usa un solo nome per evitare ambiguità. Non confondere `duration-seconds` (durata dell'effetto) o `cooldown-seconds` (ricarica) con il casting.

Barra di lancio, interruzioni, selezione del bersaglio e aura funzionano anche sulle abilità originali. I proiettili/effetti vengono eseguiti dopo la preparazione; per Meteora resta inoltre il suo avvertimento a terra di un secondo prima dell'impatto.

## HUD per gruppi LuckPerms

Apri `/classe admin` → **HUD gruppi LuckPerms**, oppure `/classe hud`. È richiesta `castigo.classes.admin`.

1. Scegli un gruppo caricato da LuckPerms o usa **Aggiungi gruppo** e scrivine il nome.
2. Clicca **Layout PNG** e scrivi l'identificatore, per esempio `castigo:textures/gui/hud/nobile.png`.
3. Configura i colori, lo stile della cornice e le coordinate degli elementi. Ogni campo si modifica cliccando e scrivendo il valore nella chat privata del menu. `annulla` torna indietro; l'input scade dopo due minuti.
4. Il salvataggio aggiorna subito i client collegati e persiste in `plugins/CastigoClasses/hud-themes.json`.

Le appartenenze ereditate e i contesti attivi di LuckPerms sono considerati. Fra i gruppi configurati vince la **priorità più alta** (0–10000); a pari priorità vince il gruppo primario, poi il nome in ordine alfabetico. Se non c'è un gruppo configurato, viene usato `default`. Senza LuckPerms viene usato lo stesso tema di riserva. La riga che mostra il gruppo continua a indicare il gruppo primario.

**Stile cornice:** `MEDIEVAL`, `FLAT` o `NONE`. Per un PNG con cornice già disegnata scegli `NONE`. **Colore risorsa:** `class` mantiene il colore della risorsa della classe; gli altri colori sono RGB esadecimali, ad esempio `FFD700`. Nel campo PNG, `nessuna` rimuove l'immagine. Se il client non trova la texture resta una cornice di riserva.

### Dove mettere i tuoi layout ItemsAdder

Esempio sul server:

```text
plugins/ItemsAdder/contents/castigo_hud/resourcepack/assets/castigo/textures/gui/hud/nobile.png
```

Nella GUI Castigo inserisci:

```text
castigo:textures/gui/hud/nobile.png
```

Rigenera il resource pack con `/iazip` e distribuiscilo ai client usando l'hosting ItemsAdder già configurato. La struttura `contents/<pacchetto>/resourcepack/assets` e la rigenerazione seguono la [procedura ufficiale ItemsAdder](https://wiki.itemsadder.com/adding-content/merge-resourcepacks/). I giocatori devono caricare il pack aggiornato.

La mod legge direttamente il PNG nel resource pack e disegna sopra testa, nome/livello, classe, gruppo e barre aggiornate. Non serve registrare un oggetto ItemsAdder o un'immagine font per questo layout. Nome, valori numerici e riempimenti delle barre devono restare dinamici: disegna nel PNG sfondo e decorazioni.

### Dimensioni e coordinate per disegnare il layout

Base: **172×64 pixel GUI**, ancorata a 5 pixel dal bordo superiore e sinistro. Puoi realizzare il PNG anche a risoluzione doppia (344×128): sarà adattato all'area configurata. Tutte le coordinate della GUI sono relative all'angolo superiore sinistro della HUD.

| Elemento | Posizione/dimensione predefinita |
|---|---|
| Testa | X 8, Y 8, lato 24 |
| Testi | X 40 |
| Nome e livello | Y 7 |
| Classe | Y 18 |
| Gruppo primario | Y 28 |
| Barre | X 6, larghezza 160 |
| Vita | Y 39, riempimento alto 8 |
| Risorsa | Y 50, riempimento alto 8 |
| Esperienza | Y 60, altezza 2 |

La GUI permette di modificare larghezza/altezza HUD, posizione e dimensione della testa, posizioni dei testi, posizione/larghezza delle barre e posizione dell'esperienza. Quando cambi le dimensioni della HUD adatta anche gli elementi interni: le coordinate non si ridistribuiscono automaticamente.

## Le 40 icone sono già integrate

I PNG del tuo ZIP sono inclusi nel JAR della mod consegnato e abbinati alle 40 abilità delle cinque discipline. Per vederli nella barra skill basta aggiornare plugin e mod; non devi installare lo ZIP in ItemsAdder per questo utilizzo.

Identificatore di esempio:

```text
texture:classi_regno:textures/item/rg_mago_bianco_orison_di_cura.png
```

Le personalizzazioni salvate nella GUI hanno precedenza: se una skill conserva una vecchia icona, apri **Configura icona → Ripristina icona originale** per usare l'icona integrata. Le icone PNG esplicite nei file classe restano anch'esse prioritarie. Le otto abilità del mago originale non hanno immagini corrispondenti nel pacchetto e conservano le icone precedenti.

Sono stati importati esclusivamente i 40 PNG, senza i mapping COAL/CustomModelData dello ZIP. Un resource pack può sostituire le immagini mantenendo gli stessi percorsi. Nel codice pubblico è presente l'importatore `tools/import_skill_icons.py`; i PNG forniti e l'audio locale sono esclusi dai sorgenti pubblicati e dal sources JAR. Per ricompilare una mod con queste icone, esegui l'importatore sullo ZIP originale prima della build, tenendo i due repository affiancati.

## Verifiche

Build di plugin e mod completate; 87 test plugin e 40 test mod superati senza test saltati. Coperti casting delle abilità originali, salvataggio HUD dalla GUI, priorità fra gruppi, validazione dei percorsi, selezione delle icone e rifiuto delle richieste del vecchio editor. LuckPerms è verificato rispetto all'API e la selezione dei temi con test; resta da collaudare nel tuo server con i gruppi reali e il pack contenente i layout che creerai.
