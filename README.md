# FAWESchematicHost
This is a replacement host for FAWE's schematic //download support, since empcraft went down.

# How to use
Download the latest release, run with:

```java -jar FAWESchematicHost.jar . 80```

Where "." represents the folder where schematics should be stored, and 80 is the port the server listens on.
Another example:

```java -jar FAWESchematicHost.jar "D:\This Path Contains Spaces So We Must Quote it" 80```

# How to link FAWE 

Open up /plugins/FastAsyncWorldEdit/config.yml with a text editor. 

Under the "web" category, look for the "url" setting. It should be below the "shorten-urls: false" and above "assets: "https://empcraft.com/assetpack/"". 

Set the url to the IP and port of your FAWESH instance, for example:

```url: "http://127.0.0.1:25532/"```

If your FAWESchematicHost is running on the same physical machine as your Minecraft server, and you started FAWESH with:

```java -jar FAWESchematicHost.jar schematics 25532```

# NOTE
This a a one-day proof of concept. Tested with FAWE 1.13.2 build 170, FastAsyncWorldEdit-bukkit-19.11.13-5505943-1282-22.3.5
