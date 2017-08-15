A video streaming server. Intended to stream videos over the local network from the host computer to any devices that support browsers. Has a [book](https://functorhub.com/func-arcs/landing.html) written about it.

Usage:

- Clone the repository via `git clone https://github.com/functortech/functional-streamer.git`, and cd to the directory it was cloned in via `cd functional-streamer`.
- Run `sbt` to enter SBT console
- Use `functionalstreamerJVM/reStart` and `functionalstreamerJVM/reStop` commands to start and stop the server respectively.
- When the server is started, navigate to [localhost:8080](localhost:8080). If you are accessing the server from the local network, you'll have to learn the local IP of the server and access via it (for example, for my computer it is `192.168.0.100`, so I'll have to navigate to `192.168.0.100:8080`). You will get access to your file system via your browser.
- Locate the videos you want to watch (only MP4 is supported so far), click them and they will be streamed to your browser.

![Screenshot](https://raw.githubusercontent.com/functortech/functional-streamer/master/screenshot.png)
