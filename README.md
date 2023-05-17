# app

1. Build the docker image

```bash
docker build -t <image_name> .
```

2. Run the docker image

```bash
docker run --rm --network <bridged_network_name> --name app -p 8080:8081 <image_name>
```
This command assumes you have a bridged network created by the `docker network` command. All containers should run on the same network.

Once you finish these two steps, you can navigate the frontend at `127.0.0.1:8080`. The frontend features a form where you can enter a review. 
After clicking `submit` a sad or happy smiley will show the sentiment of your review.
