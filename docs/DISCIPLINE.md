# Base giocabile delle cinque discipline — plugin beta.5

Richiede Purpur Minecraft 26.2, Java 25 e CastigoCore 2.1.0-beta.9. Usa la mod Fabric **0.1.0-beta.5** per le icone PNG, Fabric Loader 0.19.5 e Fabric API 0.161.0+26.2. Le 40 abilità hanno esecutori server: danni, cure, stati, guardie, movimento e frecce. Il lavoro grafico dedicato è rinviato; resta disponibile il collegamento VFX plugin → mod esistente.

## Aggiornamento e prova rapida

Arresta il server, sostituisci il vecchio JAR CastigoClasses con la beta.5 e avvia con il core beta.9. I cinque YAML mancanti vengono aggiunti automaticamente; quelli già presenti non vengono sovrascritti. Profili, punti e disposizione degli slot rimangono salvati. Le vecchie installazioni conservano la propria `default-class`; sulle nuove è `mago_bianco`.

Da console o come staff:

```text
/classe set NomeGiocatore mago_bianco
/classe livello NomeGiocatore 50
```

Ripeti il primo comando con `mago_nero`, `guerriero_scudo`, `guerriero_due_mani` o `arciere`. Il livello 50 sblocca tutte le abilità; cambiare classe non azzera i cooldown. Usa R per la barra skill, K per riordinare gli slot e assegnare punti; i tasti sono rimappabili. Senza mod puoi usare `/classe skill 1` fino a `8`.

## Progressione e attributi

| Grado | Livello predefinito | Abilità disponibili |
|---|---:|---|
| Aspirante | 1 | Nessuna; azioni vanilla |
| Apprendista | 5 | 1–2 |
| Praticante | 15 | 1–4 |
| Esperto | 30 | 1–6 |
| Maestro | 50 | 1–8 |

I gradi sono automatici e appaiono nel riepilogo `/classe`; non sono sottoclassi né promozioni manuali RP. Modifica gli `unlock-level` delle coppie nei YAML per cambiare le soglie. L'ordine nella barra del giocatore è indipendente dall'ordine delle coppie nella definizione.

Ogni classe ha valori iniziali e crescita per livello dei sette attributi. I punti attribuiti dal giocatore si aggiungono a questi valori e influenzano il combattimento. Frequenza, quantità e bonus dei punti rimangono in `stat-points` nel config. `/classe livello` assegna gli arretrati, ma non revoca punti scendendo né li duplica risalendo.

La potenza di una skill è `power + intelligenza × intelligence-scale + forza × strength-scale + destrezza × dexterity-scale + attacco × attack-scale`, con limite 10000. Danni finali, armatura, guardie e difesa dipendono poi dagli eventi di combattimento. Le formule di attacco vanilla, velocità e difesa MMO restano attive. `mana` è la capacità della risorsa: il nome mostrato può essere Fede, Energia oscura, Vigore o Concentrazione.

## Cure libere e oggetti richiesti (beta.5)

Le cure non richiedono più squadre; gli attacchi delle skill non filtrano i team. Il PvP continua a rispettare mondo, regioni e altri plugin. Ogni abilità richiede un oggetto specifico nella mano configurata. Usa `/classe admin` per scegliere classe e skill e assegnare un oggetto vanilla o ItemsAdder dal tuo inventario.

Per impostazioni predefinite, GUI, icone PNG e file VFX/suoni, consulta [OGGETTI-ICONE-VFX.md](OGGETTI-ICONE-VFX.md), che sostituisce le regole di alleanza ed equipaggiamento della beta.4. Skill e progressione elencate sotto restano disponibili.

## Le 40 abilità

Gli slot seguenti sono l'ordine iniziale; ogni giocatore può cambiarlo.

### Mago Bianco — Fede

| Slot | Abilità | Effetto |
|---:|---|---|
| 1 | Orison di cura | Cura immediata su alleato o se stesso |
| 2 | Dardo consacrato | Danno diretto a un nemico nella mira |
| 3 | Benedizione rigenerante | Cura ogni secondo per sei secondi, senza accumulo |
| 4 | Purificazione | Rimuove maledizioni Castigo, veleno, wither, lentezza e debolezza |
| 5 | Vincolo del custode | Trasferisce al guaritore il 30% del danno da entità di un alleato vicino |
| 6 | Luce respingente | Allontana i nemici vicini |
| 7 | Grande intercessione | Cura maggiore dopo 1,5 secondi di preparazione |
| 8 | Santuario di luce | Area con riduzione del 25% dei danni da entità agli alleati |

### Mago Nero — Energia oscura

| Slot | Abilità | Effetto |
|---:|---|---|
| 1 | Dardo maledetto | Colpo iniziale e danno nel tempo |
| 2 | Malocchio | Colpo lieve e riduzione del 35% della difesa MMO; sui mob aumenta il danno ricevuto |
| 3 | Consunzione | Colpo iniziale e impulsi di danno ogni secondo |
| 4 | Terrore sussurrato | Interrompe la tecnica, rallenta e allunga le preparazioni del 50% |
| 5 | Cerchio della rovina | Zona mirata sul terreno che danneggia ogni secondo |
| 6 | Sigillo del tormento | Colpo lieve e riduzione del 50% delle cure |
| 7 | Patto del sangue | Cura per il 50% della salute effettivamente sottratta |
| 8 | Vortice delle tenebre | Attira verso una zona i nemici raggiungibili, rispettando la resistenza al respingimento |

