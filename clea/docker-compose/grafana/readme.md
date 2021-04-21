
# Grafana overrides for clea 

This stack extends the default Clea stack by adding a grafana container configured with:
* a datasource to Postgres cleaDB
* a dashboard for location statistiques.

## launch the stack

Full stack

```bash
$ clea -o grafana up
```

Minimal stack

```bash
$ clea -o grafana up postgres grafana
```


## Accessing grafana 

Navigating with a browser to *http://localhost:3000* will open the grafana login page.

The first time the stack is started, the credentials to used are:
* username: admin
* password: admin

Then you are asked to change the default one.

## provisioned objects

The left panel display functionalities : "Search", "Create", "Dashboards", "Explore", "Alerting", "Configuration" and "Server Admin".

Clicking in the "Data-sources" sub-menu of the "Configuration" menu, you should see a "cleadb" datasource.

Clicking in the "Manage" sub-menu of the "Dashboards" menu, you should see a Folder "General" with a dashboard "Statistiques par type de lieu".
You should be able to click on it to open the dashboard and explore datas.


## Mocking Data:

Until the *"stat_location"* table is filled by the component "clea-venue-consumer", the next SQL instruction can be used to fill the table with mock datas from *Exposed_visits* table.

(Nota: 2208988800 is the number of seconds between NTP epoch (1/1/1900) and Unix epoch (1/1/1970))

```bash
TRUNCATE TABLE stat_location;

INSERT INTO stat_location 
	SELECT to_timestamp(period_start-2208988800) AS periode, venue_type, venue_category1, venue_category2, sum(backward_visits) as backward_visits, sum(forward_visits) as forward_visits 
		FROM exposed_visits 
		WHERE timeslot=0 AND (backward_visits>0 OR forward_visits>0) 
		GROUP BY periode,venue_type, venue_category1, venue_category2 
		ORDER BY periode 
```

