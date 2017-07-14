[![ATown Data](/img/towncrier-banner.png)](https://www.atowndata.com)

Towncrier is a simple, configurable alerter for Elasticsearch. It periodically polls Elasticsearch to evaluate alert conditions and sends out email notifications if any are met.

## Supported Platforms

Towncrier supports **Debian 8+** and **Ubuntu 16.04+**. CentOS and RedHat 7+ support is coming soon.

## Prerequisites

* **Elasticsearch v5.0+** - the instance (or instances) of Elasticsearch to alert on
* **Java v1.8+**
* **SMTP account** -- needed to send out email notifications

## Installation

### Ensure the right version of Java is installed:

```
java -version
```

If you receive a `java command not found` response, or the version is older than 1.8, consult [this walkthrough on how to install and use a recent version of Java](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04).

### Download and Verify the Debian Package

Download the package and its MD5 file

```Shell
wget https://s3.amazonaws.com/towncrier/towncrier_0.0.1_all.deb
wget https://s3.amazonaws.com/towncrier/towncrier_0.0.1_all.deb.md5
```

Verify the package with the MD5 file

````
md5sum -c towncrier_0.0.1_all.deb.md5
````

The above command should output the name of the deb package file along with "OK". If it does not, delete and re-download the files.

Next, use `dpkg` to install the Debian package

```
sudo dpkg -i towncrier_0.0.1_all.deb
```

When the above command finishes, Towncrier will be installed and running on your system. Check its status:

```
sudo service towncrier status
```

### Configuration

Let's configure Towncrier to point at our instance(s) of Elasticsearch and alert us when we want. Open `/etc/towncrier/towncrier.yml`. It will contain the following:

```YAML
elasticsearch:
  hosts:
    - host: 127.0.0.1
      port: 9200
      protocol: http
thresholds:
  - indexPattern: "trawler-*"
    name: "example1 service stopped"
    threshold: 1
    window: 60
    poll: 10
    limit: upper
    query:
      search: "stop"
      service: "example1"
outputs:
  - type: console
  - type: email
    smtp:
      host: "smtp.example.com"
      port: 465
      user: example@bigcorp.com
      pass: XXXXXXXXXXXXXXXX
      ssl: true
    from: example@example.com
    to:
      - mary@example.com
      - john@example.com
```

#### Update Elasticsearch

Update the Elasticsearch configuration with the correct host address (under `elasticsearch.hosts[n].host`), port (`elasticsearch.hosts[n].port`) and protocol (`elasticsearch.hosts[n].protocol`). You can monitor multiple instances -- for example:

```YAML
elasticsearch:
  hosts:
    - host: 192.168.10.20
      port: 9200
      protocol: http
    - host: 192.168.10.21
      port: 9200
      protocol: http
```

#### Add an Alert Condition

For each alert condition, we need to specify
  * **indexPattern** - specify the Elasticsearch index with the data to alert on (ex. `logstash-*`)
  * **name** - the name of the alert
  * **query.search** - the text to search the Elasticsearch index for
  * **threshold** - how many times the `query.search` should be found before an alert is triggered
  * **window** - the number of seconds over which the threshold applies (ex. if set to `60`, with a threshold of `3`, an alert will be triggered if the `query.search` is found in incoming data 3 times in a minute)
  * **poll** - how often, in seconds, Towncrier should check Elasticsearch

So, if we want to get an alert if the search query "error" occurs more than 2 times in a minute:

```YAML
thresholds:
  - indexPattern: "example-*"
    name: "Errors occurred"
    threshold: 2
    window: 60
    poll: 10
    limit: upper
    query:
      search: "stop"
```

#### Setup Email Notifications

To get email notifications, update the settings under the `outputs -> type:email`:
* **smtp** - add your SMTP server's host address and port, along with your authentication information.
* **from** - the email address that should appear in the "from" field of the email notification
* **to** - the list of email addresses 

### Reload `trawler-connector`

Finally, let's reload `trawler-connector` to apply our new configuration.

```Shell
sudo service trawler-connector reload
```

### Troubleshooting

If you encounter issues with getting Towncrier running, please check `/var/log/towncrier/towncrier.log`. All errors and warning will be outputted there.

---

## Contacting Us / Contributions

Please use the [Github Issues](https://github.com/atowndata/towncrier/issues) page for questions, ideas and bug reports. Pull requests are welcome.

Trawler was built by the consulting team at [ATown Data](https://www.atowndata.com). Please [contact us](https://atowndata.com/contact/) if you have a project you'd like to talk to us about!


## License

Distributed under the [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
Copyright &copy; 2017 [ATown Data](https://www.atowndata.com)