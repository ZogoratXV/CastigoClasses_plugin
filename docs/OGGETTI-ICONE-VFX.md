# Oggetti richiesti, GUI admin, icone e VFX — beta.5

Installa plugin **0.1.0-beta.5** sul server e mod **0.1.0-beta.5** sui client. Core resta **2.1.0-beta.9**, Minecraft **26.2**, Fabric Loader **0.19.5** e Fabric API **0.161.0+26.2**. La nuova mod serve per disegnare le icone PNG; la beta.4 può ancora usare le skill ma non mostra queste nuove icone.

## Cure e attacchi senza party

CastigoClasses non richiede più team o party: puoi curare qualsiasi giocatore vivo e visibile, nella portata della skill, oppure te stesso. Accovacciarsi seleziona se stessi. Non è necessario appartenere allo stesso gruppo LuckPerms. Ricucitura del Mago originale rimane una cura personale; usa Orison/Intercessione per curare altri.

Anche gli attacchi delle skill non controllano più l'appartenenza a una squadra. La vecchia opzione `combat.pvp` non viene più letta, anche se rimane nel tuo config preesistente. Restano rispettati PvP del mondo, regioni WorldGuard, modalità creativa/spettatore e annullamenti di danno degli altri plugin. Le squadre vanilla già create non vengono cancellate: se un altro sistema o Minecraft impedisce il fuoco amico, CastigoClasses non forza il danno attraverso quel blocco. Per un team creato durante i test precedenti puoi usare `/team modify gruppo friendlyFire true` oppure uscirne con `/team leave Nome`.

## Assegnare un oggetto a una skill

1. Entra con `castigo.classes.admin` (gli operatori lo hanno).
2. Metti nell'inventario l'oggetto vanilla o ItemsAdder da usare.
3. Esegui `/classe admin` e seleziona la classe, poi l'abilità.
4. Seleziona **mano principale** oppure **mano secondaria**, per esempio per lo scudo.
5. Clicca l'oggetto nel tuo inventario nella parte bassa della finestra. Il plugin ne legge l'identità, la salva e lascia l'oggetto nell'inventario. Il pulsante della mano da solo non salva: serve il clic sull'oggetto.

La scelta si applica a quella skill di quella classe. Ripeti per le altre abilità: non viene imposta automaticamente a tutta la classe. Le modifiche sono immediate e persistenti in `plugins/CastigoClasses/skill-equipment.yml`. Le modifiche manuali a questo file si caricano con `/classe reload`. Cambiamenti dalla GUI terminano gli effetti temporanei attivi per applicare coerentemente le nuove regole.

Fino alla personalizzazione, sono richiesti questi oggetti precisi:

| Tipo | Oggetto predefinito | Mano |
|---|---|---|
| Mago Bianco, Mago Nero e Mago originale | Bastone vanilla (`minecraft:stick`) | Principale |
| Guerriero con scudo | Scudo vanilla (`minecraft:shield`) | Secondaria |
| Guerriero a due mani | Spada di ferro vanilla (`minecraft:iron_sword`) | Principale |
| Arciere | Arco vanilla (`minecraft:bow`) | Principale |

Il default dipende dal campo `weapon` della skill; le skill ANY dell'Arciere usano l'arco. Non è più possibile lanciare a mani vuote. Quando manca l'oggetto, il messaggio indica ID e mano e non viene consumata risorsa. Dopo l'assegnazione dalla GUI conta l'oggetto specifico scelto, non la vecchia categoria generica spada/ascia. I tiri richiedono comunque un vero arco come materiale base, anche se ItemsAdder, nella mano principale; le altre meccaniche possono usare oggetti personalizzati. La guardia Castigo può avere un oggetto custom; per la parata vanilla serve uno scudo con il normale comportamento di scudo.

Per ItemsAdder viene usata l'API pubblica `CustomStack.byItemStack` e `getNamespacedID`: per esempio `itemsadder:castigo:bastone_sacro`. Non viene confrontato il nome visualizzato, né solo il materiale sottostante. Un bastone vanilla non sostituisce il bastone ItemsAdder richiesto. Se ItemsAdder non è disponibile o la sua API non risponde, le skill che richiedono i suoi oggetti rimangono bloccate. Riferimento: [API ufficiale CustomStack](https://lonedev6.github.io/API-ItemsAdder/dev/lone/itemsadder/api/CustomStack.html).

Esempio di salvataggio (normalmente lo crea la GUI):

```yaml
classes:
  mago_bianco:
    mago_bianco_orison:
      hand: MAIN
      item: itemsadder:castigo:bastone_sacro
      icon: texture:castigo:textures/gui/skills/orison.png
```

## Icone personalizzate

Le icone delle skill sono indipendenti dall'oggetto necessario a lanciarle. Puoi usare un oggetto vanilla come icona, oppure un PNG quadrato trasparente: suggeriti 32×32 o 64×64 pixel, visualizzato in 16×16 nella barra. È un'immagine statica; non un modello 3D ItemsAdder.

Per impostarla:

```text
/classe icona mago_bianco mago_bianco_orison texture:castigo:textures/gui/skills/orison.png
```

Per tornare all'icona originale:

```text
/classe icona mago_bianco mago_bianco_orison reset
```

Il completamento del comando suggerisce classi e skill. La GUI mostra l'icona configurata e la sintassi del comando. Puoi anche scrivere `icon: texture:castigo:textures/gui/skills/orison.png` nella skill dentro il YAML della classe e ricaricare; l'override salvato dal comando ha precedenza finché non usi `reset`.

