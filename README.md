# Couchbase Android Sync Gateway Demo

Clone into Android Studio.
Create a file named ```app/src/main/assets/config.properties``` with the following contents:

```
sgwhost=1.2.3.4
database=employees
username=demouser
password=password
```

| Property   | Description                                                     |
|------------|-----------------------------------------------------------------|
| sgwhost    | Hostname or IP address for Sync Gateway                         |
| database   | Database name configured in Sync Gateway                        |
| username   | Sync Gateway configured user with access to employee documents  |
| password   | Password for Sync Gateway user                                  |

## Backend Container

One option for a quickstart to run the Sync Gateway and Couchbase Server backend components is the [demo container](https://github.com/mminichino/employee-demo-container).

If you run the container on the same system running Android Studio (such as your desktop) use the system's IP address, not localhost (127.0.0.1).
