# Beta.23 - HUD con peso LuckPerms e bersagli custom

Aggiornare solo il plugin, riavviare il server. Mod beta.21 compatibile.

La cornice viene scelta fra i gruppi effettivamente ereditati nel contesto LP corrente con tema configurato o preset riconosciuto, ordinati per peso LuckPerms decrescente. Peso assente = 0. A parità: gruppo primario, poi nome alfabetico. Le vecchie priorità del file HUD non decidono più la selezione; il relativo campo è rimosso dalla GUI. Configurare il PNG del gruppo con /classe hud e il peso tramite LuckPerms. I gruppi senza tema/preset non oscurano quelli con una cornice. Senza corrispondenze si usa la classe base; senza classe si usa popolano. Questa regola vale anche senza classe: un amministratore con tema e peso maggiore mantiene la cornice amministratore.

Tolto il rifiuto indiscriminato delle entità con metadata NPC. Gli NPC Citizens di tipo Player vengono trattati come PvE, senza applicare i controlli PvP riservati ai giocatori reali. Restano esclusi bersagli invulnerabili, morti, armor stand e animali addomesticati. WorldGuard e gli eventi di danno degli altri plugin restano rispettati.

MythicMobs: le entità viventi custom usano il normale danno Bukkit con il giocatore come sorgente, preservando gli eventi del plugin. Non si forza la salute e non si aggirano immunità o cancellazioni. Per mob con modelli separati si deve colpire l'entità vivente effettiva: non è aggiunto un adattatore per hitbox decorative/armor stand.

Citizens: selezionare l'NPC e usare /npc vulnerable se è protetto (il comando alterna lo stato). MythicMobs: verificare che Options.Invincible non sia true. Riferimenti ufficiali: https://wiki.citizensnpcs.co/Commands e https://wiki.mythiccraft.io/mythicmobs/Mobs/Options

Build e test automatici superati, inclusi priorità per peso, pareggio, HUD senza classe, NPC Player in mondo senza PvP e bersagli custom invulnerabili. Citizens e MythicMobs reali non sono stati avviati: i test usano entità simulate, quindi è necessaria una verifica sul server con le versioni installate.
