# any2any-bot
Бот для конвертации

# Horizontal scalability
```shell script
mkdir -p media/downloads
mkdir -p media/uploads
mkdir -p media/temp
```

#Rsync daemon
/etc/rsyncd.scrt:
```text
user:password
```

/etc/rsyncd.conf:
```text
max connections = 10
log file = /var/log/rsyncd.log

[downloads]
    path = media/downloads/
    comment = Public downloads
    auth users = user
    readonly = true
    secrets file = /etc/rsyncd.scrt
    hosts allow = *

[uploads]
    path = media/uploads/
    comment = Public uploads
    uid = user
    gid = user
    readonly = false
    auth users = user
    secrets file = /etc/rsyncd.scrt
    hosts allow = *
```

```text
Allow tcp/873 
ufw allow and server hosting firewall
```

#Rsync downloads client
```shell script
mkdir -p cron.d
```

cron.d/rsyncd-client.scrt:
```text
pswd
```

cron.d/rsync_downloads.sh:
```shell script
#!/bin/bash

if [ -e cron.d/rsync_downloads.lock ]
then
  echo "Rsync downloads job already running...exiting"
  exit
fi

touch cron.d/rsync_downloads.lock

PATH=/etc:/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin

password_file='cron.d/rsyncd-client.scrt'
user='user'
ip='host'
source='downloads'
destination='media/downloads'

rsync -azP --delete --password-file=$password_file rsync://$user@$ip/$source $destination

rm cron.d/rsync_downloads.lock
```

crontab -e:
```text
* * * * * sh cron.d/rsync_downloads.sh >> cron.d/rsync-downloads-client.log 2>&1
```

#Rsync uploads client
cron.d/rsyncd.scrt:
```text
pswd
```

cron.d/rsync_uploads.sh:
```shell script
#!/bin/bash

if [ -e cron.d/rsync_uploads.lock ]
then
  echo "Rsync uploads job already running...exiting"
  exit
fi

touch cron.d/rsync_uploads.lock

PATH=/etc:/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin

password_file='cron.d/rsyncd-client.scrt'
user='user'
ip='host'
source='media/uploads/'
destination='uploads'

rsync -azP --remove-source-files --password-file=$password_file $source rsync://$user@$ip/$destination

rm cron.d/rsync_uploads.lock
```

crontab -e:
```text
* * * * * sh cron.d/rsync_uploads.sh >> cron.d/rsync-uploads-client.log 2>&1
```

```shell script
sudo chmod 600 cron.d/rsyncd-client.scrt
sudo systemctl restart cron
sudo systemctl restart rsync
```