Il PNG deve trovarsi **nel resource pack caricato dal client** a questo percorso:

```text
CastigoAssets/
  pack.mcmeta
  assets/
    castigo/
      textures/
        gui/
          skills/
            orison.png
      sounds/
        skills/
          cura.ogg
      sounds.json
```

`castigo` è il namespace, `textures/gui/skills/orison.png` il percorso relativo. Usa minuscole. Non mettere il PNG nella cartella dei plugin aspettandoti che la mod lo scarichi: il protocollo invia l'identificatore, non l'immagine. Se manca il file, appare l'icona di ripiego.

Per le prove locali metti cartella/ZIP in `.minecraft/resourcepacks` dell'istanza che stai usando e attivalo nelle opzioni. Per tutti i giocatori, distribuisci lo stesso pack tramite il server o incorporane i file nel pack servito da ItemsAdder. Il risultato distribuito deve contenere la struttura `assets/...` qui sopra: segui la procedura della tua versione ItemsAdder per aggiungere file e rigenerare il pack. Accettare il pack sul client è necessario. All'interno dello ZIP `pack.mcmeta` deve essere alla radice, non dentro una cartella aggiuntiva.

Per Minecraft 26.2, `pack.mcmeta` minimo:

```json
{"pack":{"description":"Castigo: icone e suoni","min_format":[88,0],"max_format":[88,0]}}
```

La versione risorse 88.0 è riportata nelle [note ufficiali Minecraft 26.2](https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2). Dopo la sostituzione di un asset già caricato, ricarica il resource pack sul client. In alternativa gli stessi file possono essere inclusi in `src/main/resources/assets/castigo/` della mod, ma richiede ricompilare e distribuire un nuovo JAR: il pack permette di cambiarli senza ricompilare.

## Come lavorano gli effetti visivi

Il percorso è: **lancio della skill → controllo e risultato sul server → messaggio grafico → animazione locale della mod**.

Il plugin decide danno, cura, costo, ricarica e bersagli. Quando l'esecutore emette una fase grafica, invia alla mod forma, coordinate, particella, colore, durata e suono. La mod genera nel proprio mondo client le particelle e riproduce il suono nella posizione indicata. Anche i giocatori vicini con la mod ricevono l'evento, entro i limiti di distanza e quantità già implementati. La grafica non decide chi viene colpito.

Per gli effetti attualmente supportati **non occorrono file grafici nuovi**: vengono usate particelle Minecraft, disposte come `BURST`, `LINE`, `RING` o `SPIRAL`. Il preset si mette nel file della classe sul server, per esempio `plugins/CastigoClasses/classes/mago_bianco.yml`, sotto la singola skill:

```yaml
skills:
  mago_bianco_orison:
    # Mantieni tutti gli altri campi della skill.
    presentation:
      enabled: true
      impact:
        shape: RING
        points: 24
        radius: 1.2
        duration-ticks: 10
        particles:
          enabled: true
          particle: DUST
          count: 1
          spread: 0.02
          color: 'FFE8A0'
          size: 1.1
        sound:
          enabled: true
          id: castigo:skills.cura
          category: PLAYERS
          volume: 0.8
          pitch: 1.0
```

Poi `/classe reload`. Questo esempio funziona sulla fase `impact` della cura. Un preset non crea da solo una nuova fase: alcune skill delle discipline, come guardie/scatti/tiri, non hanno ancora agganci grafici dedicati. La parte estetica di tutte le 40 skill resta da completare; non basta aggiungere una sezione `trail` a qualsiasi skill per far partire un raggio. La guida [VFX-CLIENT.md](VFX-CLIENT.md) elenca parametri e limiti; la mappa delle fasi del vecchio Mago non è una garanzia per tutti i nuovi esecutori.

Per il suono personalizzato dell'esempio aggiungi `assets/castigo/sounds/skills/cura.ogg` al pack e questo contenuto a `assets/castigo/sounds.json` (integrandolo con le eventuali altre voci):

```json
{
  "skills.cura": {
    "sounds": [{"name": "castigo:skills/cura", "stream": false}]
  }
}
```

Il nome dell'evento è `castigo:skills.cura`; il file è `sounds/skills/cura.ogg`. Preferisci audio mono per i suoni posizionali. I suoni vanilla, per esempio `minecraft:block.amethyst_block.chime`, non richiedono file aggiuntivi.

**Un PNG dell'icona non diventa un effetto 3D.** Cerchi con rune testurizzate, modelli animati, proiettili con aspetto esclusivo o nuovi tipi di particella richiedono un renderer aggiuntivo nella mod e i relativi asset nel pack/mod. Attualmente non esiste una cartella dove mettere un file `.geo.json`, un video o un generico preset di un altro plugin e farlo eseguire automaticamente. Possiamo aggiungere questi renderer nella fase VFX successiva senza spostare il combattimento sul client.

## Verifica

Le build e i test automatici coprono cure senza team, ammissibilità PvP, oggetto/mano richiesti, persistenza, permessi e clic della GUI senza consumo, oltre alle regressioni precedenti. La mod compila con il nuovo rendering PNG. Restano da provare nel tuo server l'API della versione ItemsAdder installata, la distribuzione del pack, la resa grafica e l'interazione con plugin che annullano danni/cure. Non è stato avviato un client Minecraft reale per questa verifica.
