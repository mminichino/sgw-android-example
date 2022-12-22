# Couchbase Android Sync Gateway Demo 2.0

Clone into Android Studio.
Create a file named ```app/src/main/assets/config.properties``` with the following contents:

```
sgwhost=1.2.3.4
database=employees
username=demouser
password=password
authEndpoint=1.2.3.4
```

| Property     | Description                                                    |
|--------------|----------------------------------------------------------------|
| sgwhost      | Hostname or IP address for Sync Gateway                        |
| database     | Database name configured in Sync Gateway                       |
| username     | Sync Gateway configured user with access to employee documents |
| password     | Password for Sync Gateway user                                 |
| authEndpoint | Hostname or IP address for Auth Microservice                   |

## Backend Container

To run the Sync Gateway, Couchbase Server, and Auth Microservice backend components use the [empdemo](https://hub.docker.com/r/mminichino/empdemo) container.
You can obtain a [helper utility](https://github.com/mminichino/employee-demo-container/releases/download/2.1.2/rundemo.sh) to run and manage the container.
Run the following to download the utility:
```
curl -L -O https://github.com/mminichino/employee-demo-container/releases/download/2.1.2/rundemo.sh
```
To build the container yourself, you can access the source here: [employee-demo-container](https://github.com/mminichino/employee-demo-container).

If you run the container on the same system running Android Studio (such as your desktop) use the system's IP address, not localhost (127.0.0.1).
