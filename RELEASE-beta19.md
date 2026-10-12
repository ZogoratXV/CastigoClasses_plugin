# CastigoClasses beta.19

Aggiornare sia plugin sia mod. Server Purpur 26.2, CastigoCore beta.28; client Fabric 26.2 con Fabric API. Riavviare server e client.

## Arma a due mani
La posa a riposo usa il medesimo motore e lo stesso scheletro delle animazioni di attacco. Rimossa la sovrapposizione del braccio di supporto procedurale mentre questo motore è attivo. Corretta la conversione degli spostamenti in pixel del modello: le calibrazioni ora hanno effetto nella scala prevista. La posa in prima persona viene spostata verso il basso e di lato. La perdita di durabilità non interrompe la presa. Le calibrazioni personalizzate precedenti potrebbero richiedere un aggiustamento tramite /classe pose.

## Rimuovere la classe
`/classe rimuovi <giocatore>` richiede castigo.classes.admin e un giocatore online. Rimuove classe attiva, bonus, skill e gruppo LuckPerms gestito dal sistema; gli altri gruppi restano invariati. La rimozione persiste alla riconnessione. Il profilo e il conteggio XP giornaliero restano salvati. Per assegnare nuovamente una classe: `/classe set <giocatore> <classe>`.

## Liste delle armi
Aprire `/classe admin`, scegliere la classe e il pulsante Armi consentite per classe; oppure `/classe armi <classe>`.

- Cliccare un oggetto del proprio inventario per aggiungerlo: non viene spostato né consumato. Sono riconosciuti ID vanilla e ID ItemsAdder.
- Clic destro su un oggetto nella lista: rimuove quella voce.
- Il pulsante della mano alterna lista principale e secondaria (scudi).
- Ripristina ereditarietà rimuove la lista locale: si usa quella della classe genitore, se presente; altrimenti nessun limite aggiuntivo.
- Lista vuota vieta tutti gli oggetti per quella mano. Fino a 180 voci per lista, con pagine.

Le sottoclassi ereditano le liste. Le restrizioni si applicano agli attacchi e alle skill; una lista principale configurata impedisce anche attacchi a mani nude. Le liste sostituiscono il requisito oggetto predefinito delle skill, ma un requisito specifico impostato per una skill continua a valere insieme alla lista. Le skill da arco richiedono comunque un oggetto basato su BOW. Nella linea guerriero a due mani, le armi della lista principale attivano la presa a due mani quando la mano secondaria è vuota. Le liste sono salvate in skill-equipment.yml, sezione weapon-pools.

## Verifica
Build e test automatici di plugin e mod superati. La resa della presa in prima persona richiede ancora verifica nel client Minecraft, specialmente con modelli ItemsAdder e calibrazioni personalizzate. Nessun test automatico certifica il risultato estetico in gioco.
