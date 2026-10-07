# Cinque discipline: fattibilità e proposta

Documento storico di proposta, 7 ottobre 2026. Le cinque discipline sono ora implementate nella base plugin beta.4: vedere DISCIPLINE.md per comportamento effettivo, progressione automatica e limiti. Le proposte seguenti possono differire dall'implementazione.

## Organizzazione consigliata

Le cinque discipline sono realizzabili su Purpur 26.2 con la mod Fabric. Minecraft offre salute, danni, proiettili, effetti temporanei e movimento; ferite anatomiche, dolore, paura e impegno spirituale devono essere tradotti in regole esplicite oppure lasciati al gioco di ruolo. La fattibilità non implica che basti rinominare le otto skill attuali: servono nuovi esecutori server, stati temporanei e regole per alleati, armi e preparazione.

Userei **cinque classi giocabili**, con otto slot ciascuna e cinque gradi interni. Il grado non dovrebbe essere una sottoclasse: passare da Apprendista a Maestro non cambia identità, risorsa o punti assegnati. Le sottoclassi rimangono disponibili per vere specializzazioni future. I due guerrieri possono condividere codice e configurazioni comuni, senza imporre che uno sia l'evoluzione dell'altro.

| Disciplina | Risorsa unica iniziale | Equipaggiamento e ruolo |
|---|---|---|
| Mago Bianco | Fede | Supporto, cure e protezione; focus facoltativo |
| Mago Nero | Energia oscura | Maledizioni e controllo; Patto del sangue recupera vita |
| Guerriero con scudo | Vigore | Arma a una mano e scudo nell'altra |
| Guerriero a due mani | Vigore | Arma contrassegnata come pesante e mano secondaria vuota |
| Arciere | Concentrazione | Arco e munizioni; preparazione e posizione |

Userei inizialmente una sola risorsa per disciplina. La vitalità del Mago Nero resta la salute; un eventuale costo in vita deve essere distinto dal costo in energia, visibile e limitato per non uccidere accidentalmente il personaggio. Disciplina e lucidità sono condizioni di esecuzione, non ulteriori barre obbligatorie. Le regole di rigenerazione possono differire per classe, ad esempio Concentrazione più rapida quando si resta fermi e fuori combattimento. Questo comportamento aggiuntivo è da sviluppare; oggi è configurabile la rigenerazione per secondo e per livello.

## Progressione proposta

Separerei livello numerico, punti attributo e grado narrativo. Per un server RP consiglio **livello minimo più promozione approvata da staff/maestro autorizzato**. Raggiungere il livello permette la promozione, ma non la concede automaticamente. Si potrà prevedere una modalità automatica per server più orientati al combattimento.

| Grado | Livello minimo proposto, modificabile | Abilità disponibili |
|---|---:|---|
| Aspirante | 1 | Nessuna; formazione iniziale |
| Apprendista | 5 | 1 e 2 |
| Praticante | 15 | Da 1 a 4 |
| Esperto | 30 | Da 1 a 6 |
| Maestro | 50 | Tutte e 8 |

Sono soglie iniziali da bilanciare, non valori già applicati. Il documento usa anche «veterano» nel testo: lo tratterei come titolo narrativo dell'Esperto, senza aggiungere un sesto grado. Nella barra si possono mantenere tutti gli otto slot riordinabili, mostrando un lucchetto sulle abilità non apprese. Le assegnazioni di punti continuano a dipendere dalla configurazione esistente, indipendentemente dalla promozione.

## Mago Bianco

| # | Abilità | Implementazione proposta e limiti |
|---|---|---|
| 1 | Orison di cura | Cura diretta su un alleato mirato entro portata e visibile; se non c'è bersaglio, autocura. Lenire dolore/stabilizzare ferite resta RP finché non esiste un sistema ferite. |
| 2 | Dardo consacrato | Proiettile o raggio server con collisione e danno magico; la mod anima il dardo luminoso. Un bonus contro non-morti è possibile ma non necessario. |
| 3 | Benedizione rigenerante | Cura periodica per durata definita; rinnova lo stesso effetto senza sommarlo illimitatamente. Può essere interrotta o dissolta secondo regole comuni. |
| 4 | Purificazione | Rimuove una lista configurata di malus vanilla e maledizioni Castigo di grado consentito. Non cancella automaticamente effetti gestiti da altri plugin senza integrazione. |
| 5 | Vincolo del custode | Trasferisce una percentuale del danno accettato a un protettore entro distanza; durata, tetto per colpo e interruzione per morte/uscita. Vietati cicli tra vincoli. |
| 6 | Luce respingente | Spinta radiale limitata e breve rallentamento. Resistenze al contraccolpo e protezioni restano valide; nessuna invulnerabilità implicita. |
| 7 | Grande intercessione | Cura maggiore con preparazione interrompibile e costo elevato. Le ferite gravi sono rappresentate da molta salute mancante o da un futuro stato ferita. Nessuna resurrezione. |
| 8 | Santuario di luce | Area temporanea che riduce i danni degli alleati presenti. Verifica periodica locale e anello visivo client; il bonus termina uscendo e non si accumula fra santuari. |

