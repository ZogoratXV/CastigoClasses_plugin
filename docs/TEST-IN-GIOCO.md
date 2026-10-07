# Collaudo multiplayer della beta

Compilazione e test automatici sono stati eseguiti. Queste prove richiedono ancora un avvio Minecraft e il server reale o una sua copia di prova.

1. Avvia Purpur 26.2 con il core 2.1.0-beta.5 e il plugin; verifica l'assenza di errori di abilitazione.
2. Collegati con Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2 e la mod.
3. Controlla testa della skin, nome, Mago, vita, Mana e gruppo primario LuckPerms. Prova senza LuckPerms.
4. Premi R e 1–8: il numero dello slot impugnato non deve cambiare. Premi di nuovo R: torna la hotbar normale. Rimappa R e K nelle opzioni, prova chat, inventario, F1 e spettatore.
5. Con K trascina slot 1 su slot 8 e salva; lancia le abilità, riconnettiti e verifica ordine, XP e cooldown.
6. Mira a un mob per Dardo/Folgore; prova area, gelo, cura con vita ridotta, scudo e teletrasporto vicino a pareti/liquidi. Mira a un blocco per Meteora.
7. Verifica costo, risorsa insufficiente, rigenerazione e tentativi ripetuti durante il cooldown. Un'abilità con bersaglio mancante non deve consumare risorsa.
8. Con un secondo giocatore verifica VFX/suoni e PvP disabilitato. Prova le regioni protette e gli altri plugin del server che cancellano danno o teletrasporto.
9. Assegna XP con `/classe xp Nome 500`, raccogli sfere, muori e rientra: progressione e crescita degli attributi devono rimanere coerenti.
10. Attiva l'esempio Piromante, ricarica, raggiungi livello 10 e usa `/classe sottoclasse piromante`. Controlla ereditarietà e nuovo nome nel client.
11. Crea una classe con risorsa Vigore/Fede; assegnala con `/classe set`. Verifica etichetta, colore, valori iniziali e crescita.
12. Introduci un errore nel catalogo e usa reload: il catalogo precedente deve rimanere attivo. Rimuovi l'errore e riprova.
13. Riavvia il server e verifica la persistenza. Collegati infine a un server senza CastigoClasses: la mod deve lasciare la hotbar vanilla utilizzabile.

Se qualcosa non funziona, conserva `logs/latest.log` del server e del client e indica l'abilità/azione effettuata.


## Punti attributo e statistiche effettive

1. Con profilo livello 1, assegnare XP con `/classe xp` fino al livello 2: deve apparire un punto in K → Attributi. Al livello 3 nessuno nuovo, al 4 un altro. Un salto di più livelli assegna tutte le ricompense attraversate.
2. Premere + su Forza: saldo -1, conteggio spesi +1, danno d'attacco +0,5 con configurazione predefinita e senza altri modificatori. Attacco aggiunge 1. Verificare il danno sullo stesso bersaglio senza armatura, con colpi completamente caricati e senza critici.
3. Provare Destrezza (velocità), Vita (massimo senza cura), risorsa (capacità senza ricarica), Intelligenza (skill) e Difesa (danno ricevuto da entità). Confrontare anche prima/dopo un aumento di livello, mantenendo equipaggiamento e condizioni uguali.
4. Riconnettersi e riavviare: saldo, assegnazioni e totali devono rimanere identici. Cambiare classe/sottoclasse e controllare che cambino solo i contributi della classe, conservando i punti spesi.
5. Modificare frequenza/quantità e fare reload: nessuna ricompensa duplicata per livelli già elaborati. Verificare la prossima soglia con le nuove regole. Il bonus per punto aggiornato deve ricalcolare anche le assegnazioni esistenti.
6. Verificare la migrazione di un vecchio profilo: punti arretrati concessi una volta. Nessun saldo negativo con click ripetuti, zero punti o attributi disabilitati/al limite. Una mod precedente deve continuare a usare HUD e skill; il nuovo client su vecchio plugin deve nascondere i +.
7. Controllare che K → Disposizione skill conservi trascinamento, scambio e salvataggio; HUD medievale, fame e barre vanilla devono mantenere il comportamento precedente.

Queste verifiche in gioco restano da eseguire sul server; build e test automatici non sostituiscono la prova multiplayer.
