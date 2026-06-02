Überlegungen zum Cachen von Prozessergebnissen:
Wenn ein shell-command mehrfach benutzt wird, macht es Sinn, dessen Ausgabe für eine gewisse Zeit zu cachen. Gleichzeitig soll DoCache das Handling des Prozesses vereinfachen. Je nach Anwendungsfall werden verschiedene statische Methoden angeboten
## doCmd(List\<String\> cmd)
Einfaches Ausführen eines Commands. Die Ausgabe landet dabei auf der Konsole. Ebenso die Fehlermeldungen. Der Befehl kann synchron oder asynchron abfgearbeitet werden.
## out = getQueue(List\<String\> cmd)
Führt den Befehl aus, und gibt eine Queue zurück, die  die Ergebnisse des Commands liefert. Fehlermeldungen des Befehls werden auf der Konsole ausgegeben. Die Queue kann mit poll() abgefragt werden, und liefert die Ergebnisse dann Zeilenweise schon während der Befehl läuft. Mit waitFor() kann gewartet werden bis der Befehl abgeschlossen ist.
## erg = getFirstOr(List\<String> cmd, \<String> or)
Führt den Befehl aus, und gibt nur die erste Zeile zurück. Wenn der Befehl keine Zeile zurückgibt, kommt statt dessen der String or zurück. Der Befehl wird mindestens so lange synchron abgearbeitet, bis eine Zeile erzeugt wurde, oder der Befehl endet.
## qs = getQueues(List\<String\> cmd))
Führt den Befehl aus, und gibt zwei Queues zurück. qs.out enthält die Ausgabe des Programms und qs.err enthält die Fehlermeldungen des Programms. Die Queues können mit poll() abgefragt werden, und liefern die Ergebnisse dann zeilenweise schon während der Befehl noch läuft. Mit waitFor() kann gewartet werden bis der Befehl abgeschlossen ist.
### Defaults:
Diese Defaults werden bei jedem neu ausgeführten Befehl beachtet, wenn keine speziellen Optionen mit dem Befehl angegeben wurden.
#### defaultAsynchron.set(true)
Per default werden alle operationen wenn möglich asynchron ausgeführt
#### defaultCacheMs.set(60_000)
Per default werden alle Befehle für 1 Minute im Cache gehalten. und dann automatisch aus dem cache entfernt
#### private cache
Der cache wird in einem virtuellen Thread automatisch verwaltet. Einträge werden nach etwa der eingestellten Zeit entfernt.
### Optionen
Ohne spezielle Optionen werden die Befehle asynchron in einem virtuellen Thread ausgeführt. Das Ergebnis wird für 60 Sekunden im cache gehalten und steht in dieser Zeit bei erneuten Aufrufen unverändert zur Verfügung.
Die Abarbeitung und das caching können gesteuert werden, indem entsprechende FLAGS in der Liste der Strings mit übergeben werden. 
#### DoCAche.ASYNC
Den Befehl asynchron abarbeiten. Warte nicht auf die Ergebnisse
#### DoCAche.SYNC
Den Befehl synchron abarbeiten. Warte bis der Befehl beendet ist.
#### DoCAche.DONT_CACHE
Den Befehl wird NICHT in den Cache aufgenommen
#### DoCAche.REFRESH
Der Befehl wird auf jeden Fall neu abgearbeitet. Die Ergebnisse neuen Ergebnisse landen im Cache
#### DoCAche.CACHE_MS+"5000" ... "3600_000"
Die Ergebnise des Befehls verbleiben für 5000ms = 5 Sekunden im Cache, oder 3600_000ms = 1 Stunde;
# ProcessQueueC\<E> extends LinkedBlockingQueue<E>

##### poll()
Die poll()-Funktion ist überschrieben, damit poll() wartet, bis der Befehl gestartet ist
##### getFirst()
Liefert die erste Zeile die der Befehl erzeugt, oder null, wenn der Befehl endet, ohne Ergebnisse zu produzieren. Muss warten, bis der Befehl zumindest eine Zeile erzeugt hat.
#### waitFor()
Warte bis der Befehl abgeschlossen ist 

# ProcessQueuesC\<E>
Zusammenstellung von 2 ProzessQueues mit den Namen **out** und **err**. Bei der Abarbeitung eines Befehls werden diese queues per virtualThread gefüllt sobald der Befehl Ergebnisse liefert
##### toOut
Sendet die Ausgabe des Befehls per virtualThread auf die Konsole. Erlaubt Methodchaining.
##### toErr
Sendet die Fehlermeldungen des Befehls per virtualThread auf die Konsole Erlaubt Methodchaining.
##### waitFor 
Wartet, bis der befehl abgeschlossen istund alle Daten im Cache sind.
 Erlaubt Methodchaining.
# key
Die aufgerufene Kommandozeile als String oder List<String> ist der key zum cache
Bei der Erstellung eines cache wird seine Lebenszeit festgelegt. Nach ablauf der Lebenszeit verschwindet der Eintrag aus dem cache

## erster Aufruf:
übergeben wird der Key und optional die Lebenszeit.
Zurückgegeben wird Ein Datensatz Prozessqueues mit der Outputqueue und der Errorqueue.
Intern wird ein zusätzlicher Datensatz angelegt der **sofort** im cache landet
Der Prozess wird per Do gestartet, und in der Output in den cache geleitet (V1)
Ein Prozess für den errorstream wird gestartet
Ein prozess für den Outputstream wird gestartet