## Mago Nero

| # | Abilità | Implementazione proposta e limiti |
|---|---|---|
| 1 | Dardo maledetto | Danno diretto e breve maledizione applicata solo se il colpo è accettato; durata e dissoluzione esplicite. |
| 2 | Malocchio | Riduzione temporanea della difesa MMO. Contro mob senza statistiche Castigo, vulnerabilità percentuale dedicata anziché modificare valori inesistenti. |
| 3 | Consunzione | Danno periodico con fonte, durata e frequenza definite; protezioni e morte del bersaglio rispettate a ogni impulso. |
| 4 | Terrore sussurrato | Paura autentica non è una statistica di Minecraft. Alternativa: rallentamento moderato, disturbo sonoro e aumento del tempo di preparazione delle skill Castigo. Nessun controllo totale degli input; reazione ancora possibile. |
| 5 | Cerchio della rovina | Zona con danno periodico, raggio e durata limitati. Nessuna modifica permanente ai blocchi. Gli avversari possono uscirne. |
| 6 | Sigillo del tormento | Riduce la cura ricevuta e la rigenerazione gestita da Castigo; purificazione lo rimuove. Cure di altri plugin che modificano direttamente la salute richiedono un'integrazione specifica. |
| 7 | Patto del sangue | Recupero di vita proporzionale al danno realmente inflitto, con limite per uso. Nessun recupero da un attacco annullato, bersaglio immune o salute già esaurita. |
| 8 | Vortice delle tenebre | Attrazione con velocità limitata verso un centro, per poco tempo e senza attraversare muri. Blocchi e collisioni limitano il risultato; boss e bersagli resistenti possono essere immuni. |

## Guerriero con scudo

| # | Abilità | Implementazione proposta e limiti |
|---|---|---|
| 1 | Affondo disciplinato | Attacco corto con controllo di portata e visuale, che scala con forza/attacco. Non è un vero riconoscimento anatomico di un'apertura. |
| 2 | Urto di scudo | Richiede scudo equipaggiato; danno contenuto, spinta e interruzione di una preparazione Castigo. Non può interrompere genericamente ogni azione di altri plugin. |
| 3 | Guardia salda | Riduzione del danno solo entro un cono frontale, scudo richiesto e mobilità ridotta. Attacchi laterali e posteriori conservano efficacia. |
| 4 | Passo del guardiano | Breve avanzamento controllato con guardia attiva, collisioni e confine del mondo rispettati. Evitare un teletrasporto attraverso il nemico. |
| 5 | Difesa del compagno | Collegamento difensivo corto con quota di danno trasferita o intercettata. Il corpo di un giocatore non blocca in modo affidabile tutti gli attacchi vanilla: meglio una protezione esplicita e visibile. |
| 6 | Contrattacco del veterano | Una parata effettiva apre una finestra breve per un colpo potenziato. Il trigger si basa sul danno bloccato, non sul semplice tenere lo scudo alzato. |
| 7 | Riscossa | Rimuove i rallentamenti Castigo da sbilanciamento e dà resistenza temporanea al contraccolpo. Nessuna cura e nessuna cancellazione indiscriminata di maledizioni. |
| 8 | Baluardo | Guardia potenziata temporanea con movimento molto ridotto, costo/sostegno di Vigore e copertura frontale. Protegge un passaggio senza creare un muro invisibile invalicabile. |

## Guerriero a due mani

| # | Abilità | Implementazione proposta e limiti |
|---|---|---|
| 1 | Fendente pesante | Attacco preparato con finestra di recupero anche se manca il bersaglio. La vulnerabilità dopo un errore è una regola server, non un'animazione dell'arma. |
| 2 | Affondo lungo | Portata maggiore ma limitata tramite selezione server, senza alterare la portata globale del giocatore. Gli ostacoli fermano il colpo. |
| 3 | Carica travolgente | Spinta in avanti con controllo di percorso e primo bersaglio valido; interrompe la corsa all'urto. Nessun attraversamento di muri o caricamento di chunk. |
| 4 | Colpo di arresto | Colpo corto che rallenta e interrompe una carica Castigo. Per normali nemici in movimento si valuta la direzione relativa, con tolleranza alla latenza. |
| 5 | Fendente circolare | Attacco radiale corto, bersagli massimi e controllo di visuale. Evitare di duplicare il danno dell'attacco ad area vanilla nello stesso lancio. |
| 6 | Falciata bassa | Rallentamento/sbilanciamento temporaneo; breve vulnerabilità del guerriero. Minecraft non distingue le gambe nella salute: questo è l'equivalente pratico. |
| 7 | Spezzaguardia | Breve riduzione della difesa o disabilitazione dello scudo, con durata dichiarata. Non annulla permanentemente armatura o protezioni. |
| 8 | Sequenza del campione | Pochi attacchi temporizzati, con controllo di portata e bersaglio a ogni fase. Interrompibile; non rimuovere globalmente l'invulnerabilità fra colpi per forzare danni multipli. |