### Guerriero con scudo — Vigore

| Slot | Abilità | Effetto |
|---:|---|---|
| 1 | Affondo disciplinato | Attacco a corta portata |
| 2 | Urto di scudo | Danno, spinta, rallentamento e interruzione |
| 3 | Guardia salda | Riduce del 35% i danni frontali, rallenta il movimento |
| 4 | Passo del guardiano | Breve avanzamento con guardia temporanea |
| 5 | Difesa del compagno | Condivisione del 30% dei danni da entità di un alleato |
| 6 | Contrattacco del veterano | Attacco utilizzabile entro due secondi da una parata riuscita |
| 7 | Riscossa | Rimuove paura/rallentamento Castigo e aumenta la resistenza alle spinte; non cura |
| 8 | Baluardo | Riduzione frontale del 60%, movimento fortemente ridotto |

### Guerriero a due mani — Vigore

| Slot | Abilità | Effetto |
|---:|---|---|
| 1 | Fendente pesante | Attacco preparato; espone brevemente a danni maggiori |
| 2 | Affondo lungo | Attacco con portata maggiore, limitato dagli ostacoli |
| 3 | Carica travolgente | Avanzamento fino al primo bersaglio vicino; espone il guerriero |
| 4 | Colpo di arresto | Interrompe la tecnica del nemico e lo rallenta |
| 5 | Fendente circolare | Danno ai nemici vicini visibili |
| 6 | Falciata bassa | Danno e rallentamento ad area; espone il guerriero |
| 7 | Spezzaguardia | Riduce la difesa, rimuove la guardia Castigo e mette lo scudo vanilla in ricarica |
| 8 | Sequenza del campione | Tre colpi distanziati; si interrompe con danno o perdita del bersaglio |

### Arciere — Concentrazione

| Slot | Abilità | Effetto |
|---:|---|---|
| 1 | Tiro preciso | Freccia dopo 0,6 secondi di preparazione |
| 2 | Passo del cacciatore | Scatto in avanti |
| 3 | Occhio del cacciatore | Bonus del 25% ai tiri speciali sul bersaglio studiato finché visibile |
| 4 | Disimpegno | Scatto all'indietro, senza invulnerabilità |
| 5 | Tiro ostacolante | Freccia che rallenta dopo un danno valido |
| 6 | Tiro di copertura | Tre frecce disperse verso il terreno mirato; rallentano quando colpiscono |
| 7 | Doppio tiro | Due frecce in successione con dispersione |
| 8 | Colpo del maestro | Preparazione di 1,5 secondi; danno aggiuntivo contro nemici esposti o vulnerabili |

## Configurazione e limiti della prima base

Ogni YAML contiene costi, ricariche, sblocchi, portata, durata, raggio, potenza e coefficienti. `weapon` accetta ANY, SHIELD, TWO_HANDED, BOW; `preparation-seconds` controlla la preparazione degli esecutori delle nuove discipline. `fraction` controlla la percentuale nelle meccaniche che la usano (guardia, vincolo, vulnerabilità, studio, patto e riduzione cure). Alcuni effetti hanno regole fisse nel codice, come il numero di frecce/colpi, la dispersione e la lentezza. Usa `/classe reload` dopo una modifica valida: termina gli effetti temporanei attivi e reinvia il catalogo alla mod.

Paura e sbilanciamento sono rallentamenti/interruzioni, senza animazioni di caduta né controllo forzato della visuale. Non ci sono resurrezione, ferite anatomiche, party dedicato o promozioni RP manuali. Gli scatti richiedono terreno libero e sicuro e si fermano davanti a ostacoli: non sono teletrasporti attraverso muri. Occhio del cacciatore non vede attraverso pareti. La grafica specifica delle skill verrà aggiunta in seguito tramite la mod.

Un solo ciclo server gestisce preparazioni, effetti, aree, scatti e proiettili: massimo 4096 stati, 64 zone e 256 frecce speciali attive, più i limiti VFX esistenti. Non vengono creati task individuali per ogni nuova maledizione. Morte, cambio classe/mondo, uscita e reload puliscono gli effetti correlati.

## Verifiche eseguite e collaudo da fare

Build Java 25 riuscita; **51 test plugin superati, zero fallimenti e zero saltati**. Dieci prove usano MockBukkit: caricamento delle discipline, soglie e ID, cure immediate/preparate, interruzione per movimento, cura periodica, equipaggiamento, munizioni insufficienti e scadenza/pulizia della guardia. La pianificazione del core è sostituita nel test; le utility ParticleStyle e atomicWrite provengono dal JAR reale beta.9. Non equivale a un avvio completo di CastigoCore su Purpur.

Resta il collaudo con client e server reali di tutte le 40 skill, soprattutto fisica delle frecce/scatti, scudi, danno condiviso, annullamenti da altri plugin, regioni WorldGuard, anticheat e bilanciamento. La simulazione non certifica questi aspetti. Per ciascuna classe prova gli otto slot contro mob e poi, con PvP abilitato, con un secondo giocatore; verifica anche ostacoli, morte durante preparazione, cambio mondo, logout, risorse finite e riavvio. Con due alleati controlla cure e vincoli; dopo la scadenza verifica che nessun rallentamento rimanga.
