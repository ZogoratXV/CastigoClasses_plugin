# Beta.25 - persistenza della salute (base beta.23)

Questa versione riparte dal comportamento della beta.23 e NON include le modifiche Orison della beta.24.

Il profilo UUID salva ora health (schema 5). La salute viene acquisita ai salvataggi periodici, all'uscita e allo spegnimento, prima di rimuovere i modificatori alla vita massima. All'accesso viene ripristinata dopo gli attributi di classe e limitata al massimo corrente. Vale anche per i profili senza classe. La morte non viene convertita in una resurrezione; il respawn mantiene il comportamento precedente.

I vecchi profili senza health mantengono la salute fornita da Minecraft al primo ingresso: la versione non può recuperare punti vita che non erano mai stati salvati. Da quel momento salva la salute effettiva. Per verificare: curarsi, annotare la vita, uscire e rientrare. Non viene assegnata una cura gratuita ad ogni login.

Aggiornare solo il plugin e riavviare; mod beta.21 compatibile. Test automatici superati: conservazione di 35.5 HP oltre il limite vanilla, rientro senza cura completa, migrazione vecchi profili e persistenza da popolano. Il comportamento di altri plugin che modificano la salute dopo il login richiede verifica sul server.
