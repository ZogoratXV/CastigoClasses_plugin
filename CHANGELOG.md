# Changelog

## 0.1.0-beta.6 — 2026-10-07

- Preset Orison HEALING_BEAM, UUID del destinatario e origine ai suoi piedi, solo a cura riuscita.
- Suono originale castigoclasses:skill.orison riprodotto dalla mod beta.6.
- Negoziazione healingBeam e ripiego SPIRAL/audio vanilla per mod precedenti.
- 60 test plugin; documentazione del preset e dei limiti di collaudo.

## 0.1.0-beta.5 — 2026-10-07

- Cure e selezione dei bersagli senza vincoli di squadra; rimosso il gate combat.pvp interno, mantenute protezioni del mondo e delle regioni.
- Oggetto specifico obbligatorio per ogni skill, mano principale/secondaria, identificazione tramite API ItemsAdder opzionale.
- GUI `/classe admin` per scegliere oggetti dall'inventario senza consumarli; salvataggio atomico degli override per classe e skill.
- Comando `/classe icona` e PNG da resource pack nel catalogo inviato alla mod beta.5.
- Guida a requisiti, file PNG/OGG e limiti del renderer VFX; 57 test plugin.

## 0.1.0-beta.4 — 2026-10-07

- Cinque discipline e 40 skill configurabili, con sblocchi per coppie e risorse specifiche.
- Cure alleati, maledizioni, guardie direzionali, condivisione dei danni, cariche, combo e frecce reali.
- Preparazioni interrompibili, controlli equipaggiamento e munizioni, pulizia degli stati al cambio classe/mondo, morte e uscita.
- Coefficienti di forza, destrezza e attacco per le skill; attributi totali già comprensivi di crescita e punti assegnati.
- Comando staff livello, controlli WorldGuard PVP per le abilità e limiti ai bersagli/stati/proiettili.
- Compatibilità con mod beta.4 e core beta.9; grafica dedicata delle discipline rimandata.
- 51 test plugin senza errori o test saltati, inclusi 10 test su server simulato. Collaudo multiplayer reale ancora necessario.

## 0.1.0-beta.3 — 2026-10-07

- Compilazione e persistenza contro CastigoCore 2.1.0-beta.9; adattatore WorldGuard INTERACT per l'API rimossa.
- Preset VFX/suoni configurabili per fase, validati da ParticleStyle; esempio YAML e reload compatibile con le classi esistenti.
- Eventi grafici compatti inviati alla mod compatibile, senza rendering delle skill sul server; limiti di distanza e frequenza.
- Protocollo con capacità clientVfx e UUID mondo; nessuna decisione di combattimento nel client.
- Valutazione delle 40 abilità RP e proposta di gradi, senza implementare le nuove classi.

## 0.1.0-beta.2 — 2026-10-07

- Punti attributo configurabili, assegnazione server tramite comando/mod e salvataggio immediato.
- Migrazione dei profili e registro delle ricompense contro duplicazioni.
- Totali classe, crescita e punti usati da attacco, velocità, vita, risorsa, skill e difesa.
- Protocollo v1 esteso con campi facoltativi e statistiche di combattimento.
- Build e 29 test automatici superati; verifica in gioco da effettuare.

## 0.1.0-beta.1 — 2026-10-06

- Prima implementazione di classi, sottoclassi, attributi e progressione MMO configurabili.
- Risorse per classe, cooldown, otto slot persistenti e mago con otto abilità.
- Protocollo v1 per catalogo dinamico, stato del personaggio e comandi client.
- Integrazione CastigoCore e gruppo principale LuckPerms.
- Build Purpur 26.2 / Java 25 e test automatici su logica, configurazione e persistenza.
