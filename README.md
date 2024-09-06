# Couchbase Android Sync Gateway Demo 4.1

Clone into Android Studio.

The password for all users is "password". To reset and start from scratch, use the "Clear App Data" button in the Settings activity (Note: the app will exit - this is expected).
![Usage](doc/usage-1.jpg)
![Usage](doc/usage-2.jpg)
![Architecture](doc/architecture-1.jpg)
![Access](doc/access-1.jpg)
![Access](doc/access-2.jpg)
![Access](doc/access-3.jpg)
![Access](doc/access-4.jpg)
![Access](doc/access-5.jpg)
![Login](doc/login-1.jpg)

## Backend Container

To run the Sync Gateway, Couchbase Server, and Auth Microservice backend components for the demo use the [mobiledemo](https://hub.docker.com/r/mminichino/mobiledemo) container version 2.0.4.
You can obtain a [helper utility](https://github.com/mminichino/mobile-demo-container/releases/download/2.0.4/rundemo.sh) to run and manage the container.
Run the following to download the utility:
```
curl -L -O https://github.com/mminichino/mobile-demo-container/releases/download/2.0.2/rundemo.sh
```
```
chmod +x rundemo.sh
```
To build the container yourself, you can access the source here: [mobile-demo-container](https://github.com/mminichino/mobile-demo-container).

### Notes
If you run the container on the same system running Android Studio (such as your desktop) use the system's IP address, not localhost (127.0.0.1).

To look at the documents in the database and their assigned channels, get a shell into the container and use the SGW CLI to dump the database:

```
% ./rundemo.sh --shell
root@ccb72cf9d6ee:/demo/couchbase# cd sgwcli/
root@ccb72cf9d6ee:/demo/couchbase/sgwcli# ./sgwcli database dump -h 127.0.0.1 -n timecard
Keyspace timecard.data.employees:
Key: employees::1 Id: employees::1 Channels: ['channel.location_id@1']
Key: employees::10 Id: employees::10 Channels: ['channel.location_id@1']
Key: employees::11 Id: employees::11 Channels: ['channel.location_id@2']
Key: employees::12 Id: employees::12 Channels: ['channel.location_id@2']
Key: employees::13 Id: employees::13 Channels: ['channel.location_id@2']
Key: employees::14 Id: employees::14 Channels: ['channel.location_id@2']
Key: employees::15 Id: employees::15 Channels: ['channel.location_id@2']
Key: employees::16 Id: employees::16 Channels: ['channel.location_id@2']
Key: employees::17 Id: employees::17 Channels: ['channel.location_id@2']
Key: employees::18 Id: employees::18 Channels: ['channel.location_id@2']
Key: employees::19 Id: employees::19 Channels: ['channel.location_id@2']
Key: employees::2 Id: employees::2 Channels: ['channel.location_id@1']
Key: employees::20 Id: employees::20 Channels: ['channel.location_id@2']
Key: employees::21 Id: employees::21 Channels: ['channel.location_id@3']
Key: employees::22 Id: employees::22 Channels: ['channel.location_id@3']
Key: employees::23 Id: employees::23 Channels: ['channel.location_id@3']
Key: employees::24 Id: employees::24 Channels: ['channel.location_id@3']
Key: employees::25 Id: employees::25 Channels: ['channel.location_id@3']
Key: employees::26 Id: employees::26 Channels: ['channel.location_id@3']
Key: employees::27 Id: employees::27 Channels: ['channel.location_id@3']
Key: employees::28 Id: employees::28 Channels: ['channel.location_id@3']
Key: employees::29 Id: employees::29 Channels: ['channel.location_id@3']
Key: employees::3 Id: employees::3 Channels: ['channel.location_id@1']
Key: employees::30 Id: employees::30 Channels: ['channel.location_id@3']
Key: employees::31 Id: employees::31 Channels: ['channel.location_id@4']
Key: employees::32 Id: employees::32 Channels: ['channel.location_id@4']
Key: employees::33 Id: employees::33 Channels: ['channel.location_id@4']
Key: employees::34 Id: employees::34 Channels: ['channel.location_id@4']
Key: employees::35 Id: employees::35 Channels: ['channel.location_id@4']
Key: employees::36 Id: employees::36 Channels: ['channel.location_id@4']
Key: employees::37 Id: employees::37 Channels: ['channel.location_id@4']
Key: employees::38 Id: employees::38 Channels: ['channel.location_id@4']
Key: employees::39 Id: employees::39 Channels: ['channel.location_id@4']
Key: employees::4 Id: employees::4 Channels: ['channel.location_id@1']
Key: employees::40 Id: employees::40 Channels: ['channel.location_id@4']
Key: employees::5 Id: employees::5 Channels: ['channel.location_id@1']
Key: employees::6 Id: employees::6 Channels: ['channel.location_id@1']
Key: employees::7 Id: employees::7 Channels: ['channel.location_id@1']
Key: employees::8 Id: employees::8 Channels: ['channel.location_id@1']
Key: employees::9 Id: employees::9 Channels: ['channel.location_id@1']
Keyspace timecard.data.locations:
Key: locations::1 Id: locations::1 Channels: ['channel.location_id@1']
Key: locations::2 Id: locations::2 Channels: ['channel.location_id@2']
Key: locations::3 Id: locations::3 Channels: ['channel.location_id@3']
Key: locations::4 Id: locations::4 Channels: ['channel.location_id@4']
Keyspace timecard.data.timecards:
Key: timecards::5::1::2023-05-22T17:21:40.273 Id: timecards::5::1::2023-05-22T17:21:40.273 Channels: ['channel.location_id@1']
```

See [Sync Gateway CLI](https://github.com/mminichino/sgwcli) for details on using this utility.

The config.properties file contains default values that are imported when the app is first run.
| Property      | Description                                                    |
|---------------|----------------------------------------------------------------|
| sgwhost       | Hostname or IP address for Sync Gateway                        |
| database      | Database name configured in Sync Gateway                       |
| authEndpoint  | Hostname or IP address for Auth Microservice                   |
| demoList      | List of demos (timecard,insurance)                             |
| tagList       | List of group tags (location_id,region)                        |
| activeDemo    | Default demo (timecard)                                        |
| groupTagField | Default group ID field (location_id)                           |
