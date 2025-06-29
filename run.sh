#!/bin/zsh

data=${html_download_path}

echo $data

java -Xlog:gc -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC  -XX:ShenandoahGCMode=generational -jar checker-2.2.jar $data