Minecraft non ha una categoria universale «arma a due mani». Propongo arma identificata tramite metadati/ID del core e mano secondaria obbligatoriamente vuota, controllate anche durante la preparazione. Un modello personalizzato può farla sembrare una spada pesante; l'animazione completa delle due braccia richiede lavoro aggiuntivo nella mod.

## Arciere

| # | Abilità | Implementazione proposta e limiti |
|---|---|---|
| 1 | Tiro preciso | Arco, munizione e breve preparazione; una freccia server con traiettoria e collisione reali. Movimento o colpi ricevuti possono interrompere la preparazione. |
| 2 | Passo del cacciatore | Breve accelerazione o scatto controllato; spostamento reale con collisioni e costo di Concentrazione. |
| 3 | Occhio del cacciatore | Informazioni o bonus su un bersaglio attualmente visibile. Indicatore nella mod solo finché la visuale è libera; evitare il glowing vanilla, che rivela sagome attraverso gli ostacoli. |
| 4 | Disimpegno | Scatto breve indietro/laterale verso uno spazio libero; non dà invulnerabilità automaticamente e non attraversa ostacoli. |
| 5 | Tiro ostacolante | Freccia reale che applica rallentamento solo dopo un impatto valido. Mancare il colpo consuma la munizione prevista. |
| 6 | Tiro di copertura | Scarica limitata di frecce verso una zona. L'alternativa più leggera è un'area di soppressione con impulsi e rallentamento: meno proiettili, ma va dichiarato come adattamento della tecnica. |
| 7 | Doppio tiro | Due proiettili ravvicinati con dispersione e consumo di due munizioni. Gestire recupero delle frecce ed eventuale Infinity senza duplicazioni. |
| 8 | Colpo del maestro | Preparazione più lunga e interrompibile, quindi proiettile ad alto impatto. «Esposto» può significare bersaglio senza guardia frontale o colpito da un malus definito; niente danno garantito. |

Frecce incendiarie/avvelenate/consacrate sono fattibili come oggetti con ID persistente e comportamento all'impatto. Ricette, economia e produzione da artigiani sono una funzionalità distinta da integrare con i rispettivi plugin.

## Architettura e prestazioni

Il plugin deve essere autorevole su bersagli, equipaggiamento, munizioni, risorse, danni e sblocchi. La mod riceve eventi e anima: modificarla non deve permettere di colpire più lontano o saltare un costo. La separazione è già applicata ai VFX di questo aggiornamento.

Per le nuove abilità propongo esecutori riutilizzabili: cura diretta/periodica, danno diretto/periodico, malus, guardia, collegamento, zona, spostamento, proiettile e preparazione. Ogni configurazione combina questi meccanismi; non servono quaranta scheduler separati né quaranta copie delle stesse formule.

Un unico gestore aggiornerebbe gli stati periodici ogni 5 tick; proiettili e scatti richiedono controlli più frequenti soltanto mentre sono attivi. Le zone vanno cercate localmente con massimo raggio/bersagli/durata, evitando scansioni di tutte le entità del mondo. Niente caricamenti di chunk per una skill. Alla morte, uscita o cambio mondo vanno rimossi preparazioni e collegamenti non persistenti.

Occorre un sistema esplicito di alleati/party: **il gruppo LuckPerms non identifica automaticamente un gruppo di combattimento**. Tutti i membri di un rango religioso o staff non devono diventare automaticamente bersagli delle cure. In assenza di party, autocura e alleato scelto consapevolmente sono una base più prevedibile.

I moltiplicatori di classe devono restare distinti: forza/attacco per tecniche fisiche, intelligenza per magie; destrezza può contribuire alle abilità dell'arciere senza trasformarsi in precisione garantita. I numeri andranno bilanciati con salute dei mob, armature, crescita per livello e punti spendibili già presenti.

Priorità proposta: Mago Bianco e Mago Nero per costruire cure/maledizioni; poi scudo e due mani per guardie/cariche; infine Arciere per preparazioni, munizioni e proiettili. Prima di una prova PvP vanno verificati trasferimento danni, cure, stacking e compatibilità con protezioni/anticheat del server. Non prometto un numero di giocatori sostenibili senza misurazioni sul tuo ambiente.

## Base della valutazione

La proposta deriva dal documento ricevuto, dal codice attuale e dalle API disponibili. Le particelle sono supportate in Paper 26.2 e Fabric consente renderer/particelle client; i suoni personalizzati richiedono asset noti al client. Le meccaniche nelle tabelle sono proposte progettuali, non funzioni già garantite dai plugin installati.

- [Particelle Paper 26.2](https://docs.papermc.io/paper/dev/particles/)
- [Particelle personalizzate Fabric 26.2](https://docs.fabricmc.net/develop/rendering/particles/creating-particles)
- [API delle entità viventi Paper 26.2](https://jd.papermc.io/paper/26.2/org/bukkit/entity/LivingEntity.html)
- [Suoni vanilla e da resource pack](https://docs.papermc.io/adventure/sound/)
- [Calcolo delle protezioni WorldGuard](https://worldguard.enginehub.org/en/latest/developer/regions/flag-calculation/)
