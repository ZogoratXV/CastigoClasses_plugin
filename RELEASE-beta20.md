# CastigoClasses beta.20

Aggiornare plugin e mod insieme e riavviare server e client. Compatibilità: Purpur 26.2, CastigoCore beta.28; Fabric Minecraft 26.2 con Fabric API.

## Pool armi e attacchi normali
Le liste della beta.20 bloccano già anche il danno degli attacchi normali. Verificato con test specifico: mago bianco con lista contenente soltanto minecraft:stick non può attaccare con spade di ferro, diamante o netherite. Il bastone resta utilizzabile. Nessuna lista configurata significa nessuna restrizione aggiuntiva.
Aprire /classe armi <classe>, aggiungere dal proprio inventario gli oggetti consentiti; clic destro sulla lista per rimuoverli. Oggetti vanilla e ItemsAdder, ereditarietà per le sottoclassi. Le eventuali liste locali delle sottoclassi prevalgono su quelle ereditate. Il blocco impedisce il danno; non impedisce di tenere l'oggetto nell'inventario o in mano.

## Rendering a due mani
Posa a riposo sospesa mentre viene riprodotta un'animazione d'attacco: i due livelli non sommano più gli spostamenti. Corretto il segno degli spostamenti del corpo nella libreria di animazione e rimossa la seconda rotazione verticale della camera. Posizionamento costante anche nelle fasi iniziali/finali delle clip. Calibrazioni in prima persona limitate per impedire spostamenti eccessivi. Il rendering vanilla viene escluso anche nel primo fotogramma della posa animata, senza attendere il successivo aggiornamento della transizione. Le trasformazioni temporanee della mano vengono sempre ripristinate, anche se un'altra mod interrompe il rendering.

## HUD senza classe
Dopo /classe rimuovi <giocatore>, la HUD rimane visibile anche alla riconnessione: testa, nome, livello conservato nel profilo, vita attuale e gruppo LuckPerms. La cornice segue le stesse priorità dei gruppi e, in assenza di configurazioni, usa quella del popolano. Nessuna risorsa o skill di classe viene attivata. La barra XP resta vuota: il livello visualizzato è quello conservato, non una nuova progressione del popolano. Assegnare di nuovo una classe ripristina le sue funzioni.

## Verifica
Build e test automatici superati. Sono verificati blocco danni, stato HUD senza classe e riattivazione della classe, limiti e segni del posizionamento. Non è stato eseguito un test visivo nel client Minecraft: la resa con il modello e il resource pack del server rimane da verificare in gioco.
