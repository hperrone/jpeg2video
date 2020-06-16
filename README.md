# jpeg2video

## Requirements

The solution has been developed and tested using:

  **Docker environment**
  - Docker version 19.03.8
  - docker-compose version 1.25.5
  
  **Web Browsers**
  - Google Chrome v.83.0.4103.97
  - Microsoft Edge v.83.0.478.45
  - Internet Explorer v.11.900.18362.0
  - Firefox Browser v.77.0.1
  - Mobile: Android 9 - Chrome v.83.0.4103.106

## Building the project

Unzip (or clone the repository) locally. Within the project's root directory
execute:

```
   docker-compose build  
```

This will build the 3 docker images that compose the solution:
  - pipeman
  - encoder
  - web

## Running the project

At the project's root directory, execute:
```
   docker-compose up  
```

Alternatively, to launch with multiple encoder container instances, use the 
`--scale` argument. In example, the following command will launch the solution
with 3 encoder instances:

```
   docker-compose up --scale encoder=3 
```

> Note: encoders are very processor intensive. Launching more encoder containers
> than the available CPU capacity will cause system underperformance.  


Wait the container for each image to be launched.

Access the _web client application_ from your web browser at 
http://localhost:8080

At this point you should be able to see the application interface, but no image
sequences are available to be played.

## Processing and publishing image sequences

In the project's root directory, there is a directory named `ingest`. Copy one 
of samples images sequences to it (the *whole folder* must be copied).

Wait it to be processed and published. As soon the process is completed, it must
become available in the *web client application*.

## Online documentation

The documentation is available at the web server at http://localhost:8080/doc/

> Note: to access the online documentation the _web_ container instance must be
> running.

## Third party projects

This solution uses the following OSS projects:

  **Pipeline Manager**
  - OpenJDK Docker image: openjdk:15-jdk-alpine3.11
  - javax.json 1.1.14 from Glassfish project
  - Maven Docker image: maven: 3.6.3-jdk-14  (for building)

  **Web Server**
  - NGINX Docker image: nginx:1.19-alpine

  **Web Client**
  - [dash.js v3.1.1](https://github.com/Dash-Industry-Forum/dash.js) from [Dash Industry Forum](https://dashif.org/)
  - [Boostrap v.4.5.0](https://getbootstrap.com/)
  - [AngularJS 1.7.9](https://angularjs.org/)
  - [Popper.js 1.16.0](https://popper.js.org/)
  - [jQuery 3.5.1](https://jquery.com/)
  - [Google Material icons](https://material.io/resources/icons/?style=baseline)  

  **Encoder**
  - Alpine Linux 3.12 Docker image: alpine:3.12
  - [ffmpeg version 4.2.3](https://www.ffmpeg.org/)

