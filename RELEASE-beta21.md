# CastigoClasses beta.21

Aggiornare entrambi i JAR e riavviare server e client. Purpur 26.2 / CastigoCore beta.28; Fabric 26.2 con Fabric API.

## Animazioni senza classe
Il popolano riceve contesto arma e animazioni come gli altri giocatori. La riproduzione dipende dalla connessione alla HUD, non dall'esistenza di una classe. Nessuna skill o statistica di classe viene assegnata. Spade e asce normali usano i rispettivi profili; un oggetto configurato TWO_HANDED tramite /classe pose può usare quel profilo anche senza classe, con mano secondaria vuota. Le restrizioni delle pool restano applicate ai giocatori che hanno una classe.

## Prima persona a due mani
Sostituito il modello completo del corpo in prima persona per i profili a due mani con il renderer nativo dell'arma e movimenti ridotti e limitati. Non viene più disegnato il braccio di supporto artificiale. Questo significa che la presa completa a due mani resta visibile in terza persona, mentre la prima persona privilegia visibilità e stabilità con una sola arma. Le animazioni complete e le clip restano attive in terza persona. Non è una riproduzione completa della presa Better Combat in prima persona. Le calibrazioni del vecchio corpo non vengono applicate alla nuova visuale a due mani.

## Comandi con nome RP
Il plugin usa PlayerNames e IdentityService del core per individuare i giocatori e risolvere l'UUID. Il nome RP non diventa una chiave di salvataggio: progressione e profili restano associati all'UUID. Funziona per set, rimuovi, xp e livello, oltre al reset XP giornaliero nella GUI. Completamento automatico basato sui nomi RP. I destinatari devono essere online. In caso di nome ambiguo usare UUID. I vecchi nick restano un ripiego per gli script esistenti.

Esempi con nome RP composto:
```
/classe set Aldo della Torre arciere
/classe livello Aldo della Torre 5
/classe xp Aldo della Torre 100
/classe rimuovi Aldo della Torre
```
Si possono anche racchiudere i nomi tra virgolette. UUID accettato nello stesso posto del nome.

## Verifica
Build e suite automatiche superate, compresi servizio identità RP, nomi con spazi, UUID e contesto animazione senza classe. La resa nel client Minecraft con il resource pack del server non è stata verificata visivamente.
